#!/usr/bin/env bash
# wifi-tool — reference example (v2).
#
# Runs the tool in two steps:
#   1. scan the visible networks (mode 1)
#   2. batch match a password list against a network list (mode 2)
#
# Network and password lists default to the dummy samples in input/, so the script runs out of
# the box without touching any real network. Pass your own lists as the first two arguments:
#
#   ./run_v2.sh /abs/path/networks.txt /abs/path/passwords.txt
#
# AUTHORISATION: only run the batch match against networks you own or are explicitly authorised
# to test (your own access point, a home lab, a CTF range). See input/README.md.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$SCRIPT_DIR/target/wifi-tool.jar"
INPUT_DIR="$SCRIPT_DIR/input"
NETWORKS="${1:-$INPUT_DIR/networks.sample.txt}"
PASSWORDS="${2:-$INPUT_DIR/passwords.sample.txt}"

cd "$SCRIPT_DIR"

java --version
mvn clean -s settings.xml package -DskipTests

echo
echo "== Step 1: scan visible networks (mode 1) =="
java -jar "$JAR" --mode 1 --debug false

echo
echo "== Step 2: batch match (mode 2) =="
echo "   networks : $NETWORKS"
echo "   passwords: $PASSWORDS"
echo "   (only networks you own or are authorised to test)"
java -jar "$JAR" \
     --mode 2 \
     --debug true \
     --left  "$NETWORKS" \
     --right "$PASSWORDS"

echo
echo "Matches, if any, are also written to timestamped *_success.log files under the logs/"
echo "directory next to the jar (target/logs/)."
