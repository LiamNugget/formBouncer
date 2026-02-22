"""
Enron-Spam Dataset Source
--------------------------
Downloads and parses the Enron-Spam preprocessed corpus.
~33,000 emails across 6 subsets: ~16,500 spam + ~16,500 ham.

Source: http://www.aueb.gr/users/ion/data/enron-spam/
Paper: V. Metsis, I. Androutsopoulos and G. Paliouras, "Spam Filtering with Naive Bayes —
       Which Naive Bayes?" CEAS 2006.
"""
import logging
import ssl
import tarfile
import urllib.request
from pathlib import Path

# AUEB server uses a certificate that doesn't verify cleanly — bypass for this academic dataset
_SSL_CONTEXT = ssl.create_default_context()
_SSL_CONTEXT.check_hostname = False
_SSL_CONTEXT.verify_mode = ssl.CERT_NONE

logger = logging.getLogger(__name__)

# 6 Enron employee mailboxes, each with spam/ and ham/ subdirectories
SUBSET_URLS = [
    "http://www.aueb.gr/users/ion/data/enron-spam/preprocessed/enron1.tar.gz",
    "http://www.aueb.gr/users/ion/data/enron-spam/preprocessed/enron2.tar.gz",
    "http://www.aueb.gr/users/ion/data/enron-spam/preprocessed/enron3.tar.gz",
    "http://www.aueb.gr/users/ion/data/enron-spam/preprocessed/enron4.tar.gz",
    "http://www.aueb.gr/users/ion/data/enron-spam/preprocessed/enron5.tar.gz",
    "http://www.aueb.gr/users/ion/data/enron-spam/preprocessed/enron6.tar.gz",
]


def load(unprocessed_dir: Path) -> list[dict]:
    """
    Download (if needed) and parse all Enron-Spam subsets.
    Returns a list of records with text, label, source.
    """
    cache_dir = unprocessed_dir / "enron"
    cache_dir.mkdir(parents=True, exist_ok=True)

    records = []
    for url in SUBSET_URLS:
        records.extend(_load_subset(url, cache_dir))

    logger.info(f"[enron] Loaded {len(records)} records total")
    return records


def _load_subset(url: str, cache_dir: Path) -> list[dict]:
    filename = cache_dir / url.split("/")[-1]

    if not filename.exists():
        logger.info(f"[enron] Downloading {filename.name}...")
        try:
            with urllib.request.urlopen(url, context=_SSL_CONTEXT) as response:
                with open(filename, "wb") as f:
                    f.write(response.read())
            logger.info(f"[enron] Downloaded {filename.name}")
        except Exception as e:
            logger.warning(f"[enron] Failed to download {url}: {e}")
            return []

    records = []
    try:
        with tarfile.open(filename, "r:gz") as tar:
            for member in tar.getmembers():
                if not member.isfile():
                    continue

                # Path structure: enron1/spam/0001.2003-12-18.GP.spam.txt
                #                 enron1/ham/0001.2003-12-18.GP.ham.txt
                parts = Path(member.name).parts
                if len(parts) < 2:
                    continue

                parent = parts[-2].lower()
                if parent not in ("spam", "ham"):
                    continue

                label = parent
                f = tar.extractfile(member)
                if not f:
                    continue

                try:
                    text = f.read().decode("utf-8", errors="ignore")
                    text = _clean(text)
                    if text:
                        records.append({"text": text, "label": label, "source": "enron"})
                except Exception:
                    pass

    except Exception as e:
        logger.warning(f"[enron] Error reading {filename.name}: {e}")

    spam_count = sum(1 for r in records if r["label"] == "spam")
    ham_count = len(records) - spam_count
    logger.info(f"[enron] {filename.name}: {spam_count} spam, {ham_count} ham")
    return records


def _clean(text: str) -> str:
    """
    Enron preprocessed files are already plain text (no RFC 2822 headers).
    Just normalise whitespace and truncate.
    """
    text = " ".join(text.split())
    return text[:10_000]
