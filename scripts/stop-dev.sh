#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"
API_PID_FILE="$LOG_DIR/api.pid"
WEB_PID_FILE="$LOG_DIR/web.pid"
API_CMD="$ROOT_DIR/apps/api/dist/server.js"
WEB_CMD_PATTERN="$ROOT_DIR/node_modules/.bin/vite --host 127.0.0.1"

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

if [ -f "$API_PID_FILE" ]; then
  if kill -0 "$(cat "$API_PID_FILE")" 2>/dev/null; then
    kill "$(cat "$API_PID_FILE")" 2>/dev/null || true
  fi
  rm -f "$API_PID_FILE"
fi

if [ -f "$WEB_PID_FILE" ]; then
  if kill -0 "$(cat "$WEB_PID_FILE")" 2>/dev/null; then
    kill "$(cat "$WEB_PID_FILE")" 2>/dev/null || true
  fi
  rm -f "$WEB_PID_FILE"
fi

pkill -f "$API_CMD" >/dev/null 2>&1 || true
pkill -f "$WEB_CMD_PATTERN" >/dev/null 2>&1 || true
fuser -k 4000/tcp >/dev/null 2>&1 || true
fuser -k 5173/tcp >/dev/null 2>&1 || true

cd "$ROOT_DIR"
DOCKER_BIN="$(resolve_docker_bin || true)"

if [ -n "$DOCKER_BIN" ]; then
  "$DOCKER_BIN" compose down >/dev/null 2>&1 || true
fi

echo "App processes stopped"
