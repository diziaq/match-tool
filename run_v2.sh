#!/usr/bin/env bash
# wifi-tool — reference example (v2).
#
# Runs the tool in two steps, each driven by a small config file (the command line only ever
# carries --plugin and --config):
#   1. scan the visible networks (mode 1)
#   2. batch match a password list against a network list (mode 2)
#
# Step 2 uses the config file passed as the first argument, or a generated one pointing at the
# dummy samples in input/ so the script runs out of the box.
#
#   ./run_v2.sh [/abs/path/to/your.properties]
#
# AUTHORISATION: only run the batch match against networks you own or are explicitly authorised to
# test (your own access point, a home lab, a CTF range). See input/README.md.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$SCRIPT_DIR/target/wifi-tool.jar"
cd "$SCRIPT_DIR"

java --version
mvn clean -s settings.xml package -DskipTests

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

printf 'mode=1\ndebug=false\n' > "$TMP/scan.properties"
echo
echo "== Step 1: scan visible networks (mode 1) =="
java -jar "$JAR" --plugin wifi --config "$TMP/scan.properties"

if [[ -n "${1:-}" ]]; then
  BATCH="$1"
else
  BATCH="$TMP/batch.properties"
  cat > "$BATCH" <<EOF
mode=2
networks=$SCRIPT_DIR/input/networks.sample.txt
passwords=$SCRIPT_DIR/input/passwords.sample.txt
skip=0
debug=true
EOF
fi

echo
echo "== Step 2: batch match (mode 2) =="
echo "   config: $BATCH"
echo "   (only networks you own or are authorised to test)"
java -jar "$JAR" --plugin wifi --config "$BATCH"

echo
echo "Matches, if any, are also written to timestamped *_success.log files under the logs/"
echo "directory next to the jar (target/logs/)."
