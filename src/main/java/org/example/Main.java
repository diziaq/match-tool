package org.example;

import org.example.io.CsvReader;
import org.example.io.Logger;
import org.example.matcher.PairMatcher;
import org.example.shell.ShellRunner;
import org.example.shell.SystemShellRunner;
import org.example.wifi.Network;
import org.example.wifi.connect.ShellNetworkConnector;
import org.example.wifi.scan.NetworkScanner;
import org.example.wifi.scan.NetworkScannerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

public class Main {

    private static final boolean DEBUG = true;
    private static final DateTimeFormatter LOG_FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

    public static void main(String[] args) throws Exception {
        Logger logger = new Logger(Logger.Output.CONSOLE, DEBUG);

        boolean isMac = System.getProperty("os.name", "").toLowerCase().contains("mac");
        logger.debug("OS: " + System.getProperty("os.name") + " (isMac=" + isMac + ")");

        ShellRunner shell = new SystemShellRunner(logger);
        NetworkScanner scanner = NetworkScannerFactory.forCurrentOs(shell);
        ShellNetworkConnector connector = new ShellNetworkConnector(shell, isMac, logger);

        Scanner input = new Scanner(System.in);
        logger.info("=== WiFi Manager (" + (isMac ? "macOS" : "Linux") + ") ===");
        logger.info("1. List available WiFi networks");
        logger.info("2. Connect to a WiFi network");
        logger.info("3. Try passwords for a WiFi network");
        logger.info("4. Try passwords for all WiFi networks");
        logger.print("Choice: ");
        int choice = input.nextInt();
        input.nextLine();
        logger.debug("User chose: " + choice);

        switch (choice) {
            case 1 -> {
                logger.info("Scanning...");
                List<Network> networks = scanner.scan();
                logger.info(String.format("  %-30s %s", "SSID", "SIGNAL"));
                logger.info("  " + "-".repeat(40));
                networks.forEach(n -> logger.info(String.format("  %-30s %s", n.ssid(), n.signal())));
            }
            case 2 -> {
                logger.print("SSID: ");
                String ssid = input.nextLine();
                logger.print("Password: ");
                String password = input.nextLine();
                logger.info(connector.connect(ssid, password));
            }
            case 3 -> {
                logger.print("SSID: ");
                String ssid = input.nextLine();
                logger.info("Enter passwords to try (enter '0' to stop):");
                while (true) {
                    logger.print("Password: ");
                    String password = input.nextLine();
                    if ("0".equals(password)) { logger.info("Stopped."); break; }
                    boolean ok = connector.tryConnect(ssid, password);
                    logger.info(ok ? "SUCCESS - connected to " + ssid : "ERROR - wrong password or connection failed");
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

                int skipPasswords = 0;
                logger.debug("Skipping first " + skipPasswords + " passwords");
                logger.debug("Trying networks: " + targetNetworks);

                String prefix = LocalDateTime.now().format(LOG_FILE_TIMESTAMP);
                try (var traceLog   = new Logger(Logger.Output.FILE, false, Path.of("logs", prefix + "_trace.log"));
                     var successLog = new Logger(Logger.Output.FILE, false, Path.of("logs", prefix + "_success.log"))) {

                    new PairMatcher<String, String>().match(
                        targetNetworks,
                        CsvReader.column("/passwords_num.csv", 0, s -> s.length() > 7).skip(skipPasswords),
                        (ssid, password) -> {
                            boolean result = connector.tryConnect(ssid, password);
                            traceLog.info("%s: %s @ %s".formatted(result, password, ssid));
                            return result;
                        },
                        match -> {
                            successLog.info("TRUE: %s @ %s".formatted(match.right(), match.left()));
                            logger.info("Matched " + match);
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
