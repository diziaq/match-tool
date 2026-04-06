package org.example;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import org.example.cli.ArgsParser;
import org.example.cli.ParsedArgs;
import org.example.io.Logger;
import org.example.matcher.PairMatcher;
import org.example.shell.ShellRunner;
import org.example.shell.SystemShellRunner;
import org.example.wifi.Network;
import org.example.wifi.connect.ConnectOutcome;
import org.example.wifi.connect.NetworkConnector;
import org.example.wifi.scan.NetworkScanner;

public class Main {

    private static final DateTimeFormatter LOG_FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

    public static void main(String[] args) throws Exception {
        ParsedArgs cli;
        try {
            cli = new ArgsParser()
                      .registerRequired("mode", Integer.class, raw -> {
                          int m = Integer.parseInt(raw);
                          if (m < 1 || m > 4) throw new IllegalArgumentException("must be 1..3");
                          return m;
                      })
                      .registerOptional("left", Path.class, ArgsParser.PATH, null)
                      .registerOptional("right", Path.class, ArgsParser.PATH, null)
                      .registerOptional("skip", Integer.class, ArgsParser.INTEGER, 0)
                      .registerOptional("debug", Boolean.class, Boolean::parseBoolean, false)
                      .parse(args);
        } catch (ArgsParser.ParseException e) {
            System.err.println("Usage error:\n" + e.getMessage());
            System.exit(1);
            return;
        }

        int mode = cli.get("mode");
        Path left = cli.get("left");
        Path right = cli.get("right");
        int skip = cli.get("skip");
        boolean debug = cli.get("debug");

        var logger = new Logger(Logger.Output.CONSOLE, debug);

        var platform = Platform.detect();
        logger.debug("Platform: " + platform);

        ShellRunner shell = new SystemShellRunner(logger);
        NetworkScanner scanner = platform.newScanner(shell);
        NetworkConnector connector = platform.newConnector(shell, logger);

        switch (mode) {
            case 1 -> {
                logger.info("Scanning...");
                List<Network> networks = scanner.scan();
                logger.info(String.format("  %-30s %s", "SSID", "SIGNAL"));
                logger.info("  " + "-".repeat(40));
                networks.forEach(n -> logger.info(String.format("  %-30s %s", n.ssid(), n.signal())));
            }
            case 2 -> {
                var input = new Scanner(System.in);
                logger.print("SSID: ");
                String ssid = input.nextLine();
                logger.print("Password: ");
                String password = input.nextLine();
                String message = switch (connector.tryConnect(ssid, password)) {
                    case ConnectOutcome.Connected()         -> "Connected to: " + ssid;
                    case ConnectOutcome.NetworkNotFound()   -> "Network not found: " + ssid;
                    case ConnectOutcome.WrongPassword()     -> "Wrong password for: " + ssid;
                    case ConnectOutcome.AssociationFailed() -> "Association failed for: " + ssid;
                    case ConnectOutcome.UnknownFailure(var out) -> out;
                };
                logger.info(message);
            }
            case 3 -> {
                requireFile(left, "left", logger);
                requireFile(right, "right", logger);
                List<String> lefts = Files.readAllLines(left);
                String prefix = LocalDateTime.now().format(LOG_FILE_TIMESTAMP);
                try (var traceLog = new Logger(Logger.Output.FILE, false, "logs/" + prefix + "_trace.log");
                     var successLog = new Logger(Logger.Output.FILE, false, "logs/" + prefix + "_success.log")) {
                    new PairMatcher<String, String>().match(
                        lefts,
                        Files.lines(right).skip(skip),
                        (ssid, password) -> {
                            ConnectOutcome outcome = connector.tryConnect(ssid, password);
                            traceLog.info("%s: %s @ %s".formatted(outcome.getClass().getSimpleName(), password, ssid));
                            return outcome instanceof ConnectOutcome.Connected;
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

    private static void requireFile(Path path, String argName, Logger logger) {
        if (path == null) {
            logger.error("--" + argName + " is required for this mode");
            System.exit(1);
        }
    }
}
