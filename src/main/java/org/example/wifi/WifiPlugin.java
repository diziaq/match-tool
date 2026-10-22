package org.example.wifi;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.example.Platform;
import org.example.cli.ArgSchema;
import org.example.cli.ParsedArgs;
import org.example.engine.Outcome;
import org.example.engine.PairEvaluator;
import org.example.engine.PairingEngine;
import org.example.engine.Source;
import org.example.io.Logger;
import org.example.plugin.Plugin;
import org.example.shell.ShellRunner;
import org.example.shell.SystemShellRunner;
import org.example.wifi.connect.NetworkConnector;
import org.example.wifi.scan.NetworkScanner;

/**
 * WiFi plugin: scans visible networks, or batch-matches a password list against a network list.
 *
 * <p>It wires the generic {@link PairingEngine} to WiFi: networks and passwords come from
 * {@link LinesFileSource}, and a {@link NetworkConnector} is the pair evaluator. Networks are the
 * outer source and passwords the inner, so the policies read as "try passwords for a network until
 * one connects, then move on". The parallelism budget is {@code 1} because a laptop has one radio.
 *
 * <p>Modes ({@code --mode}):
 * <ul>
 *   <li><b>1 – scan:</b> print all visible networks with signal strength.</li>
 *   <li><b>2 – batch match:</b> read SSIDs from {@code --networks} and passwords from
 *       {@code --passwords}, try each password against each network, log matches to timestamped
 *       files under {@code logs/}. Use {@code --skip N} to resume a password list from offset N.</li>
 * </ul>
 */
public final class WifiPlugin implements Plugin {

    private static final DateTimeFormatter LOG_FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

    @Override
    public String name() {
        return "wifi";
    }

    @Override
    public int parallelismBudget() {
        return 1; // a laptop has one WiFi radio; only one connection attempt can run at a time
    }

    @Override
    public ArgSchema schema() {
        return ArgSchema.builder()
            .required("mode", Integer.class, raw -> {
                int m = Integer.parseInt(raw);
                if (m < 1 || m > 2) throw new IllegalArgumentException("must be 1..2");
                return m;
            })
            .optional("networks", Path.class, ArgSchema.PATH, null)
            .optional("passwords", Path.class, ArgSchema.PATH, null)
            .optional("skip", Integer.class, ArgSchema.INTEGER, 0)
            .optional("debug", Boolean.class, Boolean::parseBoolean, false)
            .build();
    }

    @Override
    public void run(ParsedArgs args) throws Exception {
        boolean debug = args.get("debug");
        var logger = new Logger(Logger.Output.CONSOLE, debug);
        var platform = Platform.detect();
        logger.debug("Platform: " + platform);
        ShellRunner shell = new SystemShellRunner(logger);

        int mode = args.get("mode");
        switch (mode) {
            case 1 -> scan(platform, shell, logger);
            case 2 -> batchMatch(args, platform, shell, logger);
            default -> throw new IllegalArgumentException("unsupported mode: " + mode);
        }
    }

    private void scan(Platform platform, ShellRunner shell, Logger logger) throws Exception {
        NetworkScanner scanner = platform.newScanner(shell);
        logger.info("Scanning...");
        List<Network> networks = scanner.scan();
        logger.info(String.format("  %-30s %s", "SSID", "SIGNAL"));
        logger.info("  " + "-".repeat(40));
        networks.forEach(n -> logger.info(String.format("  %-30s %s", n.ssid(), n.strength())));
    }

    private void batchMatch(ParsedArgs args, Platform platform, ShellRunner shell, Logger logger) throws Exception {
        Path networksPath = requireFile(args.get("networks"), "networks");
        Path passwordsPath = requireFile(args.get("passwords"), "passwords");
        int skip = args.get("skip");

        NetworkConnector connector = platform.newConnector(shell, logger);
        Source<Network> networks = new LinesFileSource(networksPath).map(Network::of);
        Source<String> passwordLines = new LinesFileSource(passwordsPath);
        Source<Password> passwords = () -> passwordLines.stream().skip(skip).map(Password::new);

        PairingEngine<Network, Password> engine = PairingEngine.<Network, Password>builder()
            .budget(parallelismBudget())
            .stopInnerOnMatch()
            .breakInnerOnSkip()
            .breakInnerAfterFailures(3)
            .retriesOnFailure(2)
            .build();

        String prefix = LocalDateTime.now().format(LOG_FILE_TIMESTAMP);
        try (var traceLog = new Logger(Logger.Output.FILE, false, "logs/" + prefix + "_trace.log");
             var successLog = new Logger(Logger.Output.FILE, false, "logs/" + prefix + "_success.log")) {
            PairEvaluator<Network, Password> tracing = (network, password) -> {
                Outcome outcome = connector.evaluate(network, password);
                traceLog.info("%s: %s @ %s".formatted(outcome.getClass().getSimpleName(), password.value(), network.ssid()));
                return outcome;
            };
            engine.run(networks, passwords, tracing, pair -> {
                successLog.info("TRUE: %s @ %s".formatted(pair.b().value(), pair.a().ssid()));
                logger.info("Matched " + pair);
            });
        }
    }

    private static Path requireFile(Path path, String argName) {
        if (path == null) {
            throw new IllegalArgumentException("--" + argName + " is required for this mode");
        }
        return path;
    }
}
