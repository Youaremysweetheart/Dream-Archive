"""Flask API 服务：支持 per-request 模型切换。"""

import os
import traceback

from flask import Flask, jsonify, request
from flask_cors import CORS

from predict import DreamAnalyzer

app = Flask(__name__)
CORS(app)

# 启动时加载 CNN + RNN（BERT 首次请求时懒加载）
print("正在加载模型 (cnn + rnn)，BERT 按需懒加载 ...")
analyzer = DreamAnalyzer(model_type="all")
print(f"模型就绪，可用子模型: {analyzer.available_models}")


@app.route("/health", methods=["GET"])
def health():
    return jsonify({
        "status": "ok",
        "mode": "all",
        "num_classes": len(analyzer.label_names),
        "labels": analyzer.label_names,
        "available_models": analyzer.available_models,
    })


@app.route("/analyze", methods=["POST"])
def analyze():
    try:
        data = request.get_json()
        if not data or "text" not in data:
            return jsonify({"code": 400, "message": "请求体需要包含 text 字段", "data": None}), 400

        text = str(data["text"]).strip()
        if not text:
            return jsonify({"code": 400, "message": "梦境描述不能为空", "data": None}), 400

        # 可选：指定使用的模型，默认 ensemble
        sub_model = str(data.get("model", "ensemble")).strip().lower()
        if sub_model not in ("cnn", "rnn", "bert", "ensemble"):
            return jsonify({
                "code": 400,
                "message": f"不支持的 model 参数 '{sub_model}'，可选: cnn, rnn, bert, ensemble",
                "data": None,
            }), 400

        result = analyzer.analyze(text, sub_model=sub_model)
        if "error" in result:
            return jsonify({"code": 400, "message": result["error"], "data": None}), 400

        return jsonify({
            "code": 200,
            "message": "分析成功",
            "data": {
                "label": result["prediction"]["label"],
                "label_name": result["prediction"]["label_name"],
                "confidence": result["prediction"]["confidence"],
                "intensity": result["prediction"]["intensity"],
                "all_probabilities": result["all_probabilities"],
                "feedback": result["feedback"],
                "model_used": result["model"],
            },
        })
    except Exception as e:
        traceback.print_exc()
        return jsonify({"code": 500, "message": f"服务器内部错误: {e}", "data": None}), 500


@app.route("/analyze/batch", methods=["POST"])
def analyze_batch():
    try:
        data = request.get_json()
        if not data or "texts" not in data:
            return jsonify({"code": 400, "message": "请求体需要包含 texts 字段"}), 400

        texts = data["texts"]
        if not isinstance(texts, list):
            return jsonify({"code": 400, "message": "texts 必须是数组"}), 400
        if len(texts) > 50:
            return jsonify({"code": 400, "message": "单次最多分析 50 条"}), 400

        sub_model = str(data.get("model", "ensemble")).strip().lower()
        if sub_model not in ("cnn", "rnn", "bert", "ensemble"):
            return jsonify({
                "code": 400,
                "message": f"不支持的 model 参数 '{sub_model}'，可选: cnn, rnn, bert, ensemble",
            }), 400

        results = analyzer.analyze_batch(texts, sub_model=sub_model)

        return jsonify({
            "code": 200,
            "message": f"成功分析 {len(results)} 条",
            "data": results,
            "model_used": sub_model.upper(),
        })
    except Exception as e:
        traceback.print_exc()
        return jsonify({"code": 500, "message": str(e)}), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)
