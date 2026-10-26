#!/usr/bin/env bash
# Run the Radicles tests headless (cloud workspace).
#   Tests/ci/run-headless.sh [check|update] [case] [test]
# Starts JACK with its dummy driver if needed. Prints a diff for each
# snapshot that changed. Exit code 0 = all passed.
set -uo pipefail
TESTS=$(cd "$(dirname "$0")/.." && pwd)
SCLANG=${SCLANG:-sclang}
if ! pgrep -x jackd >/dev/null; then
  setsid jackd -r -d dummy -r 48000 -p 512 -C 2 -P 2 >/tmp/radtest-jackd.log 2>&1 < /dev/null &
  sleep 2
fi
rm -rf "$TESTS/results"
cd /tmp
"$SCLANG" ${RADTEST_CONF:+-l "$RADTEST_CONF"} "$TESTS/run.scd" "${1:-check}" ${2:-} ${3:-} 2>&1 | grep --line-buffered -E "^RADTEST|^  "
status=${PIPESTATUS[0]}
for actual in $(find "$TESTS/results" -name '*.actual.txt' 2>/dev/null); do
  rel=${actual#$TESTS/results/}; snap="$TESTS/snapshots/${rel%.actual.txt}.txt"
  echo; echo "--- diff for ${rel%.actual.txt} (snapshot vs this run)"
  diff -u "$snap" "$actual" 2>/dev/null || [ -f "$snap" ] || echo "(no approved snapshot yet)"
done
exit $status
