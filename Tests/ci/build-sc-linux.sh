#!/usr/bin/env bash
# Rebuild a headless SuperCollider test environment for Radicles in the
# cloud workspace (Ubuntu 24.04, no apt access; everything from GitHub).
# Takes ~25 min on 2 cores. Safe to re-run: skips steps already done.
# Usage: Tests/ci/build-sc-linux.sh   (then Tests/ci/run-headless.sh)
set -euo pipefail
SRC=${SRC:-/home/claude/src}
SC_TAG=${SC_TAG:-Version-3.14.1}
CI=$(cd "$(dirname "$0")" && pwd)
# Headless-only classes live OUTSIDE the Radicles Quark, so users never compile them.
SUPPORT=${SUPPORT:-/home/claude/radtest-support}
mkdir -p "$SUPPORT"
for f in "$CI"/headless-classes/*.sc.in; do cp "$f" "$SUPPORT/$(basename "$f" .in)"; done
mkdir -p "$SRC"
clone() { [ -d "$2/.git" ] || GIT_LFS_SKIP_SMUDGE=1 git clone -q --depth 1 ${3:-} "https://github.com/$1" "$2"; }

# 1. libsndfile (no external codecs)
if ! pkg-config --exists sndfile; then
  clone libsndfile/libsndfile "$SRC/libsndfile"
  cmake -S "$SRC/libsndfile" -B "$SRC/libsndfile/build" -G Ninja -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=ON -DENABLE_EXTERNAL_LIBS=OFF -DENABLE_MPEG=OFF -DBUILD_PROGRAMS=OFF \
    -DBUILD_EXAMPLES=OFF -DBUILD_TESTING=OFF -DCMAKE_INSTALL_PREFIX=/usr/local
  ninja -C "$SRC/libsndfile/build" install
fi

# 2. JACK2 with the dummy driver (lets scsynth run with no sound card)
if ! pkg-config --exists jack; then
  clone jackaudio/jack2 "$SRC/jack2"
  (cd "$SRC/jack2" && python3 ./waf configure --prefix=/usr/local --alsa=no --firewire=no \
     --iio=no --portaudio=no --db=no --systemd=no --classic && python3 ./waf build -j2 && python3 ./waf install)
fi
ldconfig

# 3. SuperCollider without Qt/IDE
if ! command -v sclang >/dev/null; then
  clone supercollider/supercollider "$SRC/supercollider" "--branch $SC_TAG --recurse-submodules --shallow-submodules"
  cmake -S "$SRC/supercollider" -B "$SRC/supercollider/build" -G Ninja -DCMAKE_BUILD_TYPE=Release \
    -DSC_QT=OFF -DSC_IDE=OFF -DSUPERNOVA=OFF -DNO_X11=ON -DSC_EL=OFF -DSC_VIM=OFF -DSC_ED=OFF \
    -DNO_AVAHI=ON -DSC_ABLETON_LINK=OFF -DSC_HIDAPI=OFF -DINSTALL_HELP=OFF -DCMAKE_INSTALL_PREFIX=/usr/local
  ninja -C "$SRC/supercollider/build" -j2 install
  ldconfig
fi

# 4. sc3-plugins (Radicles ext dependency). No FFTW available: build every
#    plugin that compiles without it and install what succeeded.
EXT=/root/.local/share/SuperCollider/Extensions
if [ ! -d "$EXT/SC3plugins" ]; then
  clone supercollider/sc3-plugins "$SRC/sc3-plugins" "--recurse-submodules --shallow-submodules"
  mkdir -p /tmp/nofftw
  cmake -S "$SRC/sc3-plugins" -B "$SRC/sc3-plugins/build" -G Ninja -DSC_PATH="$SRC/supercollider" \
    -DCMAKE_BUILD_TYPE=Release -DSUPERNOVA=OFF -DQUARKS=OFF -DFFTW3F_INCLUDE_DIR=/tmp/nofftw \
    -DFFTW3F_LIBRARY=/usr/lib/x86_64-linux-gnu/libm.so.6
  ninja -C "$SRC/sc3-plugins/build" -k 0 -j2 || true
  "$CI/install-sc3-plugins.sh" "$SRC/sc3-plugins" "$EXT/SC3plugins"
fi

# 5. Quarks and the GUI class library (classes only; no Qt primitives)
Q=/root/.local/share/SuperCollider/downloaded-quarks
mkdir -p "$Q"
clone supercollider-quarks/wslib "$Q/wslib"
clone freuben/RadiclesLibs "$Q/RadiclesLibs"
[ -e "$Q/Radicles" ] || ln -s "${RADICLES:-/home/claude/radicles}" "$Q/Radicles"
G=/home/claude/sc-gui-classes
if [ ! -d "$G" ]; then
  for d in Common/GUI Common/Quarks/GUI Common/UnitTesting/GUI JITLib/GUI Platform/linux/GUI deprecated/3.10/GUI deprecated/3.13/GUI; do
    [ -d "$SRC/supercollider/SCClassLibrary/$d" ] || continue
    mkdir -p "$G/$(dirname $d)"; cp -r "$SRC/supercollider/SCClassLibrary/$d" "$G/$d"
  done
fi

# 6. RadiclesFiles skeleton and sclang config
F="$EXT/RadiclesFiles"
mkdir -p "$F/Settings" "$F/SoundFiles/Record"
for i in 0 1 2 3 4 5 6 7 8 9; do mkdir -p "$F/SoundFiles/Play/$i"; done
# Empty preset files, as an existing install has them (presets silently fail without).
mkdir -p "$F/Settings/Presets" "$F/Settings/Sessions"
for c in Track Tracks Radicles Filter Session Bstore Map Dstore Assemblage Store; do
  [ -f "$F/Settings/Presets/$c.scd" ] || echo "[]" > "$F/Settings/Presets/$c.scd"
done
mkdir -p /root/.config/SuperCollider
cat > /root/.config/SuperCollider/sclang_conf.yaml <<EOF
includePaths:
    - $SUPPORT
    - $G
    - $Q/Radicles
    - $Q/RadiclesLibs
    - $Q/wslib
excludePaths: []
postInlineWarnings: false
EOF
echo "Headless SuperCollider ready: $(sclang -v 2>/dev/null | head -1)"
