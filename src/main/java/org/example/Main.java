package org.example;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.example.cli.ArgSchema;
import org.example.cli.CliArgSource;
import org.example.cli.ParsedArgs;
import org.example.io.Logger;
import org.example.matcher.MatchCoordinator;
import org.example.matcher.MatchOutcome;
import org.example.matcher.MatchPredicate;
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
 *   <li><b>2 – batch match:</b> Cross-product password-spray — reads SSIDs from {@code --left} and
 *       passwords from {@code --right}, tries every combination, logs matches to timestamped files
 *       under {@code logs/}. Use {@code --skip N} to resume a password list from offset N.</li>
 * </ul>
 *
 * <p>Add {@code --debug} to enable verbose shell logging via {@link org.example.io.Logger}.
 */
public class Main {

    private static final DateTimeFormatter LOG_FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

    public static void main(String[] args) throws Exception {
        ArgSchema schema = ArgSchema.builder()
            .required("mode", Integer.class, raw -> {
                int m = Integer.parseInt(raw);
                if (m < 1 || m > 2) throw new IllegalArgumentException("must be 1..2");
                return m;
            })
            .optional("left", Path.class, ArgSchema.PATH, null)
            .optional("right", Path.class, ArgSchema.PATH, null)
            .optional("skip", Integer.class, ArgSchema.INTEGER, 0)
            .optional("debug", Boolean.class, Boolean::parseBoolean, false)
            .build();

        ParsedArgs cli;
        try {
            cli = new ParsedArgs(schema, new CliArgSource(args));
        } catch (IllegalArgumentException e) {
            System.err.println("Usage error:\n" + e.getMessage());
            System.exit(1);
            return;
        }

        int mode;
        try {
            mode = cli.get("mode");
        } catch (IllegalArgumentException e) {
            System.err.println("Usage error:\n" + e.getMessage());
            System.exit(1);
            return;
        }

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
                    MatchCoordinator.<Network, Password>effective().match(
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
