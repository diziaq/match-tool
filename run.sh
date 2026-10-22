#!/usr/bin/env bash
# wifi-tool — build the jar and run a batch match (mode 2) against the sample lists.
#
# This is the quick "does it run" script. It uses the dummy lists in input/, so it is safe to
# run anywhere: the sample SSIDs do not exist, so every attempt comes back unavailable or a
# mismatch and nothing is connected. Point it at your own lists (see input/README.md) to do real
# work, and only against networks you own or are authorised to test.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

java --version
mvn clean -s settings.xml package -DskipTests

java -jar target/wifi-tool.jar \
     --plugin wifi \
     --mode 2 \
     --debug true \
     --networks  "$SCRIPT_DIR/input/networks.sample.txt" \
     --passwords "$SCRIPT_DIR/input/passwords.sample.txt"
