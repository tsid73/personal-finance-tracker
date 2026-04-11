#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="${ROOT_DIR}/backups"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUTPUT_PATH="${1:-$BACKUP_DIR/personal_finance-${TIMESTAMP}.sql.gz}"

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

if [ ! -f "$ROOT_DIR/.env" ]; then
  echo ".env is required" >&2
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

mkdir -p "$(dirname "$OUTPUT_PATH")"

"$DOCKER_BIN" exec personal-finance-mysql sh -lc \
  "exec mysqldump -u\"$MYSQL_USER\" -p\"$MYSQL_PASSWORD\" --databases \"$MYSQL_DATABASE\" --single-transaction --quick --skip-lock-tables" \
  | gzip -c > "$OUTPUT_PATH"

echo "Backup written to $OUTPUT_PATH"
