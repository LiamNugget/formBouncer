"""
Train Naive Bayes classifier on SpamAssassin corpus.

Usage:
    pip install -r requirements.txt
    python train.py

The script downloads the SpamAssassin public corpus, trains a
MultinomialNB classifier, and saves it alongside a TF-IDF vectorizer.
"""
import os
import pickle
import tarfile
import urllib.request
import email
import logging
from pathlib import Path
from sklearn.naive_bayes import MultinomialNB
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

CORPUS_URLS = {
    "spam": [
        "https://spamassassin.apache.org/old/publiccorpus/20050311_spam_2.tar.bz2",
        "https://spamassassin.apache.org/old/publiccorpus/20030228_spam.tar.bz2",
    ],
    "ham": [
        "https://spamassassin.apache.org/old/publiccorpus/20030228_easy_ham.tar.bz2",
        "https://spamassassin.apache.org/old/publiccorpus/20030228_easy_ham_2.tar.bz2",
    ]
}

CACHE_DIR = Path(__file__).parent / "corpus_cache"
MODEL_DIR = Path(__file__).parent
CACHE_DIR.mkdir(exist_ok=True)


def download_and_extract(url: str, label: str) -> list[str]:
    filename = CACHE_DIR / url.split("/")[-1]
    if not filename.exists():
        logger.info(f"Downloading {url}...")
        urllib.request.urlretrieve(url, filename)

    texts = []
    with tarfile.open(filename, "r:bz2") as tar:
        for member in tar.getmembers():
            if member.isfile() and not member.name.endswith("cmds"):
                f = tar.extractfile(member)
                if f:
                    try:
                        raw = f.read().decode("utf-8", errors="ignore")
                        msg = email.message_from_string(raw)
                        body = ""
                        if msg.is_multipart():
                            for part in msg.walk():
                                if part.get_content_type() == "text/plain":
                                    body += part.get_payload(decode=True).decode("utf-8", errors="ignore")
                        else:
                            body = msg.get_payload(decode=True)
                            if isinstance(body, bytes):
                                body = body.decode("utf-8", errors="ignore")
                        texts.append(str(body))
                    except Exception:
                        pass
    logger.info(f"Loaded {len(texts)} {label} samples from {url.split('/')[-1]}")
    return texts


def main():
    spam_texts, ham_texts = [], []

    for url in CORPUS_URLS["spam"]:
        spam_texts.extend(download_and_extract(url, "spam"))

    for url in CORPUS_URLS["ham"]:
        ham_texts.extend(download_and_extract(url, "ham"))

    texts = spam_texts + ham_texts
    labels = ["spam"] * len(spam_texts) + ["ham"] * len(ham_texts)

    logger.info(f"Total: {len(spam_texts)} spam, {len(ham_texts)} ham")

    vectorizer = TfidfVectorizer(
        max_features=30000,
        ngram_range=(1, 2),
        min_df=2,
        strip_accents="unicode",
        sublinear_tf=True,
    )
    X = vectorizer.fit_transform(texts)

    X_train, X_test, y_train, y_test = train_test_split(
        X, labels, test_size=0.2, random_state=42, stratify=labels
    )

    clf = MultinomialNB(alpha=0.1)
    clf.fit(X_train, y_train)

    y_pred = clf.predict(X_test)
    logger.info("\n" + classification_report(y_test, y_pred))

    with open(MODEL_DIR / "classifier.pkl", "wb") as f:
        pickle.dump(clf, f)
    with open(MODEL_DIR / "vectorizer.pkl", "wb") as f:
        pickle.dump(vectorizer, f)

    logger.info("Model saved to model/classifier.pkl and model/vectorizer.pkl")


if __name__ == "__main__":
    main()
