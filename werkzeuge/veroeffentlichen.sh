#!/usr/bin/env bash
# Baut die signierte Release-APK und legt sie mit update/version.json ab.
# Danach committen und auf main pushen: Die App findet das Update von selbst (einmal am Tag oder per „Nach Update suchen“).
#
# Aufruf:  LERNEN_KEYSTORE=/pfad/lernen.jks LERNEN_KEYSTORE_PASSWORT=… werkzeuge/veroeffentlichen.sh "Was ist neu"
set -euo pipefail
cd "$(dirname "$0")/.."
: "${LERNEN_KEYSTORE:?Pfad zu lernen.jks fehlt}"
: "${LERNEN_KEYSTORE_PASSWORT:?Passwort fehlt}"
export LERNEN_KEY_ALIAS="${LERNEN_KEY_ALIAS:-lernen}"
export LERNEN_KEY_PASSWORT="${LERNEN_KEY_PASSWORT:-$LERNEN_KEYSTORE_PASSWORT}"
VERSION=$(grep -oP 'versionName = "\K[^"]+' app/build.gradle.kts)
NOTIZEN="${1:-Neue Version $VERSION}"

./gradlew test assembleRelease
mkdir -p update
rm -f update/lernen-*.apk
cp app/build/outputs/apk/release/app-release.apk "update/lernen-$VERSION.apk"
python3 - "$VERSION" "$NOTIZEN" <<'PY'
import json, sys
v, n = sys.argv[1], sys.argv[2]
json.dump({"version": v, "apk": f"update/lernen-{v}.apk", "notizen": n}, open("update/version.json", "w"), ensure_ascii=False, indent=2)
PY
echo "Fertig: update/lernen-$VERSION.apk und update/version.json – jetzt committen und auf main pushen."
