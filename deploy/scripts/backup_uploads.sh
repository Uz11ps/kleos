#!/usr/bin/env sh
set -eu

SOURCE_DIR="${1:-./data/uploads}"
BACKUP_DIR="${2:-./backups}"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
ARCHIVE_PATH="${BACKUP_DIR}/uploads_${TIMESTAMP}.tar.gz"

if [ ! -d "$SOURCE_DIR" ]; then
  echo "Source directory not found: $SOURCE_DIR"
  exit 1
fi

mkdir -p "$BACKUP_DIR"

tar -czf "$ARCHIVE_PATH" -C "$SOURCE_DIR" .

echo "Backup created: $ARCHIVE_PATH"
