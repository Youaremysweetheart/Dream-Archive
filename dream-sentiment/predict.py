"""Prediction module: long-text chunking + model ensemble + lexicon fusion."""

import json
import os
from typing import List

import torch
import torch.nn.functional as F

from model.rnn_model import DreamRNN
from model.text_cnn import TextCNN
from utils.data_utils import Vocabulary
from utils.feedback import get_feedback


DEFAULT_LABELS = [
    "\u5feb\u4e50\u68a6",
    "\u6050\u6016\u68a6",
    "\u7126\u8651\u68a6",
    "\u5947\u5e7b\u68a6",
    "\u65e5\u5e38\u68a6",
    "\u9884\u77e5\u68a6",
    "\u5669\u68a6",
    "\u5176\u4ed6",
]

try:
    from transformers import AutoModelForSequenceClassification, AutoTokenizer
except Exception:  # pragma: no cover - optional dependency
    AutoModelForSequenceClassification = None
    AutoTokenizer = None


class DreamAnalyzer:
    def __init__(self, model_type="ensemble", checkpoint_dir="checkpoints"):
        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model_type = model_type
        self.checkpoint_dir = checkpoint_dir

        self.vocab = Vocabulary()
        self.vocab.load(os.path.join(checkpoint_dir, "../data/vocab.json"))

        self.models = []
        self.label_names = DEFAULT_LABELS
        self.max_len = 80

        if model_type == "ensemble":
            for m in ["cnn", "rnn"]:
                ck = self._try_load_model(m)
                if ck is not None:
                    self.models.append(ck)
            if not self.models:
                raise FileNotFoundError("No checkpoints found: best_cnn.pt / best_rnn.pt")
        else:
            self.models.append(self._load_model(model_type))

        self.label_names = self.models[0]["label_names"]
        self.max_len = max(m["max_len"] for m in self.models)
        self.idx = {name: i for i, name in enumerate(self.label_names)}

        self.lexicons = {
            "\u6050\u6016\u68a6": {
                "strong": {"\u9b3c", "\u602a\u7269", "\u9ed1\u5f71", "\u6050\u6016", "\u9634\u68ee", "\u8840", "\u5c16\u53eb", "\u8be1\u5f02", "\u60ca\u609a", "\u60ca\u6050"},
                "weak": {"\u5bb3\u6015", "\u53ef\u6015", "\u53d1\u6bdb", "\u540e\u80cc\u53d1\u51c9"},
            },
            "\u7126\u8651\u68a6": {
                "strong": {"\u8fdf\u5230", "\u8d76\u4e0d\u4e0a", "\u6765\u4e0d\u53ca", "\u5931\u8d25", "\u7126\u8651", "\u7d27\u5f20", "\u538b\u529b", "\u5d29\u6e83"},
                "weak": {"\u62c5\u5fc3", "\u614c", "\u4e0d\u5b89", "\u5fc3\u614c"},
            },
            "\u5947\u5e7b\u68a6": {
                "strong": {"\u9b54\u6cd5", "\u661f\u7403", "\u7a7f\u8d8a", "\u5f02\u4e16\u754c", "\u8d85\u80fd\u529b", "\u9f99", "\u65f6\u7a7a", "\u5e73\u884c\u4e16\u754c"},
                "weak": {"\u98de\u884c", "\u53d1\u5149", "\u5b87\u5b99", "\u795e\u6bbf"},
            },
            "\u65e5\u5e38\u68a6": {
                "strong": {"\u4e0a\u73ed", "\u505a\u996d", "\u4e70\u83dc", "\u5730\u94c1", "\u540c\u4e8b", "\u5bb6\u52a1", "\u8d85\u5e02", "\u516c\u4ea4", "\u901a\u52e4"},
                "weak": {"\u6392\u961f", "\u6d17\u8863\u670d", "\u6536\u62fe\u623f\u95f4"},
            },
            "\u9884\u77e5\u68a6": {
                "strong": {"\u9884\u77e5", "\u9884\u793a", "\u5f81\u5146", "\u540e\u6765\u771f\u7684\u53d1\u751f", "\u4e00\u6a21\u4e00\u6837", "\u63d0\u524d\u68a6\u5230", "\u5e94\u9a8c"},
                "weak": {"\u65e5\u671f\u543b\u5408", "\u573a\u666f\u91cd\u73b0", "\u9884\u611f\u5e94\u9a8c"},
            },
            "\u5669\u68a6": {
                "strong": {"\u88ab\u8ffd", "\u88ab\u8ffd\u8d76", "\u86c7", "\u5760\u843d", "\u7a92\u606f", "\u5413\u9192", "\u60ca\u9192", "\u5669\u68a6", "\u51b7\u6c57", "\u9003\u8dd1"},
                "weak": {"\u5598\u4e0d\u8fc7\u6c14", "\u8dd1\u4e0d\u52a8", "\u9192\u6765\u5bb3\u6015"},
            },
            "\u5feb\u4e50\u68a6": {
                "strong": {"\u5f00\u5fc3", "\u5e78\u798f", "\u653e\u677e", "\u6e29\u6696", "\u62e5\u62b1", "\u5e86\u795d", "\u6cbb\u6108", "\u6109\u5feb"},
                "weak": {"\u7b11", "\u5b89\u5fc3", "\u8f7b\u677e"},
            },
        }

        print(f"Model loaded [{self.model_type.upper()}], sub_models={len(self.models)}, classes={len(self.label_names)}")

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

    def _try_load_model(self, model_type):
        ckpt_path = os.path.join(self.checkpoint_dir, f"best_{model_type}.pt")
        if not os.path.exists(ckpt_path):
            return None
        return self._load_model(model_type)

    def _load_model(self, model_type):
        ckpt_path = os.path.join(self.checkpoint_dir, f"best_{model_type}.pt")
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

    @staticmethod
    def _split_tokens(tokens: List[str], win: int, stride: int):
        if len(tokens) <= win:
            return [tokens]
        chunks = []
        i = 0
        while i < len(tokens):
            chunk = tokens[i : i + win]
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

        if "\u5669\u68a6" in self.idx and any(k in text for k in ["\u86c7", "\u6bd2\u86c7", "\u87d2"]):
            if any(k in text for k in ["\u6015", "\u5413\u9192", "\u60ca\u9192", "\u8ffd", "\u9003"]):
                scores[self.idx["\u5669\u68a6"]] += 2.5

        return torch.softmax(scores, dim=0)

    def _fuse(self, text, model_probs):
        lex_probs = self._lexicon_distribution(text)
        model_conf = float(model_probs.max().item())

        length_bonus = 0.18 if len(text) >= 180 else 0.0
        uncertainty_bonus = max(0.0, 0.58 - model_conf) * 0.9
        alpha = min(0.62, 0.16 + length_bonus + uncertainty_bonus)

        if "\u9884\u77e5\u68a6" in self.idx:
            strong_pre = self._count_hits(text, self.lexicons["\u9884\u77e5\u68a6"]["strong"])
            if strong_pre == 0:
                p = self.idx["\u9884\u77e5\u68a6"]
                model_probs[p] *= 0.72
                model_probs = model_probs / model_probs.sum()

        return (1.0 - alpha) * model_probs + alpha * lex_probs

    def analyze(self, text: str) -> dict:
        text = text.strip()
        if len(text) < 5:
            return {"error": "\u6587\u672c\u592a\u77ed\uff0c\u8bf7\u8f93\u5165\u81f3\u5c115\u4e2a\u5b57\u7684\u68a6\u5883\u63cf\u8ff0", "text": text}

        tokens = self.vocab.tokenize(text)
        if not tokens:
            return {"error": "\u6587\u672c\u65e0\u6cd5\u5206\u8bcd\uff0c\u8bf7\u8f93\u5165\u66f4\u6e05\u6670\u7684\u68a6\u5883\u63cf\u8ff0", "text": text}

        probs_sum = None
        for m in self.models:
            probs = self._predict_with_single_model(m, tokens)
            probs_sum = probs if probs_sum is None else probs_sum + probs

        model_probs = probs_sum / len(self.models)
        final_probs = self._fuse(text, model_probs)

        top_vals, top_idx = torch.topk(final_probs, k=2)
        label = int(top_idx[0].item())
        confidence = float(top_vals[0].item())
        margin = float((top_vals[0] - top_vals[1]).item())
        other_name = "\u5176\u4ed6"
        other_conf_th = float(os.getenv("OTHER_CONF_THRESHOLD", "0.20"))
        other_margin_th = float(os.getenv("OTHER_MARGIN_THRESHOLD", "0.03"))
        # 低置信 + 低区分度时回退为“其他”，避免胡乱高置信误判
        if confidence < other_conf_th and margin < other_margin_th and other_name in self.idx:
            label = self.idx[other_name]
            confidence = float(final_probs[label].item())
        label_name = self.label_names[label]

        all_probs = {self.label_names[i]: round(float(final_probs[i].item()), 4) for i in range(len(self.label_names))}
        feedback = get_feedback(label, confidence, label_name)

        return {
            "text": text,
            "model": self.model_type.upper(),
            "prediction": {
                "label": label,
                "label_name": label_name,
                "confidence": round(confidence, 4),
                "intensity": feedback["intensity"],
            },
            "all_probabilities": all_probs,
            "feedback": feedback["feedback"],
        }


