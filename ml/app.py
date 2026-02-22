"""
FormShield ML Microservice
--------------------------
Naive Bayes spam classifier trained on SpamAssassin corpus.
Called by the Spring Boot API for Pro+ plan users.
"""
import os
import pickle
import logging
from flask import Flask, request, jsonify

app = Flask(__name__)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

MODEL_PATH = os.path.join(os.path.dirname(__file__), "model", "classifier.pkl")
VECTORIZER_PATH = os.path.join(os.path.dirname(__file__), "model", "vectorizer.pkl")

model = None
vectorizer = None


def load_model():
    global model, vectorizer
    if os.path.exists(MODEL_PATH) and os.path.exists(VECTORIZER_PATH):
        with open(MODEL_PATH, "rb") as f:
            model = pickle.load(f)
        with open(VECTORIZER_PATH, "rb") as f:
            vectorizer = pickle.load(f)
        logger.info("Model loaded successfully")
    else:
        logger.warning("No trained model found. Run ml/model/train.py first.")


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "model_loaded": model is not None})


@app.route("/predict", methods=["POST"])
def predict():
    data = request.get_json(force=True)
    text = data.get("text", "")

    if not text:
        return jsonify({"score": 0.0, "label": "ham"})

    if model is None or vectorizer is None:
        logger.warning("Model not loaded, returning neutral score")
        return jsonify({"score": 0.0, "label": "ham"})

    try:
        features = vectorizer.transform([text])
        probabilities = model.predict_proba(features)[0]
        # Assumes class order: [ham, spam]
        spam_index = list(model.classes_).index("spam") if "spam" in model.classes_ else 1
        spam_score = float(probabilities[spam_index])
        label = "spam" if spam_score >= 0.5 else "ham"
        return jsonify({"score": spam_score, "label": label})
    except Exception as e:
        logger.error(f"Prediction error: {e}")
        return jsonify({"score": 0.0, "label": "ham"})


if __name__ == "__main__":
    load_model()
    port = int(os.environ.get("PORT", 5000))
    app.run(host="0.0.0.0", port=port)
