"""
SpamAssassin Public Corpus Source
----------------------------------
Downloads and parses the SpamAssassin public email corpus.
~6,000 emails: ~2,500 spam + ~3,500 ham.

Source: https://spamassassin.apache.org/old/publiccorpus/
"""
import email
import logging
import tarfile
import urllib.request
from pathlib import Path

logger = logging.getLogger(__name__)

CORPUS_URLS = {
    "spam": [
        "https://spamassassin.apache.org/old/publiccorpus/20050311_spam_2.tar.bz2",
        "https://spamassassin.apache.org/old/publiccorpus/20030228_spam.tar.bz2",
    ],
    "ham": [
        "https://spamassassin.apache.org/old/publiccorpus/20030228_easy_ham.tar.bz2",
        "https://spamassassin.apache.org/old/publiccorpus/20030228_easy_ham_2.tar.bz2",
    ],
}


def load(unprocessed_dir: Path) -> list[dict]:
    """
    Download (if needed) and parse all SpamAssassin corpus archives.
    Returns a list of records with text, label, source.
    """
    cache_dir = unprocessed_dir / "spamassassin"
    cache_dir.mkdir(parents=True, exist_ok=True)

    records = []
    for label, urls in CORPUS_URLS.items():
        for url in urls:
            records.extend(_load_archive(url, label, cache_dir))

    logger.info(f"[spamassassin] Loaded {len(records)} records total")
    return records


def _load_archive(url: str, label: str, cache_dir: Path) -> list[dict]:
    filename = cache_dir / url.split("/")[-1]

    if not filename.exists():
        logger.info(f"[spamassassin] Downloading {url.split('/')[-1]}...")
        urllib.request.urlretrieve(url, filename)
        logger.info(f"[spamassassin] Downloaded {filename.name}")

    records = []
    with tarfile.open(filename, "r:bz2") as tar:
        for member in tar.getmembers():
            if not member.isfile() or member.name.endswith("cmds"):
                continue
            f = tar.extractfile(member)
            if not f:
                continue
            try:
                raw = f.read().decode("utf-8", errors="ignore")
                text = _extract_body(raw)
                if text:
                    records.append({"text": text, "label": label, "source": "spamassassin"})
            except Exception:
                pass

    logger.info(f"[spamassassin] Parsed {len(records)} {label} records from {filename.name}")
    return records


def _extract_body(raw: str) -> str:
    """Parse RFC 2822 email and return the plain text body."""
    msg = email.message_from_string(raw)
    body = ""

    if msg.is_multipart():
        for part in msg.walk():
            if part.get_content_type() == "text/plain":
                payload = part.get_payload(decode=True)
                if isinstance(payload, bytes):
                    body += payload.decode("utf-8", errors="ignore")
    else:
        payload = msg.get_payload(decode=True)
        if isinstance(payload, bytes):
            body = payload.decode("utf-8", errors="ignore")
        elif isinstance(payload, str):
            body = payload

    return _clean(body)


def _clean(text: str) -> str:
    """Normalise whitespace and truncate."""
    text = " ".join(text.split())
    return text[:10_000]
