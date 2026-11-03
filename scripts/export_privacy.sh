#!/usr/bin/env bash
set -euo pipefail
privacy_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
exec python3 "$privacy_root/scripts/export_privacy.py" "$@"
