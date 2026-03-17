"""BERT fine-tuning for dream sentiment classification (8 classes)."""

import argparse
import json
import os

import torch
from torch.utils.data import Dataset, DataLoader
from transformers import AutoTokenizer, AutoModelForSequenceClassification
from transformers.optimization import get_linear_schedule_with_warmup
from sklearn.metrics import f1_score, accuracy_score
from tqdm import tqdm


def load_label_names(path="data/labels.json"):
    with open(path, "r", encoding="utf-8") as f:
        obj = json.load(f)
    return obj["labels"]


def load_json(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


class DreamTextDataset(Dataset):
    def __init__(self, samples, tokenizer, max_len=128):
        self.samples = samples
        self.tokenizer = tokenizer
        self.max_len = max_len

    def __len__(self):
        return len(self.samples)

    def __getitem__(self, idx):
        item = self.samples[idx]
        text = item["text"]
        label = item["label"]
        enc = self.tokenizer(
            text,
            max_length=self.max_len,
            truncation=True,
            padding="max_length",
            return_tensors="pt",
        )
        return {
            "input_ids": enc["input_ids"].squeeze(0),
            "attention_mask": enc["attention_mask"].squeeze(0),
            "labels": torch.tensor(label, dtype=torch.long),
        }


def evaluate(model, loader, device, loss_fn):
    model.eval()
    all_preds = []
    all_labels = []
    total_loss = 0.0

    with torch.no_grad():
        for batch in loader:
            batch = {k: v.to(device) for k, v in batch.items()}
            outputs = model(**batch)
            logits = outputs.logits
            loss = loss_fn(logits, batch["labels"])
            total_loss += loss.item()
            preds = torch.argmax(outputs.logits, dim=1)
            all_preds.extend(preds.cpu().tolist())
            all_labels.extend(batch["labels"].cpu().tolist())

    acc = accuracy_score(all_labels, all_preds)
    f1 = f1_score(all_labels, all_preds, average="macro")
    return total_loss / max(1, len(loader)), acc, f1


def train(
    model_name="hfl/chinese-roberta-wwm-ext",
    epochs=4,
    lr=2e-5,
    batch_size=16,
    max_len=128,
    output_dir="checkpoints/bert",
):
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    label_names = load_label_names()
    num_labels = len(label_names)

    train_data = load_json("data/train.json")
    test_data = load_json("data/test.json")

    tokenizer = AutoTokenizer.from_pretrained(model_name)
    model = AutoModelForSequenceClassification.from_pretrained(model_name, num_labels=num_labels)
    model.to(device)

    train_ds = DreamTextDataset(train_data, tokenizer, max_len=max_len)
    test_ds = DreamTextDataset(test_data, tokenizer, max_len=max_len)
    train_loader = DataLoader(train_ds, batch_size=batch_size, shuffle=True)
    test_loader = DataLoader(test_ds, batch_size=batch_size, shuffle=False)

    # class weights to reduce bias toward frequent labels (e.g., "其他")
    label_counts = {}
    for item in train_data:
        label_counts[item["label"]] = label_counts.get(item["label"], 0) + 1
    weights = []
    for i in range(num_labels):
        count = label_counts.get(i, 1)
        weights.append(1.0 / count)
    weight_tensor = torch.tensor(weights, dtype=torch.float32).to(device)
    loss_fn = torch.nn.CrossEntropyLoss(weight=weight_tensor)

    optimizer = torch.optim.AdamW(model.parameters(), lr=lr, weight_decay=0.01)
    total_steps = len(train_loader) * epochs
    scheduler = get_linear_schedule_with_warmup(
        optimizer,
        num_warmup_steps=max(1, total_steps // 10),
        num_training_steps=total_steps,
    )

    best_f1 = 0.0
    history = {"train_loss": [], "test_loss": [], "test_acc": [], "test_f1": []}

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

        test_loss, test_acc, test_f1 = evaluate(model, test_loader, device, loss_fn)
        history["train_loss"].append(epoch_loss / max(1, len(train_loader)))
        history["test_loss"].append(test_loss)
        history["test_acc"].append(test_acc)
        history["test_f1"].append(test_f1)

        print(
            f"Epoch {epoch}: train_loss={history['train_loss'][-1]:.4f} "
            f"test_loss={test_loss:.4f} acc={test_acc:.2%} f1={test_f1:.2%}"
        )

        if test_f1 > best_f1:
            best_f1 = test_f1
            os.makedirs(output_dir, exist_ok=True)
            model.save_pretrained(output_dir)
            tokenizer.save_pretrained(output_dir)
            with open(os.path.join(output_dir, "labels.json"), "w", encoding="utf-8") as f:
                json.dump({"labels": label_names}, f, ensure_ascii=False, indent=2)
            print("  <- save best")

    with open(os.path.join(output_dir, "history_bert.json"), "w", encoding="utf-8") as f:
        json.dump(history, f, ensure_ascii=False, indent=2)

    print(f"Best macro-F1: {best_f1:.2%}")
    print(f"Model saved to: {output_dir}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="BERT dream sentiment fine-tuning")
    parser.add_argument("--model_name", type=str, default="hfl/chinese-roberta-wwm-ext")
    parser.add_argument("--epochs", type=int, default=4)
    parser.add_argument("--lr", type=float, default=2e-5)
    parser.add_argument("--batch_size", type=int, default=16)
    parser.add_argument("--max_len", type=int, default=128)
    parser.add_argument("--output_dir", type=str, default="checkpoints/bert")
    args = parser.parse_args()

    train(
        model_name=args.model_name,
        epochs=args.epochs,
        lr=args.lr,
        batch_size=args.batch_size,
        max_len=args.max_len,
        output_dir=args.output_dir,
    )
