"""
Ling-Spam Dataset Source
-------------------------
Downloads and parses the Ling-Spam corpus (bare, non-lemmatised version).
~2,893 emails: ~481 spam + ~2,412 ham (linguistics mailing list messages).

Source: http://nlp.cs.aueb.gr/software_and_datasets/lingspam_public.tar.gz
Paper: I. Androutsopoulos et al., "An Evaluation of Naive Bayesian Anti-Spam Filtering"
       ECML 2000 Workshop on Machine Learning in the New Information Age.

Note: Ling-Spam is smaller than SpamAssassin/Enron but adds domain diversity —
the ham emails are from a linguistics academic mailing list, which is a good contrast
to the corporate email ham in Enron.
"""
import logging
import tarfile
import urllib.request
from pathlib import Path

logger = logging.getLogger(__name__)

DATASET_URL = "https://github.com/AshtonSBradley/lingspam_public/archive/refs/heads/master.tar.gz"


def load(unprocessed_dir: Path) -> list[dict]:
    """
    Download (if needed) and parse the Ling-Spam bare corpus.
    Returns a list of records with text, label, source.
    """
    cache_dir = unprocessed_dir / "ling_spam"
    cache_dir.mkdir(parents=True, exist_ok=True)

    filename = cache_dir / "lingspam_public_master.tar.gz"

    if not filename.exists():
        logger.info(f"[ling_spam] Downloading lingspam_public.tar.gz...")
        try:
            urllib.request.urlretrieve(DATASET_URL, filename)
            logger.info(f"[ling_spam] Downloaded {filename.name}")
        except Exception as e:
            logger.warning(f"[ling_spam] Failed to download: {e}")
            return []

    records = []
    try:
        with tarfile.open(filename, "r:gz") as tar:
            for member in tar.getmembers():
                if not member.isfile() or not member.name.endswith(".txt"):
                    continue

                # Ling-Spam structure: lingspam_public/bare/partN/spmsgNNN.txt  (spam)
                #                      lingspam_public/bare/partN/msgNNNN.txt   (ham)
                # "bare" = not stop-word removed, not lemmatised
                path = Path(member.name)

                # Only use the "bare" (unprocessed) variant
                if "bare" not in path.parts:
                    continue

                filename_stem = path.stem
                # Spam files start with "spmsg", ham files start with "msg"
                if filename_stem.startswith("spmsg"):
                    label = "spam"
                elif filename_stem.startswith("msg"):
                    label = "ham"
                else:
                    continue

                f = tar.extractfile(member)
                if not f:
                    continue

                try:
                    text = f.read().decode("utf-8", errors="ignore")
                    text = _clean(text)
                    if text:
                        records.append({"text": text, "label": label, "source": "ling_spam"})
                except Exception:
                    pass

    except Exception as e:
        logger.warning(f"[ling_spam] Error reading archive: {e}")

    spam_count = sum(1 for r in records if r["label"] == "spam")
    ham_count = len(records) - spam_count
    logger.info(f"[ling_spam] Loaded {spam_count} spam, {ham_count} ham")
    return records


def _clean(text: str) -> str:
    """Normalise whitespace and truncate."""
    text = " ".join(text.split())
    return text[:10_000]
