#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"
REMOTE_LOG="$LOG_DIR/tailscale-remote.log"
REMOTE_PID_FILE="$LOG_DIR/tailscale-remote.pid"
REMOTE_SERVER="$ROOT_DIR/scripts/tailscale-remote-server.mjs"
REMOTE_PORT="${REMOTE_PORT:-5173}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "$1 is required." >&2
    exit 1
  fi
}

require_command tailscale
require_command node
mkdir -p "$LOG_DIR"
cd "$ROOT_DIR"

if ! tailscale status >/dev/null 2>&1; then
  echo "Tailscale is installed but not connected. Start Tailscale first." >&2
  exit 1
fi

TAILSCALE_IP="$(tailscale status --json | node -e 'let data=""; process.stdin.on("data", (chunk) => data += chunk); process.stdin.on("end", () => { const status = JSON.parse(data); const ip = status?.Self?.TailscaleIPs?.find((value) => typeof value === "string" && value.includes(".")); if (!ip) process.exit(1); process.stdout.write(ip); });')"
DNS_NAME="$(tailscale status --json | node -e 'let data=""; process.stdin.on("data", (chunk) => data += chunk); process.stdin.on("end", () => { const status = JSON.parse(data); const dns = (status?.Self?.DNSName ?? "").replace(/\.$/, ""); if (!dns) process.exit(1); process.stdout.write(dns); });')"

if ! curl -sf http://127.0.0.1:4000/api/health >/dev/null 2>&1 || ! curl -sf http://127.0.0.1:5173 >/dev/null 2>&1; then
  bash "$ROOT_DIR/scripts/start-dev.sh"
fi

npm run build --workspace @finance/web >/dev/null

if [ -f "$REMOTE_PID_FILE" ] && kill -0 "$(cat "$REMOTE_PID_FILE")" 2>/dev/null; then
  kill "$(cat "$REMOTE_PID_FILE")" 2>/dev/null || true
fi
rm -f "$REMOTE_PID_FILE"
pkill -f "$REMOTE_SERVER" >/dev/null 2>&1 || true

: > "$REMOTE_LOG"
setsid -f bash -lc "cd '$ROOT_DIR' && exec env TAILSCALE_BIND_IP='$TAILSCALE_IP' REMOTE_PORT='$REMOTE_PORT' node '$REMOTE_SERVER' >> '$REMOTE_LOG' 2>&1"
sleep 2
pgrep -n -f "$REMOTE_SERVER" > "$REMOTE_PID_FILE"

for _ in $(seq 1 20); do
  if curl -sf "http://$TAILSCALE_IP:$REMOTE_PORT/health" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

if ! curl -sf "http://$TAILSCALE_IP:$REMOTE_PORT/health" >/dev/null 2>&1; then
  echo "Remote access server did not become ready in time." >&2
  exit 1
fi

echo "Tailscale remote access started"
echo "Tailnet IP URL: http://$TAILSCALE_IP:$REMOTE_PORT"
echo "MagicDNS URL: http://$DNS_NAME:$REMOTE_PORT"
echo "Logs: $REMOTE_LOG"
