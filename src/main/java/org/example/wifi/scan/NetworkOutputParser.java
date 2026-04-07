package org.example.wifi.scan;

import org.example.wifi.Network;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Stateless parser for the raw text produced by OS WiFi scan commands.
 *
 * <p>The two parse methods handle the different line formats:
 * <ul>
 *   <li><b>macOS</b> ({@code swift wifi_scan.swift}): pipe-delimited {@code SSID|signal},
 *       where {@code signal} is a negative dBm integer (e.g. {@code -45})</li>
 *   <li><b>Linux</b> ({@code nmcli -t -f SSID,SIGNAL dev wifi list}): colon-delimited
 *       {@code SSID:signal}, where {@code signal} is a 0-100 percent integer</li>
 * </ul>
 *
 * <p>Blank SSIDs, malformed lines, and lines whose signal field is not a parseable integer are
 * silently skipped. Signal classification into {@link org.example.wifi.Strength} variants is
 * handled by {@link Network#of(String, int)}. After parsing, call {@link #deduplicated(List)} to
 * collapse duplicate SSIDs and sort the result.
 */
public class NetworkOutputParser {

    /**
     * Parses pipe-delimited macOS scan output into a (possibly unsorted, possibly duplicate)
     * list of networks. Each valid line must have the form {@code SSID|signal} where signal is
     * a parseable integer dBm value.
     */
    public static List<Network> parseMacOs(String output) {
        List<Network> networks = new ArrayList<>();
        output.lines()
              .filter(l -> !l.isBlank() && l.contains("|"))
              .forEach(line -> {
                  String[] parts = line.split("\\|", 2);
                  if (parts.length == 2 && !parts[0].isBlank()) {
                      try {
                          networks.add(Network.of(parts[0], Integer.parseInt(parts[1].trim())));
                      } catch (NumberFormatException ignored) { /* skip malformed signal */ }
                  }
              });
        return networks;
    }

    /**
     * Parses colon-delimited Linux ({@code nmcli}) scan output into a (possibly unsorted,
     * possibly duplicate) list of networks. Each valid line must have the form {@code SSID:signal}
     * where signal is a parseable integer percent value.
     *
     * <p><b>Note:</b> SSIDs that themselves contain a {@code :} character will be truncated at
     * the first colon — and if the second field is not a parseable integer, the line is skipped.
     */
    public static List<Network> parseLinux(String output) {
        List<Network> networks = new ArrayList<>();
        output.lines()
              .filter(l -> !l.isBlank())
              .forEach(line -> {
                  String[] parts = line.split(":");
                  if (parts.length >= 2 && !parts[0].isBlank()) {
                      try {
                          networks.add(Network.of(parts[0], Integer.parseInt(parts[1].trim())));
                      } catch (NumberFormatException ignored) { /* skip malformed signal */ }
                  }
              });
        return networks;
    }

    /**
     * Deduplicates {@code networks} by SSID (case-insensitive), keeping the entry with the
     * highest signal strength value, then returns them sorted case-insensitively by SSID.
     */
    public static List<Network> deduplicated(List<Network> networks) {
        Map<String, Network> unique = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Network n : networks) {
            unique.merge(n.ssid(), n, (old, next) -> signalValue(next) > signalValue(old) ? next : old);
        }
        return new ArrayList<>(unique.values());
    }

    private static int signalValue(Network n) {
        return n.strength().value();
    }
}
