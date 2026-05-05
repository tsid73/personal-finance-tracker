#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"
API_LOG="$LOG_DIR/api.log"
WEB_LOG="$LOG_DIR/web.log"
API_PID_FILE="$LOG_DIR/api.pid"
WEB_PID_FILE="$LOG_DIR/web.pid"
API_CMD="$ROOT_DIR/apps/api/dist/server.js"
WEB_ROOT="$ROOT_DIR/apps/web"
WEB_CMD="$ROOT_DIR/node_modules/.bin/vite --host 127.0.0.1"

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

mkdir -p "$LOG_DIR"
cd "$ROOT_DIR"

if [ ! -f .env ] && [ -f .env.example ]; then
  cp .env.example .env
fi

set -a
. "$ROOT_DIR/.env"
set +a

if [ ! -d node_modules ]; then
  npm install
fi

if ! DOCKER_BIN="$(resolve_docker_bin)"; then
  echo "Docker is required to start the bundled MySQL service." >&2
  exit 1
fi

if ! "$DOCKER_BIN" version >/dev/null 2>&1; then
  echo "Docker is installed but the daemon is not running." >&2
  exit 1
fi

bash "$ROOT_DIR/scripts/stop-dev.sh" >/dev/null 2>&1 || true

"$DOCKER_BIN" compose up -d

echo "Waiting for MySQL to become ready..."
ready=0
for _ in $(seq 1 60); do
  if "$DOCKER_BIN" exec personal-finance-mysql mysqladmin ping -h127.0.0.1 -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" --silent >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 2
done

if [ "$ready" -ne 1 ]; then
  echo "MySQL did not become ready in time." >&2
  exit 1
fi

npm run db:migrate >/dev/null
npm run build --workspace @finance/shared >/dev/null
npm run build --workspace @finance/api >/dev/null

: > "$API_LOG"
: > "$WEB_LOG"

setsid -f bash -lc "cd '$ROOT_DIR' && exec node '$API_CMD' >> '$API_LOG' 2>&1"
sleep 2
pgrep -n -f "$API_CMD" > "$API_PID_FILE"

setsid -f bash -lc "cd '$WEB_ROOT' && exec $WEB_CMD >> '$WEB_LOG' 2>&1"
sleep 3
pgrep -n -f "$ROOT_DIR/node_modules/.bin/vite --host 127.0.0.1" > "$WEB_PID_FILE"

for _ in $(seq 1 30); do
  if curl -sf http://127.0.0.1:4000/api/health >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

for _ in $(seq 1 30); do
  if curl -sf http://127.0.0.1:5173 >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

if command -v powershell.exe >/dev/null 2>&1; then
  powershell.exe -NoProfile -Command "Start-Process 'http://localhost:5173'" >/dev/null 2>&1 || true
fi

echo "App started"
echo "API: http://127.0.0.1:4000"
echo "Web: http://127.0.0.1:5173"
echo "Logs: $LOG_DIR"
echo "The app runs in the background and the terminal returns immediately."
