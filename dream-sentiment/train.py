"""训练脚本：支持 TextCNN / BiLSTM 的 8分类训练。"""

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


def load_label_names(path="data/labels.json"):
    with open(path, "r", encoding="utf-8") as f:
        obj = json.load(f)
    return obj["labels"]


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


def train_one(model_type="cnn", epochs=35, lr=8e-4, batch_size=32, max_len=80):
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    label_names = load_label_names()
    num_classes = len(label_names)

    print(f"使用设备: {device}")
    print(f"分类任务: {num_classes} 类 -> {', '.join(label_names)}")

    train_loader, test_loader, vocab = load_data(
        "data/train.json",
        "data/test.json",
        batch_size=batch_size,
        max_len=max_len,
    )
    vocab.save("data/vocab.json")

    if model_type == "cnn":
        model_config = {
            "embed_dim": 128,
            "kernel_sizes": (2, 3, 4, 5),
            "num_filters": 96,
            "dropout": 0.4,
        }
        model = TextCNN(
            vocab_size=len(vocab),
            embed_dim=model_config["embed_dim"],
            num_classes=num_classes,
            kernel_sizes=model_config["kernel_sizes"],
            num_filters=model_config["num_filters"],
            dropout=model_config["dropout"],
        )
    else:
        model_config = {
            "embed_dim": 128,
            "hidden_dim": 128,
            "num_layers": 2,
            "dropout": 0.35,
        }
        model = DreamRNN(
            vocab_size=len(vocab),
            embed_dim=model_config["embed_dim"],
            hidden_dim=model_config["hidden_dim"],
            num_layers=model_config["num_layers"],
            num_classes=num_classes,
            dropout=model_config["dropout"],
        )

    model = model.to(device)
    info = model.get_model_info()
    print(f"模型参数量: {info['trainable_params']:,}")

    criterion = nn.CrossEntropyLoss(label_smoothing=0.05)
    optimizer = optim.AdamW(model.parameters(), lr=lr, weight_decay=1e-4)
    scheduler = StepLR(optimizer, step_size=12, gamma=0.5)

    os.makedirs("checkpoints", exist_ok=True)
    best_acc = 0.0
    history = {"train_loss": [], "train_acc": [], "test_loss": [], "test_acc": []}

    print(f"开始训练，总 epoch={epochs}")
    print(f"{'Epoch':>6} | {'Train Loss':>10} | {'Train Acc':>9} | {'Test Loss':>9} | {'Test Acc':>8}")
    print("-" * 60)

    latest_class_acc = {}
    for epoch in range(1, epochs + 1):
        train_loss, train_acc = train_epoch(model, train_loader, optimizer, criterion, device)
        test_loss, test_acc, class_acc = evaluate(model, test_loader, criterion, device, label_names)
        scheduler.step()

        history["train_loss"].append(train_loss)
        history["train_acc"].append(train_acc)
        history["test_loss"].append(test_loss)
        history["test_acc"].append(test_acc)
        latest_class_acc = class_acc

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
    print("各类别准确率:")
    for name in label_names:
        acc = latest_class_acc.get(name, 0.0)
        print(f"  {name}: {acc:.1%}")

    with open(f"checkpoints/history_{model_type}.json", "w", encoding="utf-8") as f:
        json.dump(history, f, ensure_ascii=False, indent=2)

    print(f"模型已保存: checkpoints/best_{model_type}.pt")


def train(model_type="cnn", epochs=35, lr=8e-4, batch_size=32, max_len=80):
    if model_type == "both":
        train_one("cnn", epochs, lr, batch_size, max_len)
        train_one("rnn", epochs, lr, batch_size, max_len)
        return
    train_one(model_type, epochs, lr, batch_size, max_len)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="梦境情感分类训练")
    parser.add_argument("--model", type=str, default="cnn", choices=["cnn", "rnn", "both"])
    parser.add_argument("--epochs", type=int, default=35)
    parser.add_argument("--lr", type=float, default=8e-4)
    parser.add_argument("--batch_size", type=int, default=32)
    parser.add_argument("--max_len", type=int, default=80)
    args = parser.parse_args()

    train(
        model_type=args.model,
        epochs=args.epochs,
        lr=args.lr,
        batch_size=args.batch_size,
        max_len=args.max_len,
    )
