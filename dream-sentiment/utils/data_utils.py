"""
数据预处理工具
- 中文词级分词（jieba）
- 词表构建
- 数据加载
"""

import json
import os
from collections import Counter

import jieba
import torch
from torch.utils.data import Dataset, DataLoader

CUSTOM_WORDS = [
    "被追赶",
    "吓醒",
    "惊醒",
    "赶不上",
    "来不及",
    "预知梦",
    "平行世界",
    "异世界",
    "黑影",
    "冷汗",
]
for w in CUSTOM_WORDS:
    jieba.add_word(w)


class Vocabulary:
    """词表管理"""

    def __init__(self, min_freq=1):
        self.min_freq = min_freq
        self.word2idx = {"<PAD>": 0, "<UNK>": 1}
        self.idx2word = {0: "<PAD>", 1: "<UNK>"}

    @staticmethod
    def tokenize(text):
        # 使用精确模式，过滤纯空白 token
        return [tok.strip() for tok in jieba.lcut(text, cut_all=False) if tok.strip()]

    def build(self, texts):
        """从文本列表构建词表"""
        counter = Counter()
        for text in texts:
            counter.update(self.tokenize(text))

        for word, freq in counter.items():
            if freq >= self.min_freq and word not in self.word2idx:
                idx = len(self.word2idx)
                self.word2idx[word] = idx
                self.idx2word[idx] = word

        print(f"词表构建完成，共 {len(self.word2idx)} 个词")

    def encode(self, text, max_len=60):
        """文本转索引序列"""
        tokens = self.tokenize(text)
        return self.encode_tokens(tokens, max_len=max_len)

    def encode_tokens(self, tokens, max_len=60):
        """token 序列转索引序列"""
        indices = [self.word2idx.get(tok, 1) for tok in tokens]

        if len(indices) < max_len:
            indices += [0] * (max_len - len(indices))
        else:
            indices = indices[:max_len]
        return indices

    def __len__(self):
        return len(self.word2idx)

    def save(self, path):
        with open(path, "w", encoding="utf-8") as f:
            json.dump(self.word2idx, f, ensure_ascii=False)

    def load(self, path):
        with open(path, "r", encoding="utf-8") as f:
            self.word2idx = json.load(f)
        self.idx2word = {v: k for k, v in self.word2idx.items()}


class DreamDataset(Dataset):
    """梦境文本数据集"""

    def __init__(self, data_path, vocab, max_len=60):
        self.vocab = vocab
        self.max_len = max_len
        self.samples = []

        with open(data_path, "r", encoding="utf-8") as f:
            raw_data = json.load(f)

        for item in raw_data:
            indices = vocab.encode(item["text"], max_len)
            self.samples.append(
                {
                    "input_ids": torch.tensor(indices, dtype=torch.long),
                    "label": torch.tensor(item["label"], dtype=torch.long),
                    "text": item["text"],
                    "label_name": item["label_name"],
                }
            )

    def __len__(self):
        return len(self.samples)

    def __getitem__(self, idx):
        return self.samples[idx]


def load_data(train_path, test_path, batch_size=16, max_len=60):
    """加载训练和测试数据（含增强数据）"""
    all_texts = []
    for path in [train_path, test_path]:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
            all_texts.extend([item["text"] for item in data])

    # 加载增强数据（如果存在）
    extra_path = train_path.replace(".json", "_extra.json")
    if os.path.exists(extra_path):
        with open(extra_path, "r", encoding="utf-8") as f:
            extra_data = json.load(f)
            all_texts.extend([item["text"] for item in extra_data])

    vocab = Vocabulary()
    vocab.build(all_texts)

    train_dataset = DreamDataset(train_path, vocab, max_len)
    # 如果有增强数据，也加入训练
    if os.path.exists(extra_path):
        extra_dataset = DreamDataset(extra_path, vocab, max_len)
        from torch.utils.data import ConcatDataset
        train_dataset = ConcatDataset([train_dataset, extra_dataset])

    test_dataset = DreamDataset(test_path, vocab, max_len)

    train_loader = DataLoader(train_dataset, batch_size=batch_size, shuffle=True)
    test_loader = DataLoader(test_dataset, batch_size=batch_size, shuffle=False)

    return train_loader, test_loader, vocab
