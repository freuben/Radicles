#!/usr/bin/env bash
# GitHub Actions only (.github/workflows/tests.yml): prepare an ubuntu-24.04
# runner for build-sc-linux.sh and run-headless.sh.
# Everything is installed into folders the runner's own user owns
# ($HOME/radtest/...), so actions/cache can save and restore them without
# root, and nothing in /usr/local is touched. sudo is used only for the
# build tools from apt and for telling the system linker about the new
# libraries folder (one file in /etc/ld.so.conf.d, then ldconfig).
set -euo pipefail
: "${GITHUB_ENV:?run this from a GitHub Actions step}"
RT=$HOME/radtest
PREFIX=$RT/usr
DATA=${XDG_DATA_HOME:-$HOME/.local/share}/SuperCollider
CONF=${XDG_CONFIG_HOME:-$HOME/.config}/SuperCollider

sudo apt-get update -q
sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -q --no-install-recommends \
  build-essential cmake ninja-build pkg-config git python3 unzip

mkdir -p "$PREFIX"
printf '%s\n' "$PREFIX/lib" "$PREFIX/lib/x86_64-linux-gnu" | sudo tee /etc/ld.so.conf.d/radtest.conf >/dev/null
sudo ldconfig

{
  echo "SRC=$RT/src"
  echo "SUPPORT=$RT/support"
  echo "PREFIX=$PREFIX"
  echo "LDCONFIG=sudo ldconfig"
  echo "SC_DATA=$DATA"
  echo "SC_CONFIG=$CONF"
  echo "GUI_CLASSES=$RT/sc-gui-classes"
  echo "RADICLES=$GITHUB_WORKSPACE"
  echo "PKG_CONFIG_PATH=$PREFIX/lib/pkgconfig:$PREFIX/lib/x86_64-linux-gnu/pkgconfig:$PREFIX/share/pkgconfig"
  echo "CMAKE_PREFIX_PATH=$PREFIX"
} >> "$GITHUB_ENV"
echo "$PREFIX/bin" >> "$GITHUB_PATH"
