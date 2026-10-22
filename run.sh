#!/usr/bin/env bash
# wifi-tool — build the jar and run a batch match (mode 2) against the sample lists.
#
# The command line takes only the plugin name and a config file; everything else lives in the
# config. This script generates a throwaway config pointing at the dummy sample lists in input/, so
# it is safe to run anywhere: the sample SSIDs do not exist, so nothing is connected. Point it at
# your own lists (see input/README.md) to do real work, and only against networks you own or are
# authorised to test.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

java --version
mvn clean -s settings.xml package -DskipTests

CONFIG="/Users/sn/FT/project/public/wifi-tool/input/wifi.properties"

java -jar target/wifi-tool.jar --plugin wifi --config "$CONFIG"
