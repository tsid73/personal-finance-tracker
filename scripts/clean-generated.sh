#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

find "$ROOT_DIR/apps/web/src" -type f -name '*.js' -delete

echo "Removed generated JavaScript files from apps/web/src"
