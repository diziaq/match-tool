package org.example.wifi.scan;

import org.example.wifi.Network;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class NetworkOutputParser {

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
