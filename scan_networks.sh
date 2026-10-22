#!/usr/bin/env bash
# scan_networks.sh — list the visible Wi-Fi networks to a plain TSV for review.
#
# Scans with the standard macOS CoreWLAN framework (through the `swift` tool, the same interface the
# tool itself uses to scan). Writes a tab-separated file with one row per visible SSID and its
# visible characteristics, strongest signal first, so you can open it, delete the rows that are out
# of scope, and keep only the networks you own or are authorised to test. Then turn the kept rows
# into a plain SSID list for the WiFi plugin:
#
#     cut -f1 <file> | tail -n +2 > input/networks.txt
#
# Columns: SSID, Signal(dBm), Channel, Band, Security.
# Duplicate SSIDs (same network on several bands/APs) are collapsed to the strongest signal.
#
# Usage: ./scan_networks.sh [output.tsv]        (default: input/networks_scan.tsv)
#
# Note: scanning needs the `swift` tool (Xcode command line tools). macOS also requires Location
# Services permission for your terminal to reveal SSIDs; without it SSIDs come back blank.
set -euo pipefail

if [[ "$(uname)" != "Darwin" ]]; then
  echo "This script uses macOS CoreWLAN; run it on macOS." >&2
  exit 1
fi
if ! command -v swift >/dev/null 2>&1; then
  echo "The 'swift' tool is not available. Install the Xcode command line tools (xcode-select --install)." >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="${1:-$SCRIPT_DIR/input/networks_scan.tsv}"
mkdir -p "$(dirname "$OUT")"

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

cat > "$WORK/scan.swift" <<'SWIFT'
import CoreWLAN
guard let iface = CWWiFiClient.shared().interface() else { exit(1) }
func security(_ n: CWNetwork) -> String {
    if n.supportsSecurity(.wpa3Personal) { return "WPA3" }
    if n.supportsSecurity(.wpa2Personal) { return "WPA2" }
    if n.supportsSecurity(.wpaPersonal)  { return "WPA" }
    if n.supportsSecurity(.none)         { return "Open" }
    return "?"
}
let nets = (try? iface.scanForNetworks(withSSID: nil)) ?? []
for n in nets {
    let ssid = n.ssid ?? ""
    let channel = n.wlanChannel?.channelNumber ?? 0
    let band = n.wlanChannel?.channelBand.rawValue ?? 0   // 1=2.4GHz, 2=5GHz, 3=6GHz
    print("\(ssid)\t\(n.rssiValue)\t\(channel)\t\(band)\t\(security(n))")
}
SWIFT

# Columns from swift: ssid, rssi, channel, band(raw), security.
# Drop blank SSIDs, map the band code to text, sort by signal (desc), keep the strongest per SSID.
rows="$(swift "$WORK/scan.swift" 2>/dev/null | awk -F '\t' '
  $1 != "" {
    band = ($4 == 1) ? "2.4GHz" : ($4 == 2) ? "5GHz" : ($4 == 3) ? "6GHz" : "?"
    printf "%s\t%s\t%s\t%s\t%s\n", $1, $2, $3, band, $5
  }' | sort -t "$(printf '\t')" -k2,2 -nr | awk -F '\t' '!seen[$1]++')"

{
  printf 'SSID\tSignal(dBm)\tChannel\tBand\tSecurity\n'
  [[ -n "$rows" ]] && printf '%s\n' "$rows"
} > "$OUT"

count=0
[[ -n "$rows" ]] && count="$(printf '%s\n' "$rows" | grep -c .)"
echo "Wrote $count network(s) to $OUT"
echo "Review it, delete rows that are out of scope, then build the plugin's SSID list with:"
echo "  cut -f1 \"$OUT\" | tail -n +2 > \"$SCRIPT_DIR/input/networks.txt\""

if [[ "$count" -eq 0 ]]; then
  echo >&2
  echo "No networks found. If SSIDs come back blank, grant Location Services to your terminal app" >&2
  echo "(System Settings > Privacy & Security > Location Services) — macOS requires it to reveal SSIDs." >&2
fi
