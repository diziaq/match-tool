#!/usr/bin/env bash
set -e

java --version
mvn clean  -s settings.xml package -D skipTests

java -jar target/wifi-tool.jar --enable-final-field-mutation=ALL-UNNAMED \
     --debug true \
     --mode 3 \
     --right "/Users/sn/FT/project/public/wifi-tool/src/main/resources/passwords_num.txt" \
     --left "/Users/sn/FT/project/public/wifi-tool/src/main/resources/networks.txt"
