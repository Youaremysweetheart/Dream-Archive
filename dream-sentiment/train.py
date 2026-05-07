"""统一训练脚本：支持 CNN / RNN / BERT / all，一条命令完成全部训练。"""

import argparse
import json
import os

import torch
import torch.nn as nn
import torch.optim as optim
from torch.optim.lr_scheduler import StepLR

from model.rnn_model import DreamRNN
from model.text_cnn import TextCNN
from utils.data_utils import load_data


# ── 公共 ──────────────────────────────────────────────────────

def load_label_names(path="data/labels.json"):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)["labels"]


# ── CNN / RNN 训练 ────────────────────────────────────────────

def train_epoch(model, loader, optimizer, criterion, device):
    model.train()
    total_loss = 0.0
    correct = 0
    total = 0
    for batch in loader:
        input_ids = batch["input_ids"].to(device)
        labels = batch["label"].to(device)

        optimizer.zero_grad()
        logits = model(input_ids)
        loss = criterion(logits, labels)
        loss.backward()
        nn.utils.clip_grad_norm_(model.parameters(), max_norm=1.0)
        optimizer.step()

        total_loss += loss.item()
        preds = logits.argmax(dim=1)
        correct += (preds == labels).sum().item()
        total += labels.size(0)

    return total_loss / len(loader), correct / total


def evaluate(model, loader, criterion, device, label_names):
    model.eval()
    total_loss = 0.0
    correct = 0
    total = 0
    class_correct = [0 for _ in label_names]
    class_total = [0 for _ in label_names]

    with torch.no_grad():
        for batch in loader:
            input_ids = batch["input_ids"].to(device)
            labels = batch["label"].to(device)
            logits = model(input_ids)
            loss = criterion(logits, labels)

            total_loss += loss.item()
            preds = logits.argmax(dim=1)
            correct += (preds == labels).sum().item()
            total += labels.size(0)

            for i in range(len(labels)):
                label = labels[i].item()
                class_total[label] += 1
                if preds[i].item() == label:
                    class_correct[label] += 1

    class_acc = {}
    for i, name in enumerate(label_names):
        if class_total[i] > 0:
            class_acc[name] = class_correct[i] / class_total[i]

    return total_loss / len(loader), correct / total, class_acc


def train_cnn_rnn(model_type, epochs, lr, batch_size, max_len):
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    label_names = load_label_names()
    num_classes = len(label_names)

    print(f"\n使用设备: {device}")
    print(f"分类任务: {num_classes} 类 -> {', '.join(label_names)}")

    train_loader, test_loader, vocab = load_data(
        "data/train.json", "data/test.json",
        batch_size=batch_size, max_len=max_len,
    )
    vocab.save("data/vocab.json")

    if model_type == "cnn":
        model_config = {
            "embed_dim": 128, "kernel_sizes": (2, 3, 4, 5),
            "num_filters": 96, "dropout": 0.4,
        }
        model = TextCNN(
            vocab_size=len(vocab), embed_dim=model_config["embed_dim"],
            num_classes=num_classes, kernel_sizes=model_config["kernel_sizes"],
            num_filters=model_config["num_filters"], dropout=model_config["dropout"],
        )
    else:
        model_config = {
            "embed_dim": 128, "hidden_dim": 128,
            "num_layers": 2, "dropout": 0.35,
        }
        model = DreamRNN(
            vocab_size=len(vocab), embed_dim=model_config["embed_dim"],
            hidden_dim=model_config["hidden_dim"], num_layers=model_config["num_layers"],
            num_classes=num_classes, dropout=model_config["dropout"],
        )

    model = model.to(device)
    info = model.get_model_info()
    print(f"模型参数量: {info['trainable_params']:,}")

    criterion = nn.CrossEntropyLoss(label_smoothing=0.05)
    optimizer = optim.AdamW(model.parameters(), lr=lr, weight_decay=1e-4)
    scheduler = StepLR(optimizer, step_size=12, gamma=0.5)

    os.makedirs("checkpoints", exist_ok=True)
    best_acc = 0.0

    print(f"开始训练 {model_type.upper()}，总 epoch={epochs}")
    print(f"{'Epoch':>6} | {'Train Loss':>10} | {'Train Acc':>9} | {'Test Loss':>9} | {'Test Acc':>8}")
    print("-" * 60)

    for epoch in range(1, epochs + 1):
        train_loss, train_acc = train_epoch(model, train_loader, optimizer, criterion, device)
        test_loss, test_acc, _ = evaluate(model, test_loader, criterion, device, label_names)
        scheduler.step()

        print(
            f"{epoch:>6} | {train_loss:>10.4f} | {train_acc:>8.1%} | {test_loss:>9.4f} | {test_acc:>7.1%}",
            end="",
        )

        if test_acc > best_acc:
            best_acc = test_acc
            torch.save(
                {
                    "epoch": epoch,
                    "model_state_dict": model.state_dict(),
                    "model_type": model_type,
                    "vocab_size": len(vocab),
                    "num_classes": num_classes,
                    "label_names": label_names,
                    "max_len": max_len,
                    "test_acc": test_acc,
                    "model_config": model_config,
                },
                f"checkpoints/best_{model_type}.pt",
            )
            print("  <- save best")
        else:
            print()

    print(f"训练完成，最优测试准确率: {best_acc:.1%}")
    print(f"模型已保存: checkpoints/best_{model_type}.pt")


