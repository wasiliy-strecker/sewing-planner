#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
PACKAGE_TYPE=${1:-app-image}
case "$PACKAGE_TYPE" in
  app-image|dmg) ;;
  *) printf 'Aufruf: scripts/package.sh [app-image|dmg]\n' >&2; exit 1 ;;
esac
if [[ "$PACKAGE_TYPE" == dmg && "$(uname -s)" != Darwin ]]; then
  printf 'DMG-Pakete müssen auf macOS gebaut werden.\n' >&2
  exit 1
fi
if [[ -z "${JAVA_HOME:-}" && -x "$HOME/.local/jdk-21/bin/javac" ]]; then
  export JAVA_HOME="$HOME/.local/jdk-21"
fi
if [[ -z "${JAVA_HOME:-}" || ! -x "$JAVA_HOME/bin/javac" || ! -x "$JAVA_HOME/bin/jpackage" ]]; then
  printf 'JAVA_HOME muss auf ein vollständiges JDK 21 mit jpackage zeigen.\n' >&2
  exit 1
fi
export PATH="$JAVA_HOME/bin:$PATH"
cd "$ROOT"
# Verification is a separate, documented step. Packaging still compiles current sources.
./mvnw --batch-mode --no-transfer-progress package -DskipTests
VERSION=$(./mvnw --quiet -Dstyle.color=never help:evaluate -Dexpression=project.version -DforceStdout)
INPUT="$ROOT/target/package-input"
MODULES="$ROOT/target/javafx-modules"
IMAGE_DEST="$ROOT/target/app-image"
DIST="$ROOT/target/dist"
# These are generated staging directories only. User data never lives here.
rm -rf "$INPUT" "$MODULES" "$IMAGE_DEST"
mkdir -p "$INPUT/legal" "$MODULES" "$IMAGE_DEST" "$DIST"
./mvnw --batch-mode --no-transfer-progress dependency:copy-dependencies \
  -DincludeScope=runtime -DoutputDirectory="$INPUT"
cp "target/naehplan-$VERSION.jar" "$INPUT/"
cp THIRD_PARTY_NOTICES.md "$INPUT/legal/"
cp -R third-party-licenses/. "$INPUT/legal/"
# Keep JavaFX on the module path. The application and JDBC remain on the classpath.
for library in "$INPUT"/javafx-*.jar; do
  case "$library" in
    *-linux.jar|*-linux-aarch64.jar|*-mac.jar|*-mac-aarch64.jar|*-win.jar)
      mv "$library" "$MODULES/" ;;
    *) rm "$library" ;;
  esac
done
APP_NAME=NaehPlan
common=(--name "$APP_NAME" --app-version "$VERSION" --vendor "Wasiliy Strecker"
  --description "NähPlan – Nähprojekte mit Checkliste"
  --input "$INPUT" --main-jar "naehplan-$VERSION.jar"
  --main-class de.appfabrik.naehplan.Launcher
  --module-path "$JAVA_HOME/jmods:$MODULES"
  --add-modules java.se,javafx.controls,jdk.unsupported,jdk.localedata
  --java-options "--add-modules=javafx.controls"
  --java-options "-Dfile.encoding=UTF-8")
if [[ "$(uname -s)" == Linux ]]; then
  common+=(--icon "$ROOT/assets/branding/icon-256.png")
fi
if [[ "$(uname -s)" == Darwin ]]; then
  common+=(--mac-package-identifier de.appfabrik.naehplan --mac-package-name NaehPlan)
fi
"$JAVA_HOME/bin/jpackage" "${common[@]}" --type app-image --dest "$IMAGE_DEST"
if [[ "$(uname -s)" == Darwin ]]; then
  APP_IMAGE="$IMAGE_DEST/$APP_NAME.app"
  LAUNCHER="$APP_IMAGE/Contents/MacOS/$APP_NAME"
  PLATFORM=macos
else
  APP_IMAGE="$IMAGE_DEST/$APP_NAME"
  LAUNCHER="$APP_IMAGE/bin/$APP_NAME"
  PLATFORM=linux
fi
ACTUAL_VERSION=$("$LAUNCHER" --version)
if [[ "$ACTUAL_VERSION" != "NähPlan $VERSION" ]]; then
  printf 'Versionsprüfung des paketierten Launchers fehlgeschlagen.\n' >&2
  exit 1
fi
ARCHITECTURE=$(uname -m)
case "$ARCHITECTURE" in
  x86_64|amd64) ARCHITECTURE=x64 ;;
  aarch64|arm64) ARCHITECTURE=arm64 ;;
esac
if [[ "$PACKAGE_TYPE" == dmg ]]; then
  STAGING=$(mktemp -d "$ROOT/target/dmg.XXXXXX")
  "$JAVA_HOME/bin/jpackage" --type dmg --name "$APP_NAME" --app-version "$VERSION" \
    --app-image "$APP_IMAGE" --dest "$STAGING" --vendor "Wasiliy Strecker"
  OUTPUT="$DIST/naehplan-$VERSION-$PLATFORM-$ARCHITECTURE.dmg"
  if [[ -e "$OUTPUT" ]]; then
    printf 'Das bereits vorhandene Paket wird nicht überschrieben: %s\n' "$OUTPUT" >&2
    exit 1
  fi
  mv "$STAGING/$APP_NAME-$VERSION.dmg" "$OUTPUT"
  rmdir "$STAGING"
  printf 'DMG erstellt, native Abnahme separat durchführen: %s\n' "$OUTPUT"
fi
printf 'App-Image erstellt und Launcher-Version geprüft: %s\n' "$APP_IMAGE"
printf 'GUI-Abnahme: "%s" --data-dir=/absoluter/pfad/zur/testsammlung\n' "$LAUNCHER"
