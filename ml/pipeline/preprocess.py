"""
FormShield Data Preprocessing Pipeline
----------------------------------------
Orchestrates all corpus sources, deduplicates, splits, and writes
standardised JSONL files to data/processed/.

Usage:
    python ml/pipeline/preprocess.py [--force]

    --force   Re-download all corpora even if cached

Output:
    data/processed/train.jsonl  (80% split)
    data/processed/test.jsonl   (20% split)

See data/processed/schema.md for the record format.
"""
import argparse
import hashlib
import json
import logging
import random
import sys
from datetime import datetime, timezone
from pathlib import Path

# Resolve project root (two levels up from this file: ml/pipeline/preprocess.py)
PROJECT_ROOT = Path(__file__).resolve().parents[2]
UNPROCESSED_DIR = PROJECT_ROOT / "data" / "unprocessed"
PROCESSED_DIR = PROJECT_ROOT / "data" / "processed"

# Ensure pipeline modules are importable regardless of working directory
sys.path.insert(0, str(Path(__file__).resolve().parent))

from sources import spamassassin, enron, ling_spam  # noqa: E402

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger(__name__)

TRAIN_RATIO = 0.8
RANDOM_SEED = 42


def main(force: bool = False) -> None:
    PROCESSED_DIR.mkdir(parents=True, exist_ok=True)

    logger.info("=== FormShield Preprocessing Pipeline ===")
    logger.info(f"Unprocessed dir : {UNPROCESSED_DIR}")
    logger.info(f"Processed dir   : {PROCESSED_DIR}")

    # 1. Load all sources
    all_records: list[dict] = []

    logger.info("--- Loading SpamAssassin corpus ---")
    all_records.extend(spamassassin.load(UNPROCESSED_DIR))

    logger.info("--- Loading Enron-Spam corpus ---")
    all_records.extend(enron.load(UNPROCESSED_DIR))

    logger.info("--- Loading Ling-Spam corpus ---")
    all_records.extend(ling_spam.load(UNPROCESSED_DIR))

    logger.info(f"Total raw records: {len(all_records)}")

    # 2. Deduplicate by content hash
    all_records = _deduplicate(all_records)
    logger.info(f"After deduplication: {len(all_records)}")

    # 3. Stamp processed_at timestamp
    now = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S")
    for record in all_records:
        record["processed_at"] = now

    # 4. Stratified 80/20 split
    train, test = _stratified_split(all_records, TRAIN_RATIO, RANDOM_SEED)

    _log_split_stats("Train", train)
    _log_split_stats("Test", test)

    # 5. Write JSONL files
    _write_jsonl(PROCESSED_DIR / "train.jsonl", train)
    _write_jsonl(PROCESSED_DIR / "test.jsonl", test)

    logger.info("=== Done ===")
    logger.info(f"  {PROCESSED_DIR / 'train.jsonl'}  ({len(train)} records)")
    logger.info(f"  {PROCESSED_DIR / 'test.jsonl'}   ({len(test)} records)")


def _deduplicate(records: list[dict]) -> list[dict]:
    """Remove records with identical text content."""
    seen: set[str] = set()
    unique: list[dict] = []
    for record in records:
        h = hashlib.md5(record["text"].encode(), usedforsecurity=False).hexdigest()
        if h not in seen:
            seen.add(h)
            unique.append(record)
    duplicates_removed = len(records) - len(unique)
    if duplicates_removed:
        logger.info(f"Removed {duplicates_removed} duplicate records")
    return unique


def _stratified_split(
    records: list[dict], train_ratio: float, seed: int
) -> tuple[list[dict], list[dict]]:
    """
    Split records into train/test while preserving the spam/ham ratio in each split.
    """
    rng = random.Random(seed)

    spam = [r for r in records if r["label"] == "spam"]
    ham = [r for r in records if r["label"] == "ham"]

    rng.shuffle(spam)
    rng.shuffle(ham)

    spam_train = spam[: int(len(spam) * train_ratio)]
    spam_test = spam[int(len(spam) * train_ratio):]
    ham_train = ham[: int(len(ham) * train_ratio)]
    ham_test = ham[int(len(ham) * train_ratio):]

    train = spam_train + ham_train
    test = spam_test + ham_test

    rng.shuffle(train)
    rng.shuffle(test)

    return train, test


def _log_split_stats(name: str, records: list[dict]) -> None:
    spam = sum(1 for r in records if r["label"] == "spam")
    ham = len(records) - spam
    sources: dict[str, int] = {}
    for r in records:
        sources[r["source"]] = sources.get(r["source"], 0) + 1

    source_str = ", ".join(f"{k}: {v}" for k, v in sorted(sources.items()))
    logger.info(f"{name}: {len(records)} total | {spam} spam, {ham} ham | {source_str}")


def _write_jsonl(path: Path, records: list[dict]) -> None:
    with open(path, "w", encoding="utf-8") as f:
        for record in records:
            f.write(json.dumps(record, ensure_ascii=False) + "\n")
    logger.info(f"Wrote {path.name}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="FormShield preprocessing pipeline")
    parser.add_argument(
        "--force",
        action="store_true",
        help="Re-download all corpora even if cached in data/unprocessed/",
    )
    args = parser.parse_args()
    main(force=args.force)
