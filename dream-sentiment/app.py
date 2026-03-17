"""Flask API 服务。"""

import os
import traceback

from flask import Flask, jsonify, request
from flask_cors import CORS

from predict import DreamAnalyzer, BertDreamAnalyzer

app = Flask(__name__)
CORS(app)

model_type = os.getenv("MODEL_TYPE", "ensemble").strip().lower()

print(f"正在加载模型 ({model_type}) ...")
if model_type == "bert":
    analyzer = BertDreamAnalyzer()
else:
    analyzer = DreamAnalyzer(model_type=model_type)
print("模型就绪，服务已启动")


@app.route("/health", methods=["GET"])
def health():
    return jsonify(
        {
            "status": "ok",
            "model": analyzer.model_type.upper(),
            "num_classes": len(analyzer.label_names),
            "labels": analyzer.label_names,
        }
    )


@app.route("/analyze", methods=["POST"])
def analyze():
    try:
        data = request.get_json()
        if not data or "text" not in data:
            return jsonify({"code": 400, "message": "请求体需要包含 text 字段", "data": None}), 400

        text = str(data["text"]).strip()
        if not text:
            return jsonify({"code": 400, "message": "梦境描述不能为空", "data": None}), 400

        result = analyzer.analyze(text)
        if "error" in result:
            return jsonify({"code": 400, "message": result["error"], "data": None}), 400

        return jsonify(
            {
                "code": 200,
                "message": "分析成功",
                "data": {
                    "label": result["prediction"]["label"],
                    "label_name": result["prediction"]["label_name"],
                    "confidence": result["prediction"]["confidence"],
                    "intensity": result["prediction"]["intensity"],
                    "all_probabilities": result["all_probabilities"],
                    "feedback": result["feedback"],
                },
            }
        )
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

        results = []
        for text in texts:
            result = analyzer.analyze(str(text).strip())
            if "error" in result:
                continue
            results.append(
                {
                    "text": text,
                    "label": result["prediction"]["label"],
                    "label_name": result["prediction"]["label_name"],
                    "confidence": result["prediction"]["confidence"],
                    "feedback": result["feedback"],
                }
            )

        return jsonify({"code": 200, "message": f"成功分析 {len(results)} 条", "data": results})
    except Exception as e:
        traceback.print_exc()
        return jsonify({"code": 500, "message": str(e)}), 500


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)
