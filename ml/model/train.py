"""
FormShield Model Trainer
--------------------------
Trains a Naive Bayes spam classifier from pre-processed JSONL data.

Run the preprocessing pipeline first:
    python ml/pipeline/preprocess.py

Then train:
    python ml/model/train.py

Output:
    ml/model/classifier.pkl   — trained MultinomialNB model
    ml/model/vectorizer.pkl   — fitted TF-IDF vectorizer
"""
import json
import logging
import pickle
from pathlib import Path

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics import classification_report
from sklearn.model_selection import train_test_split
from sklearn.naive_bayes import MultinomialNB

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger(__name__)

PROJECT_ROOT = Path(__file__).resolve().parents[2]
PROCESSED_DIR = PROJECT_ROOT / "data" / "processed"
MODEL_DIR = Path(__file__).resolve().parent


def load_jsonl(path: Path) -> tuple[list[str], list[str]]:
    """Load a JSONL file and return (texts, labels)."""
    texts, labels = [], []
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            record = json.loads(line)
            texts.append(record["text"])
            labels.append(record["label"])
    return texts, labels


def main() -> None:
    train_path = PROCESSED_DIR / "train.jsonl"
    test_path = PROCESSED_DIR / "test.jsonl"

    if not train_path.exists() or not test_path.exists():
        raise FileNotFoundError(
            f"Processed data not found at {PROCESSED_DIR}.\n"
            "Run the preprocessing pipeline first:\n"
            "    python ml/pipeline/preprocess.py"
        )

    logger.info(f"Loading training data from {train_path}")
    train_texts, train_labels = load_jsonl(train_path)
    logger.info(f"Loading test data from {test_path}")
    test_texts, test_labels = load_jsonl(test_path)

    spam_count = train_labels.count("spam")
    ham_count = train_labels.count("ham")
    logger.info(f"Train: {len(train_texts)} records ({spam_count} spam, {ham_count} ham)")
    logger.info(f"Test:  {len(test_texts)} records")

    # Vectorise
    logger.info("Fitting TF-IDF vectorizer...")
    vectorizer = TfidfVectorizer(
        max_features=30_000,
        ngram_range=(1, 2),
        min_df=2,
        strip_accents="unicode",
        sublinear_tf=True,
    )
    X_train = vectorizer.fit_transform(train_texts)
    X_test = vectorizer.transform(test_texts)

    # Train
    logger.info("Training MultinomialNB classifier...")
    clf = MultinomialNB(alpha=0.1)
    clf.fit(X_train, train_labels)

    # Evaluate
    y_pred = clf.predict(X_test)
    logger.info("\n" + classification_report(test_labels, y_pred))

    # Save
    classifier_path = MODEL_DIR / "classifier.pkl"
    vectorizer_path = MODEL_DIR / "vectorizer.pkl"

    with open(classifier_path, "wb") as f:
        pickle.dump(clf, f)
    with open(vectorizer_path, "wb") as f:
        pickle.dump(vectorizer, f)

    logger.info(f"Saved model     → {classifier_path}")
    logger.info(f"Saved vectorizer → {vectorizer_path}")


if __name__ == "__main__":
    main()
