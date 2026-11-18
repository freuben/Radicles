#!/usr/bin/env bash
# Run a share of the test cases one after another and keep every case's
# results. Used by the GitHub workflow (.github/workflows/tests.yml), which
# splits the suite across several machines ("shards").
#   Tests/ci/run-shard.sh cases <shard> <shards>        # list this shard's cases
#   Tests/ci/run-shard.sh run <shard> <shards> [case] [test]
#   Tests/ci/run-shard.sh summary                       # Markdown summary so far
# Cases are the files in Tests/cases/, shared out by their number of tests so
# the shards take about as long; a new case file is picked up automatically.
# With [case], shard 1 runs only that case (and [test]); other shards run nothing.
# run-headless.sh empties Tests/results at the start of every case, so after
# each case its results are copied to $RADTEST_OUT (default Tests/results-all),
# with that case's output in <case>.out. Exit code 0 = no case failed.
set -uo pipefail
CI=$(cd "$(dirname "$0")" && pwd)
TESTS=$(cd "$CI/.." && pwd)
OUT=${RADTEST_OUT:-$TESTS/results-all}

cases_for() { # shard shards: greedy split, biggest case first, to the lightest shard
  for f in "$TESTS"/cases/*.scd; do
    echo "$(grep -c '~radTest\.(' "$f") $(basename "$f" .scd)"
  done | sort -k1,1nr -k2,2 | awk -v shard="$1" -v n="$2" '
    { best = 1; for (i = 2; i <= n; i++) if (load[i] < load[best]) best = i;
      load[best] += ($1 > 0 ? $1 : 1); if (best == shard) print $2 }'
}

summary() {
  local f n st total
  echo "### Radicles tests${RADTEST_TITLE:+, $RADTEST_TITLE}"
  echo
  if ! ls "$OUT"/*.out >/dev/null 2>&1; then
    echo "No case ran (or none finished): see the job log."; return
  fi
  echo "| result | tests |"; echo "|---|---|"
  for st in pass updated known skipped fail error; do
    total=0
    for f in "$OUT"/*.out; do
      n=$(sed -n "/^RADTEST SUMMARY/,\$s/^  $st: \([0-9][0-9]*\)\$/\1/p" "$f" | head -1)
      total=$((total + ${n:-0}))
    done
    [ "$total" -gt 0 ] && echo "| $st | $total |"
  done
  echo
  echo "Cases: $(for f in "$OUT"/*.out; do basename "$f" .out; done | tr '\n' ' ')"
  if [ -s "$OUT/failed-cases.txt" ]; then
    echo; echo "**Failed:**"; echo; echo '```'
    for f in "$OUT"/*.out; do
      sed -n -E '/^RADTEST SUMMARY/,$ { /^  FAILED /p; }; /^RADTEST \[[0-9]+\/[0-9]+\] ERROR /p' "$f"
    done
    sed 's/^/  case did not finish: /' "$OUT/no-summary.txt" 2>/dev/null
    echo '```'
    echo; echo "Logs, and .actual.txt files next to changed snapshots, are in this run's results artifact."
  fi
}

case "${1:-}" in
  cases) cases_for "$2" "$3" ;;
  summary) summary ;;
  run)
    shard=$2; shards=$3; only_case=${4:-}; only_test=${5:-}
    if [ -n "$only_case" ]; then
      [ "$shard" = 1 ] && cases=("$only_case") || cases=()
    else
      mapfile -t cases < <(cases_for "$shard" "$shards")
    fi
    rm -rf "$OUT"; mkdir -p "$OUT"; : > "$OUT/failed-cases.txt"
    echo "RADTEST SHARD $shard/$shards: ${cases[*]:-(nothing to run)}"
    for c in "${cases[@]}"; do
      if [ ! -f "$TESTS/cases/$c.scd" ]; then
        echo "RADTEST no such case: Tests/cases/$c.scd"; echo "$c" >> "$OUT/failed-cases.txt"; continue
      fi
      echo; echo "=== case $c"
      "$CI/run-headless.sh" check "$c" ${only_test:+"$only_test"} 2>&1 | tee "$OUT/$c.out"
      status=${PIPESTATUS[0]}
      [ -d "$TESTS/results" ] && cp -R "$TESTS/results/." "$OUT/"
      if ! grep -q '^RADTEST SUMMARY' "$OUT/$c.out"; then
        echo "$c" >> "$OUT/no-summary.txt"; status=1
      fi
      [ "$status" -ne 0 ] && echo "$c" >> "$OUT/failed-cases.txt"
    done
    echo
    if [ -s "$OUT/failed-cases.txt" ]; then
      echo "RADTEST SHARD FAILED cases: $(tr '\n' ' ' < "$OUT/failed-cases.txt")"; exit 1
    fi
    echo "RADTEST SHARD passed"
    ;;
  *) sed -n '2,13p' "$0"; exit 2 ;;
esac
