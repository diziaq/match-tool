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
import org.example.matcher.MatchOutcome;
import org.example.matcher.MatchPredicate;
import org.example.matcher.PairMatcher;
import org.example.shell.ShellRunner;
import org.example.shell.SystemShellRunner;
import org.example.wifi.Network;
import org.example.wifi.Password;
import org.example.wifi.connect.NetworkConnector;
import org.example.wifi.scan.NetworkScanner;

/**
 * Application entry point. Dispatches to one of four operational modes based on {@code --mode}:
 *
 * <ul>
 *   <li><b>1 – scan:</b> Prints all visible WiFi networks with signal strength.</li>
 *   <li><b>2 – connect:</b> Interactive prompt for SSID and password; prints the typed outcome.</li>
 *   <li><b>3 – batch match:</b> Cross-product password-spray — reads SSIDs from {@code --left} and
 *       passwords from {@code --right}, tries every combination, logs matches to timestamped files
 *       under {@code logs/}. Use {@code --skip N} to resume a password list from offset N.</li>
 * </ul>
 *
 * <p>Add {@code --debug} to enable verbose shell logging via {@link org.example.io.Logger}.
 */
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
                networks.forEach(n -> logger.info(String.format("  %-30s %s", n.ssid(), n.strength())));
            }
            case 2 -> {
                var input = new Scanner(System.in);
                logger.print("SSID: ");
                String ssid = input.nextLine();
                logger.print("Password: ");
                String password = input.nextLine();
                String message = switch (connector.test(Network.of(ssid), new Password(password))) {
                    case MatchOutcome.Match()        -> "Connected to: " + ssid;
                    case MatchOutcome.Unavailable()  -> "Network not found: " + ssid;
                    case MatchOutcome.Mismatch()     -> "Wrong password for: " + ssid;
                    case MatchOutcome.Failure(var r) -> r;
                };
                logger.info(message);
            }
            case 3 -> {
                requireFile(left, "left", logger);
                requireFile(right, "right", logger);
                List<Network> lefts = Files.readAllLines(left).stream()
                    .map(Network::of).toList();
                String prefix = LocalDateTime.now().format(LOG_FILE_TIMESTAMP);
                try (var traceLog = new Logger(Logger.Output.FILE, false, "logs/" + prefix + "_trace.log");
                     var successLog = new Logger(Logger.Output.FILE, false, "logs/" + prefix + "_success.log")) {
                    MatchPredicate<Network, Password> tracingPredicate = (network, password) -> {
                        MatchOutcome outcome = connector.test(network, password);
                        traceLog.info("%s: %s @ %s".formatted(outcome.getClass().getSimpleName(), password.value(), network.ssid()));
                        return outcome;
                    };
                    new PairMatcher<Network, Password>().match(
                        lefts,
                        Files.lines(right).skip(skip).map(Password::new),
                        tracingPredicate,
                        match -> {
                            successLog.info("TRUE: %s @ %s".formatted(match.right().value(), match.left().ssid()));
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
