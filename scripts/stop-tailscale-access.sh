#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"
REMOTE_PID_FILE="$LOG_DIR/tailscale-remote.pid"
REMOTE_SERVER="$ROOT_DIR/scripts/tailscale-remote-server.mjs"

if [ -f "$REMOTE_PID_FILE" ]; then
  if kill -0 "$(cat "$REMOTE_PID_FILE")" 2>/dev/null; then
    kill "$(cat "$REMOTE_PID_FILE")" 2>/dev/null || true
  fi
  rm -f "$REMOTE_PID_FILE"
fi

pkill -f "$REMOTE_SERVER" >/dev/null 2>&1 || true

echo "Tailscale remote access stopped"
