"""Prediction module: supports per-request model selection (cnn / rnn / bert / ensemble)."""

import json
import os
from typing import List, Optional

import torch
import torch.nn.functional as F

from model.rnn_model import DreamRNN
from model.text_cnn import TextCNN
from utils.data_utils import Vocabulary
from utils.feedback import get_feedback


DEFAULT_LABELS = [
    "快乐梦",      # 快乐梦
    "恐怖梦",      # 恐怖梦
    "焦虑梦",      # 焦虑梦
    "奇幻梦",      # 奇幻梦
    "日常梦",      # 日常梦
    "预知梦",      # 预知梦
    "噩梦",        # 噩梦
    "其他",        # 其他
]

try:
    from transformers import AutoModelForSequenceClassification, AutoTokenizer
except Exception:
    AutoModelForSequenceClassification = None
    AutoTokenizer = None

# ─── 子模型注册表 ──────────────────────────────────────────────
# 启动时 analyzer 一次性加载全部，analyze() 按 sub_model 选取

class DreamAnalyzer:
    """梦境分析器。

    - 启动时仅加载 CNN + RNN，BERT 首次使用才懒加载（节省 ~700MB）。
    - 推理时通过 sub_model 参数按需选取子模型。

    model_type 参数:
      - "all"      : 加载 cnn + rnn，BERT 按需懒加载（推荐）
      - "ensemble" : 仅加载 cnn + rnn（旧版兼容）
      - "cnn"/"rnn"/"bert" : 仅加载单个模型

    analyze(text, sub_model="ensemble") 的 sub_model 参数:
      - "cnn"/"rnn"/"bert" : 仅用指定模型
      - "ensemble" : 所有已加载模型取均值（默认）
    """

    def __init__(self, model_type="all", checkpoint_dir="checkpoints"):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model_type = model_type
        self.checkpoint_dir = checkpoint_dir

        # 词表
        self.vocab = Vocabulary()
        vocab_path = os.path.join(checkpoint_dir, "../data/vocab.json")
        if os.path.exists(vocab_path):
            self.vocab.load(vocab_path)

        self.models: dict = {}          # name -> {"model", "max_len", "label_names"}
        self.bert_model: Optional[dict] = None  # 懒加载，首次推理后填充
        self._bert_dir_exists = os.path.isdir(os.path.join(checkpoint_dir, "bert"))
        self._bert_load_error: Optional[str] = None  # 记录懒加载失败原因
        self.label_names = DEFAULT_LABELS
        self.max_len = 80

        # ── 根据 model_type 决定加载哪些模型 ──
        if model_type in ("all", "ensemble"):
            for m in ["cnn", "rnn"]:
                ck = self._load_model(m)
                if ck is not None:
                    self.models[m] = ck
            if not self.models and model_type != "all":
                raise FileNotFoundError("No checkpoints found. Train models first.")
            # "all" 模式下 BERT 不立即加载，留到 _predict_bert 时懒加载
        else:
            if model_type == "bert":
                pass  # 懒加载
            else:
                ck = self._load_model(model_type)
                if ck is None:
                    raise FileNotFoundError(f"Checkpoint not found: best_{model_type}.pt")
                self.models[model_type] = ck

        # 统一 label_names
        if self.label_names is DEFAULT_LABELS:
            for m in self.models.values():
                self.label_names = m["label_names"]
                break
            if self.label_names is DEFAULT_LABELS and self._bert_dir_exists:
                bert_labels = os.path.join(checkpoint_dir, "bert", "labels.json")
                if os.path.exists(bert_labels):
                    with open(bert_labels, "r", encoding="utf-8") as f:
                        self.label_names = json.load(f).get("labels", DEFAULT_LABELS)
        self.idx = {name: i for i, name in enumerate(self.label_names)}

        # max_len
        if self.models:
            self.max_len = max(m["max_len"] for m in self.models.values())

        # 词典融合规则
        self.lexicons = {
            "恐怖梦": {
                "strong": {"鬼", "怪物", "黑影", "恐怖", "阴森", "血", "尖叫", "诡异", "惊悚", "惊恐"},
                "weak": {"害怕", "可怕", "发毛", "后背发凉"},
            },
            "焦虑梦": {
                "strong": {"迟到", "赶不上", "来不及", "失败", "焦虑", "紧张", "压力", "崩溃"},
                "weak": {"担心", "慌", "不安", "心慌"},
            },
            "奇幻梦": {
                "strong": {"魔法", "星球", "穿越", "异世界", "超能力", "龙", "时空", "平行世界"},
                "weak": {"飞行", "发光", "宇宙", "神殿"},
            },
            "日常梦": {
                "strong": {"上班", "做饭", "买菜", "地铁", "同事", "家务", "超市", "公交", "通勤"},
                "weak": {"排队", "洗衣服", "收拾房间"},
            },
            "预知梦": {
                "strong": {"预知", "预示", "征兆", "后来真的发生", "一模一样", "提前梦到", "应验"},
                "weak": {"日期吻合", "场景重现", "预感应验"},
            },
            "噩梦": {
                "strong": {"被追", "被追赶", "蛇", "坠落", "窒息", "吓醒", "惊醒", "噩梦", "冷汗", "逃跑"},
                "weak": {"喘不过气", "跑不动", "醒来害怕"},
            },
            "快乐梦": {
                "strong": {"开心", "幸福", "放松", "温暖", "拥抱", "庆祝", "治愈", "愉快"},
                "weak": {"笑", "安心", "轻松"},
            },
        }

        loaded = list(self.models.keys())
        bert_status = "bert(lazy)" if self._bert_dir_exists else "bert(unavailable)"
        print(f"Analyzer ready [mode={model_type}], loaded={loaded}, {bert_status}, classes={len(self.label_names)}")

    # ── 模型加载 ────────────────────────────────────────────

    def _build_model_by_checkpoint(self, checkpoint):
        model_type = checkpoint.get("model_type", "cnn")
        cfg = checkpoint.get("model_config", {})
        num_classes = checkpoint.get("num_classes", len(checkpoint.get("label_names", DEFAULT_LABELS)))
        vocab_size = checkpoint["vocab_size"]

        if model_type == "cnn":
            return TextCNN(
                vocab_size=vocab_size,
                embed_dim=cfg.get("embed_dim", 128),
                num_classes=num_classes,
                kernel_sizes=tuple(cfg.get("kernel_sizes", (2, 3, 4, 5))),
                num_filters=cfg.get("num_filters", 96),
                dropout=cfg.get("dropout", 0.4),
            )
        return DreamRNN(
            vocab_size=vocab_size,
            embed_dim=cfg.get("embed_dim", 128),
            hidden_dim=cfg.get("hidden_dim", 128),
            num_layers=cfg.get("num_layers", 2),
            num_classes=num_classes,
            dropout=cfg.get("dropout", 0.35),
        )

    def _load_model(self, model_type):
        ckpt_path = os.path.join(self.checkpoint_dir, f"best_{model_type}.pt")
        if not os.path.exists(ckpt_path):
            return None
        checkpoint = torch.load(ckpt_path, map_location=self.device)
        model = self._build_model_by_checkpoint(checkpoint)
        model.load_state_dict(checkpoint["model_state_dict"])
        model.to(self.device)
        model.eval()
        return {
            "name": model_type,
            "model": model,
            "max_len": checkpoint.get("max_len", 80),
            "label_names": checkpoint.get("label_names", DEFAULT_LABELS),
        }

    def _try_load_bert(self, checkpoint_dir):
        if AutoModelForSequenceClassification is None:
            self._bert_load_error = "transformers not installed"
            return
        bert_dir = os.path.join(checkpoint_dir, "bert")
        if not os.path.isdir(bert_dir):
            self._bert_load_error = "bert dir not found"
            return
        try:
            tokenizer = AutoTokenizer.from_pretrained(bert_dir)
            model = AutoModelForSequenceClassification.from_pretrained(bert_dir)
            model.to(self.device)
            model.eval()
            self.bert_model = {"model": model, "tokenizer": tokenizer}
            print("  [BERT] lazy-loaded successfully")
        except Exception as e:
            self._bert_load_error = str(e)
            import traceback
            traceback.print_exc()

    def _ensure_bert(self):
        """确保 BERT 已加载，未加载则执行懒加载。"""
        if self.bert_model is not None:
            return True
        if self._bert_load_error is not None:
            return False
        if not self._bert_dir_exists:
            self._bert_load_error = "BERT checkpoint directory not found"
            return False
        self._try_load_bert(self.checkpoint_dir)
        return self.bert_model is not None

    def _predict_bert(self, text: str) -> torch.Tensor:
        if not self._ensure_bert():
            return None
        model = self.bert_model["model"]
        tokenizer = self.bert_model["tokenizer"]
        inputs = tokenizer(
            text, max_length=128, truncation=True, padding="max_length", return_tensors="pt",
        )
        inputs = {k: v.to(self.device) for k, v in inputs.items()}
        with torch.no_grad():
            logits = model(**inputs).logits[0]
            return torch.softmax(logits, dim=0)

    # ── 词级推理（CNN / RNN）──────────────────────────────

    @staticmethod
    def _split_tokens(tokens: List[str], win: int, stride: int):
        if len(tokens) <= win:
            return [tokens]
        chunks = []
        i = 0
        while i < len(tokens):
            chunk = tokens[i: i + win]
            if not chunk:
                break
            chunks.append(chunk)
            if i + win >= len(tokens):
                break
            i += stride
        return chunks

    def _predict_with_single_model(self, model_entry, tokens):
        model = model_entry["model"]
        max_len = model_entry["max_len"]
        stride = max(24, max_len // 2)
        chunks = self._split_tokens(tokens, max_len, stride)

        probs_sum = None
        with torch.no_grad():
            for chunk in chunks:
                indices = self.vocab.encode_tokens(chunk, max_len=max_len)
                input_tensor = torch.tensor([indices], dtype=torch.long).to(self.device)
                logits = model(input_tensor)
                probs = F.softmax(logits, dim=1)[0]
                probs_sum = probs if probs_sum is None else probs_sum + probs
        return probs_sum / len(chunks)

    # ── 词典融合 ──────────────────────────────────────────

    @staticmethod
    def _count_hits(text, words):
        return sum(1 for w in words if w in text)

    def _lexicon_distribution(self, text):
        scores = torch.full((len(self.label_names),), 0.01, dtype=torch.float32, device=self.device)
        for label_name, cfg in self.lexicons.items():
            if label_name not in self.idx:
                continue
            strong_hits = self._count_hits(text, cfg["strong"])
            weak_hits = self._count_hits(text, cfg["weak"])
            scores[self.idx[label_name]] += 1.6 * strong_hits + 0.6 * weak_hits

        if "噩梦" in self.idx and any(k in text for k in ["蛇", "毒蛇", "蟒"]):
            if any(k in text for k in ["怕", "吓醒", "惊醒", "追", "逃"]):
                scores[self.idx["噩梦"]] += 2.5
        return torch.softmax(scores, dim=0)

    def _fuse(self, text, model_probs):
        lex_probs = self._lexicon_distribution(text)
        model_conf = float(model_probs.max().item())

        length_bonus = 0.18 if len(text) >= 180 else 0.0
        uncertainty_bonus = max(0.0, 0.58 - model_conf) * 0.9
        alpha = min(0.62, 0.16 + length_bonus + uncertainty_bonus)

        if "预知梦" in self.idx:
            strong_pre = self._count_hits(text, self.lexicons["预知梦"]["strong"])
            if strong_pre == 0:
                p = self.idx["预知梦"]
                model_probs[p] *= 0.72
                model_probs = model_probs / model_probs.sum()

        return (1.0 - alpha) * model_probs + alpha * lex_probs

    # ── 对外推理接口 ──────────────────────────────────────

    def analyze(self, text: str, sub_model: str = "ensemble") -> dict:
        """分析梦境文本。

        Args:
            text: 梦境描述
            sub_model: 使用的模型 — "cnn", "rnn", "bert", "ensemble"

        Returns:
            包含预测结果的字典
        """
        text = text.strip()
        if len(text) < 5:
            return {"error": "文本太短，请输入至少5个字的梦境描述", "text": text}

        sub_model = sub_model.lower()

        # ── 收集概率 ──
        probs_list = []

        # CNN / RNN
        if sub_model in ("cnn", "rnn", "ensemble"):
            tokens = self.vocab.tokenize(text)
            if not tokens:
                return {"error": "文本无法分词，请输入更清晰的梦境描述", "text": text}

            if sub_model == "ensemble":
                targets = list(self.models.keys())
            else:
                targets = [sub_model] if sub_model in self.models else []

            for name in targets:
                entry = self.models.get(name)
                if entry is not None:
                    p = self._predict_with_single_model(entry, tokens)
                    probs_list.append(p)

        # BERT（_predict_bert 内部会自动触发懒加载）
        if sub_model in ("bert", "ensemble"):
            p = self._predict_bert(text)
            if p is not None:
                probs_list.append(p)

        if not probs_list:
            return {"error": f"模型 '{sub_model}' 未加载，可用: {self.available_models}", "text": text}

        # ── 均值融合 ──
        model_probs = sum(probs_list) / len(probs_list)
        final_probs = self._fuse(text, model_probs)

        # ── 输出 ──
        top_vals, top_idx = torch.topk(final_probs, k=2)
        label = int(top_idx[0].item())
        confidence = float(top_vals[0].item())
        margin = float((top_vals[0] - top_vals[1]).item())

        other_name = "其他"
        other_conf_th = float(os.getenv("OTHER_CONF_THRESHOLD", "0.20"))
        other_margin_th = float(os.getenv("OTHER_MARGIN_THRESHOLD", "0.03"))
        if confidence < other_conf_th and margin < other_margin_th and other_name in self.idx:
            label = self.idx[other_name]
            confidence = float(final_probs[label].item())

        label_name = self.label_names[label]
        all_probs = {self.label_names[i]: round(float(final_probs[i].item()), 4)
                     for i in range(len(self.label_names))}
        fb = get_feedback(label, confidence, label_name)

        return {
            "text": text,
            "model": sub_model.upper(),
            "prediction": {
                "label": label,
                "label_name": label_name,
                "confidence": round(confidence, 4),
                "intensity": fb["intensity"],
            },
            "all_probabilities": all_probs,
            "feedback": fb["feedback"],
        }

    @property
    def available_models(self) -> list:
        models = list(self.models.keys())
        if self._bert_dir_exists:
            models.append("bert")
        return models

    def analyze_batch(self, texts: list, sub_model: str = "ensemble") -> list:
        """批量分析"""
        results = []
        for text in texts:
            result = self.analyze(str(text).strip(), sub_model=sub_model)
            if "error" in result:
                continue
            results.append({
                "text": text,
                "label": result["prediction"]["label"],
                "label_name": result["prediction"]["label_name"],
                "confidence": result["prediction"]["confidence"],
                "intensity": result["prediction"]["intensity"],
                "feedback": result["feedback"],
            })
        return results


class BertDreamAnalyzer:
    """旧版 BERT 分析器（兼容）"""

    def __init__(self, model_dir="checkpoints/bert", model_name="bert-base-chinese"):
        if AutoTokenizer is None or AutoModelForSequenceClassification is None:
            raise ImportError("transformers is required for BertDreamAnalyzer.")
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model_dir = model_dir
        self.model_name = model_name

        labels_path = os.path.join(model_dir, "labels.json")
        if os.path.exists(labels_path):
            with open(labels_path, "r", encoding="utf-8") as f:
                self.label_names = json.load(f).get("labels", DEFAULT_LABELS)
        else:
            self.label_names = DEFAULT_LABELS

        if os.path.exists(model_dir):
            self.tokenizer = AutoTokenizer.from_pretrained(model_dir)
            self.model = AutoModelForSequenceClassification.from_pretrained(
                model_dir, num_labels=len(self.label_names))
        else:
            self.tokenizer = AutoTokenizer.from_pretrained(model_name)
            self.model = AutoModelForSequenceClassification.from_pretrained(
                model_name, num_labels=len(self.label_names))
        self.model.to(self.device)
        self.model.eval()
        self.idx = {name: i for i, name in enumerate(self.label_names)}
        print(f"Model loaded [BERT], classes={len(self.label_names)}")

    def analyze(self, text: str) -> dict:
        text = str(text or "").strip()
        if len(text) < 5:
            return {"error": "文本太短，请输入至少5个字的梦境描述", "text": text}

        inputs = self.tokenizer(
            text, max_length=128, truncation=True, padding="max_length", return_tensors="pt")
        inputs = {k: v.to(self.device) for k, v in inputs.items()}
        with torch.no_grad():
            logits = self.model(**inputs).logits[0]
            probs = torch.softmax(logits, dim=0)

        top_vals, top_idx = torch.topk(probs, k=2)
        label = int(top_idx[0].item())
        confidence = float(top_vals[0].item())
        margin = float((top_vals[0] - top_vals[1]).item())
        other_name = "其他"
        other_conf_th = float(os.getenv("OTHER_CONF_THRESHOLD", "0.20"))
        other_margin_th = float(os.getenv("OTHER_MARGIN_THRESHOLD", "0.03"))
        if confidence < other_conf_th and margin < other_margin_th and other_name in self.idx:
            label = self.idx[other_name]
            confidence = float(probs[label].item())

        label_name = self.label_names[label]
        all_probs = {self.label_names[i]: round(float(probs[i].item()), 4)
                     for i in range(len(self.label_names))}
        fb = get_feedback(label, confidence, label_name)

        return {
            "text": text,
            "model": "BERT",
            "prediction": {
                "label": label,
                "label_name": label_name,
                "confidence": round(confidence, 4),
                "intensity": fb["intensity"],
            },
            "all_probabilities": all_probs,
            "feedback": fb["feedback"],
        }
