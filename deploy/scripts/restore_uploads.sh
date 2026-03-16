#!/usr/bin/env sh
set -eu

ARCHIVE_PATH="${1:-}"
TARGET_DIR="${2:-./data/uploads}"

if [ -z "$ARCHIVE_PATH" ]; then
  echo "Usage: ./restore_uploads.sh <archive.tar.gz> [target_dir]"
  exit 1
fi

if [ ! -f "$ARCHIVE_PATH" ]; then
  echo "Archive not found: $ARCHIVE_PATH"
  exit 1
fi

mkdir -p "$TARGET_DIR"
tar -xzf "$ARCHIVE_PATH" -C "$TARGET_DIR"

echo "Backup restored to: $TARGET_DIR"