def predict_single(text, model_type="ensemble"):
    analyzer = DreamAnalyzer(model_type=model_type)
    return analyzer.analyze(text)


class BertDreamAnalyzer:
    def __init__(self, model_dir="checkpoints/bert", model_name="bert-base-chinese"):
        if AutoTokenizer is None or AutoModelForSequenceClassification is None:
            raise ImportError("transformers is required for BertDreamAnalyzer. Please install it first.")

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
                model_dir, num_labels=len(self.label_names)
            )
        else:
            self.tokenizer = AutoTokenizer.from_pretrained(model_name)
            self.model = AutoModelForSequenceClassification.from_pretrained(
                model_name, num_labels=len(self.label_names)
            )

        self.model.to(self.device)
        self.model.eval()
        self.idx = {name: i for i, name in enumerate(self.label_names)}

        print(f"Model loaded [BERT], classes={len(self.label_names)}")

    def analyze(self, text: str) -> dict:
        text = str(text or "").strip()
        if len(text) < 5:
            return {"error": "文本太短，请输入至少5个字的梦境描述", "text": text}

        inputs = self.tokenizer(
            text,
            max_length=128,
            truncation=True,
            padding="max_length",
            return_tensors="pt",
        )
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
        all_probs = {self.label_names[i]: round(float(probs[i].item()), 4) for i in range(len(self.label_names))}
        feedback = get_feedback(label, confidence, label_name)

        return {
            "text": text,
            "model": "BERT",
            "prediction": {
                "label": label,
                "label_name": label_name,
                "confidence": round(confidence, 4),
                "intensity": feedback["intensity"],
            },
            "all_probabilities": all_probs,
            "feedback": feedback["feedback"],
        }
