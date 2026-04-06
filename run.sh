#!/usr/bin/env bash
set -e

mvn clean package -D skipTests

java -jar target/wifi-tool.jar \
     --debug true \
     --mode 3 \
     --right "/Users/sn/FT/project/public/wifi-tool/src/main/resources/passwords_num.txt" \
     --left "/Users/sn/FT/project/public/wifi-tool/src/main/resources/networks.txt"
