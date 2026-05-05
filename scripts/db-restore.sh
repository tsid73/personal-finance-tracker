#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INPUT_PATH="${1:-}"

resolve_docker_bin() {
  if command -v docker >/dev/null 2>&1; then
    command -v docker
    return 0
  fi

  local candidate
  for candidate in \
    "/mnt/c/Program Files/Docker/Docker/resources/bin/docker.exe" \
    "/mnt/c/Program Files/Docker/cli-plugins/docker.exe" \
    "/usr/bin/docker" \
    "/bin/docker"
  do
    if [ -x "$candidate" ]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  return 1
}

if [ -z "$INPUT_PATH" ]; then
  echo "Usage: bash scripts/db-restore.sh /path/to/backup.sql.gz" >&2
  exit 1
fi

if [ ! -f "$ROOT_DIR/.env" ]; then
  echo ".env is required" >&2
  exit 1
fi

if [ ! -f "$INPUT_PATH" ]; then
  echo "Backup file not found: $INPUT_PATH" >&2
  exit 1
fi

set -a
. "$ROOT_DIR/.env"
set +a

if ! DOCKER_BIN="$(resolve_docker_bin)"; then
  echo "Docker is required" >&2
  exit 1
fi

if ! "$DOCKER_BIN" version >/dev/null 2>&1; then
  echo "Docker is installed but the daemon is not running." >&2
  exit 1
fi

case "$INPUT_PATH" in
  *.gz)
    gzip -dc "$INPUT_PATH"
    ;;
  *)
    cat "$INPUT_PATH"
    ;;
esac | "$DOCKER_BIN" exec -i personal-finance-mysql sh -lc \
  "exec mysql -u\"$MYSQL_USER\" -p\"$MYSQL_PASSWORD\" \"$MYSQL_DATABASE\""

echo "Restore completed from $INPUT_PATH"