# ── BERT 训练 ────────────────────────────────────────────────

import os
os.environ["HF_HUB_DISABLE_SYMLINKS_WARNING"] = "1"
os.environ["HF_HUB_DISABLE_TELEMETRY"] = "1"
os.environ["SAFETENSORS_CONVERSION_DISABLE"] = "1"

from torch.utils.data import Dataset, DataLoader as BertDataLoader
from transformers import AutoTokenizer, AutoModelForSequenceClassification
from transformers.optimization import get_linear_schedule_with_warmup
from sklearn.metrics import f1_score, accuracy_score
from tqdm import tqdm


class DreamTextDataset(Dataset):
    def __init__(self, samples, tokenizer, max_len=128):
        self.samples = samples
        self.tokenizer = tokenizer
        self.max_len = max_len

    def __len__(self):
        return len(self.samples)

    def __getitem__(self, idx):
        item = self.samples[idx]
        enc = self.tokenizer(
            item["text"], max_length=self.max_len,
            truncation=True, padding="max_length", return_tensors="pt",
        )
        return {
            "input_ids": enc["input_ids"].squeeze(0),
            "attention_mask": enc["attention_mask"].squeeze(0),
            "labels": torch.tensor(item["label"], dtype=torch.long),
        }


def evaluate_bert(model, loader, device, loss_fn):
    model.eval()
    all_preds, all_labels = [], []
    total_loss = 0.0

    with torch.no_grad():
        for batch in loader:
            batch = {k: v.to(device) for k, v in batch.items()}
            outputs = model(**batch)
            loss = loss_fn(outputs.logits, batch["labels"])
            total_loss += loss.item()
            preds = torch.argmax(outputs.logits, dim=1)
            all_preds.extend(preds.cpu().tolist())
            all_labels.extend(batch["labels"].cpu().tolist())

    acc = accuracy_score(all_labels, all_preds)
    f1 = f1_score(all_labels, all_preds, average="macro")
    return total_loss / max(1, len(loader)), acc, f1


