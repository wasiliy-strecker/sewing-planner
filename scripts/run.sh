#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
if [[ -z "${JAVA_HOME:-}" && -x "$HOME/.local/share/naehplan/jdk-21.0.13+9/bin/javac" ]]; then
  export JAVA_HOME="$HOME/.local/share/naehplan/jdk-21.0.13+9"
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi
cd "$ROOT"
exec ./mvnw --no-transfer-progress javafx:run "$@"
