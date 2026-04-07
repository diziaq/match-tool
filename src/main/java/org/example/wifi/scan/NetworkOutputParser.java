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
 *       signal appended with {@code " dBm"}</li>
 *   <li><b>Linux</b> ({@code nmcli -t -f SSID,SIGNAL dev wifi list}): colon-delimited
 *       {@code SSID:signal}, signal appended with {@code "%"}</li>
 * </ul>
 *
 * <p>Blank SSIDs and malformed lines are silently skipped. After parsing, call
 * {@link #deduplicated(List)} to collapse duplicate SSIDs and sort the result.
 */
public class NetworkOutputParser {

    /**
     * Parses pipe-delimited macOS scan output into a (possibly unsorted, possibly duplicate)
     * list of networks.
     */
    public static List<Network> parseMacOs(String output) {
        List<Network> networks = new ArrayList<>();
        output.lines()
              .filter(l -> !l.isBlank() && l.contains("|"))
              .forEach(line -> {
                  String[] parts = line.split("\\|", 2);
                  if (parts.length == 2 && !parts[0].isBlank()) {
                      networks.add(new Network(parts[0], parts[1] + " dBm"));
                  }
              });
        return networks;
    }

    /**
     * Parses colon-delimited Linux ({@code nmcli}) scan output into a (possibly unsorted,
     * possibly duplicate) list of networks.
     *
     * <p><b>Note:</b> SSIDs that themselves contain a {@code :} character will be truncated at the
     * first colon, as the parser splits on the first colon delimiter.
     */
    public static List<Network> parseLinux(String output) {
        List<Network> networks = new ArrayList<>();
        output.lines()
              .filter(l -> !l.isBlank())
              .forEach(line -> {
                  String[] parts = line.split(":");
                  if (parts.length >= 2 && !parts[0].isBlank()) {
                      networks.add(new Network(parts[0], parts[1] + "%"));
                  }
              });
        return networks;
    }

    /**
     * Deduplicates {@code networks} by SSID (case-insensitive), keeping the entry with the
     * highest numeric signal strength, then returns them sorted case-insensitively by SSID.
     * Entries with unparseable signal values are treated as having the weakest possible signal.
     */
    public static List<Network> deduplicated(List<Network> networks) {
        Map<String, Network> unique = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Network n : networks) {
            unique.merge(n.ssid(), n, (old, next) -> signalStrength(next) > signalStrength(old) ? next : old);
        }
        return new ArrayList<>(unique.values());
    }

    private static int signalStrength(Network n) {
        try {
            return Integer.parseInt(n.signal().replace(" dBm", "").replace("%", "").trim());
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }
}