def train_bert(epochs, lr, batch_size, max_len):
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    label_names = load_label_names()
    num_labels = len(label_names)

    model_name = "hfl/chinese-roberta-wwm-ext"
    output_dir = "checkpoints/bert"

    print(f"\n使用设备: {device}")
    print(f"BERT 模型: {model_name}")
    print(f"分类任务: {num_labels} 类")

    def load_json(path):
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f)

    train_data = load_json("data/train.json")
    test_data = load_json("data/test.json")

    # 同时加载增强数据
    extra_path = "data/train_extra.json"
    if os.path.exists(extra_path):
        extra_data = load_json(extra_path)
        train_data.extend(extra_data)
        print(f"增强数据已加入训练集，共 {len(train_data)} 条")

    tokenizer = AutoTokenizer.from_pretrained(model_name)
    model = AutoModelForSequenceClassification.from_pretrained(model_name, num_labels=num_labels)
    model.to(device)

    train_ds = DreamTextDataset(train_data, tokenizer, max_len=max_len)
    test_ds = DreamTextDataset(test_data, tokenizer, max_len=max_len)
    train_loader = BertDataLoader(train_ds, batch_size=batch_size, shuffle=True)
    test_loader = BertDataLoader(test_ds, batch_size=batch_size, shuffle=False)

    # class weights
    label_counts = {}
    for item in train_data:
        label_counts[item["label"]] = label_counts.get(item["label"], 0) + 1
    weights = [1.0 / label_counts.get(i, 1) for i in range(num_labels)]
    weight_tensor = torch.tensor(weights, dtype=torch.float32).to(device)
    loss_fn = nn.CrossEntropyLoss(weight=weight_tensor)

    optimizer = optim.AdamW(model.parameters(), lr=lr, weight_decay=0.01)
    total_steps = len(train_loader) * epochs
    scheduler = get_linear_schedule_with_warmup(
        optimizer,
        num_warmup_steps=max(1, total_steps // 10),
        num_training_steps=total_steps,
    )

    best_f1 = 0.0

    for epoch in range(1, epochs + 1):
        model.train()
        epoch_loss = 0.0
        progress = tqdm(train_loader, desc=f"Epoch {epoch}/{epochs}")
        for batch in progress:
            batch = {k: v.to(device) for k, v in batch.items()}
            outputs = model(**batch)
            loss = loss_fn(outputs.logits, batch["labels"])
            loss.backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), max_norm=1.0)
            optimizer.step()
            scheduler.step()
            optimizer.zero_grad()
            epoch_loss += loss.item()
            progress.set_postfix({"loss": f"{loss.item():.4f}"})

        test_loss, test_acc, test_f1 = evaluate_bert(model, test_loader, device, loss_fn)
        print(f"Epoch {epoch}: train_loss={epoch_loss / len(train_loader):.4f} "
              f"test_loss={test_loss:.4f} acc={test_acc:.2%} f1={test_f1:.2%}")

        if test_f1 > best_f1:
            best_f1 = test_f1
            os.makedirs(output_dir, exist_ok=True)
            model.save_pretrained(output_dir)
            tokenizer.save_pretrained(output_dir)
            with open(os.path.join(output_dir, "labels.json"), "w", encoding="utf-8") as f:
                json.dump({"labels": label_names}, f, ensure_ascii=False, indent=2)
            print("  <- save best")

    print(f"Best macro-F1: {best_f1:.2%}")
    print(f"Model saved to: {output_dir}")


# ── 入口 ──────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="梦境情感分类统一训练")
    parser.add_argument("--model", type=str, default="all",
                        choices=["cnn", "rnn", "bert", "both", "all"],
                        help="训练哪个模型（默认 all: cnn+rnn+bert）")
    parser.add_argument("--epochs", type=int, default=50, help="CNN/RNN 训练轮数")
    parser.add_argument("--lr", type=float, default=8e-4, help="CNN/RNN 学习率")
    parser.add_argument("--batch_size", type=int, default=32, help="CNN/RNN batch size")
    parser.add_argument("--max_len", type=int, default=100, help="CNN/RNN 最大序列长度")
    parser.add_argument("--bert_epochs", type=int, default=6, help="BERT 训练轮数")
    parser.add_argument("--bert_lr", type=float, default=2e-5, help="BERT 学习率")
    parser.add_argument("--bert_batch_size", type=int, default=16, help="BERT batch size")
    args = parser.parse_args()

    if args.model in ("cnn", "rnn"):
        train_cnn_rnn(args.model, args.epochs, args.lr, args.batch_size, args.max_len)
    elif args.model == "bert":
        train_bert(args.bert_epochs, args.bert_lr, args.bert_batch_size, args.max_len)
    elif args.model == "both":
        train_cnn_rnn("cnn", args.epochs, args.lr, args.batch_size, args.max_len)
        train_cnn_rnn("rnn", args.epochs, args.lr, args.batch_size, args.max_len)
    else:  # all
        print("\n========== 1/3 训练 TextCNN ==========")
        train_cnn_rnn("cnn", args.epochs, args.lr, args.batch_size, args.max_len)
        print("\n========== 2/3 训练 BiLSTM ==========")
        train_cnn_rnn("rnn", args.epochs, args.lr, args.batch_size, args.max_len)
        print("\n========== 3/3 训练 BERT ==========")
        train_bert(args.bert_epochs, args.bert_lr, args.bert_batch_size, args.max_len)
        print("\n全部模型训练完成！")
        print("  checkpoints/best_cnn.pt")
        print("  checkpoints/best_rnn.pt")
        print("  checkpoints/bert/")


if __name__ == "__main__":
    main()
