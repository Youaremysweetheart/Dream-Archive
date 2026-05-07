# 梦境情感分析 API 接口文档

> 服务端口: 5000
> 请求头: `Content-Type: application/json`
> 所有 POST 参数写在 **请求体（Request Body）** 中，使用 JSON 格式。

---

## 健康检查

```
GET /health
```

**响应**:
```json
{
  "status": "ok",
  "mode": "all",
  "num_classes": 8,
  "labels": ["快乐梦", "恐怖梦", "焦虑梦", "奇幻梦", "日常梦", "预知梦", "噩梦", "其他"],
  "available_models": ["cnn", "rnn", "bert"]
}
```

---

## 单条分析

```
POST /analyze
```

### 请求体格式

```json
{
  "text": "梦境描述文字",
  "model": "模型选择"   // 可选，不传则默认 ensemble
}
```

### 模型参数说明

| 参数值 | 说明 |
|--------|------|
| `ensemble` | **默认**，CNN + RNN + BERT 集成投票 |
| `cnn` | 仅用 TextCNN |
| `rnn` | 仅用 BiLSTM |
| `bert` | 仅用 BERT（首次触发懒加载，约需几秒） |

### 调用示例

**4 个模型各调用一次**:

```
POST /analyze

Body (CNN):
{
  "text": "梦见被一群怪物追赶，怎么也跑不动，吓出一身冷汗",
  "model": "cnn"
}

Body (RNN):
{
  "text": "梦见被一群怪物追赶，怎么也跑不动，吓出一身冷汗",
  "model": "rnn"
}

Body (BERT):
{
  "text": "梦见被一群怪物追赶，怎么也跑不动，吓出一身冷汗",
  "model": "bert"
}

Body (集成，不传 model 也一样):
{
  "text": "梦见被一群怪物追赶，怎么也跑不动，吓出一身冷汗",
  "model": "ensemble"
}
```

### 正常响应

```json
{
  "code": 200,
  "message": "分析成功",
  "data": {
    "label": 6,
    "label_name": "噩梦",
    "confidence": 0.8723,
    "intensity": "strong",
    "model_used": "ENSEMBLE",
    "all_probabilities": {
      "快乐梦": 0.0021,
      "恐怖梦": 0.0145,
      "焦虑梦": 0.0032,
      "奇幻梦": 0.0010,
      "日常梦": 0.0008,
      "预知梦": 0.0005,
      "噩梦": 0.8723,
      "其他": 0.1056
    },
    "feedback": "这是强烈噩梦体验，先照顾好身体反应，避免继续沉浸在恐惧里。"
  }
}
```

### 错误响应

```json
{
  "code": 400,
  "message": "不支持的 model 参数 'xxx'，可选: cnn, rnn, bert, ensemble",
  "data": null
}
```

---

## 批量分析

```
POST /analyze/batch
```

### 请求体格式

```json
{
  "texts": ["梦境1", "梦境2", "梦境3"],
  "model": "rnn"
}
```

限制: 单次最多 50 条。

### 调用示例

```
POST /analyze/batch

Body:
{
  "texts": [
    "梦见被怪物追赶",
    "梦见考试迟到",
    "梦见和老朋友吃饭"
  ],
  "model": "rnn"
}
```

### 响应

```json
{
  "code": 200,
  "message": "成功分析 3 条",
  "model_used": "RNN",
  "data": [
    {
      "text": "梦见被怪物追赶",
      "label": 6,
      "label_name": "噩梦",
      "confidence": 0.78,
      "intensity": "strong",
      "feedback": "这是强烈噩梦体验..."
    },
    {
      "text": "梦见考试迟到",
      "label": 2,
      "label_name": "焦虑梦",
      "confidence": 0.91,
      "intensity": "strong",
      "feedback": "焦虑信号较强..."
    },
    {
      "text": "梦见和老朋友吃饭",
      "label": 0,
      "label_name": "快乐梦",
      "confidence": 0.65,
      "intensity": "mild",
      "feedback": "这个梦整体偏积极..."
    }
  ]
}
```

---

## 分类对照

| 编号 | 标签名 |
|------|--------|
| 0 | 快乐梦 |
| 1 | 恐怖梦 |
| 2 | 焦虑梦 |
| 3 | 奇幻梦 |
| 4 | 日常梦 |
| 5 | 预知梦 |
| 6 | 噩梦 |
| 7 | 其他 |

## 响应字段说明

| 字段 | 说明 |
|------|------|
| `label` | 分类编号（0-7） |
| `label_name` | 分类名称 |
| `confidence` | 置信度（0-1），越高越确信 |
| `intensity` | `"strong"`（置信度≥0.7）或 `"mild"`（<0.7） |
| `model_used` | 实际使用的模型 |
| `all_probabilities` | 8 个类别各自的概率，总和为 1 |
| `feedback` | 针对该梦境的分析建议文案 |

## curl 速查

```bash
# 健康检查
curl http://localhost:5000/health

# CNN 分析
curl -X POST http://localhost:5000/analyze \
  -H "Content-Type: application/json" \
  -d "{\"text\": \"梦见考试迟到\", \"model\": \"cnn\"}"

# RNN 分析
curl -X POST http://localhost:5000/analyze \
  -H "Content-Type: application/json" \
  -d "{\"text\": \"梦见考试迟到\", \"model\": \"rnn\"}"

# BERT 分析
curl -X POST http://localhost:5000/analyze \
  -H "Content-Type: application/json" \
  -d "{\"text\": \"梦见考试迟到\", \"model\": \"bert\"}"

# 集成分析
curl -X POST http://localhost:5000/analyze \
  -H "Content-Type: application/json" \
  -d "{\"text\": \"梦见考试迟到\", \"model\": \"ensemble\"}"

# 批量分析
curl -X POST http://localhost:5000/analyze/batch \
  -H "Content-Type: application/json" \
  -d "{\"texts\": [\"梦A\", \"梦B\"], \"model\": \"cnn\"}"
```
