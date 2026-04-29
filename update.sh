#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SERVICE_NAME="${SERVICE_NAME:-myblog-app.service}"

command -v systemctl >/dev/null 2>&1 || {
    echo "Command not found: systemctl" >&2
    exit 1
}

echo "Applying service configuration..."
"$ROOT_DIR/install.sh"
systemctl --user --no-pager --full status "$SERVICE_NAME" || true
