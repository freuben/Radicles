#!/usr/bin/env bash
# Install whatever sc3-plugins built: every plugin .so, plus the class files
# under each plugin group's sc/ folder (help files skipped).
set -euo pipefail
SRC=$1; DEST=$2
mkdir -p "$DEST/plugins" "$DEST/classes"
find "$SRC/build" -name "*.so" -exec cp {} "$DEST/plugins/" \;
cd "$SRC/source"
find . -path "*/sc/*" -name "*.sc" -not -path "*/HelpSource/*" | while read -r f; do
  mkdir -p "$DEST/classes/$(dirname "$f")"; cp "$f" "$DEST/classes/$f"
done
echo "installed $(ls "$DEST/plugins" | wc -l) plugins, $(find "$DEST/classes" -name '*.sc' | wc -l) class files"
