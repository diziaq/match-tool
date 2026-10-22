package org.example.wifi.connect;

import java.time.Duration;
import org.example.Platform;
import org.example.engine.Outcome;
import org.example.io.Logger;
import org.example.shell.ShellRunner;
import org.example.wifi.Network;
import org.example.wifi.Password;

/**
 * {@link NetworkConnector} that delegates to a {@link ShellRunner}.
 *
 * <p>Resolves the correct {@link ConnectCommand} and {@link OutputClassifier} for the current
 * platform at construction time. Each attempt is bounded by {@code timeout}; a timeout (or any other
 * shell exception) is caught and returned as {@link Outcome.Failure}, so callers (particularly batch
 * mode) never crash or hang on a single bad attempt.
 */
public class ShellNetworkConnector implements NetworkConnector {

    /** Used when no explicit timeout is given (for example by tests). */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final ShellRunner shell;
    private final Logger logger;
    private final Duration timeout;
    private final ConnectCommand connectCommand;
    private final OutputClassifier outputClassifier;

    public ShellNetworkConnector(ShellRunner shell, Platform platform, Logger logger) {
        this(shell, platform, logger, DEFAULT_TIMEOUT);
    }

    public ShellNetworkConnector(ShellRunner shell, Platform platform, Logger logger, Duration timeout) {
        this.shell = shell;
        this.logger = logger;
        this.timeout = timeout;
        this.connectCommand = platform.resolve(ConnectCommand.class);
        this.outputClassifier = platform.resolve(OutputClassifier.class);
    }

    @Override
    public Outcome evaluate(Network network, Password password) {
        String ssid = network.ssid();
        logger.debug("Attempting connection to '" + ssid + "'");
        try {
            String cmd = connectCommand.build(ssid, password);
            String output = shell.run(cmd, timeout);
            Outcome outcome = outputClassifier.classify(output);
            logger.debug("Connection result: " + outcome.getClass().getSimpleName() + " | output: " + output);
            return outcome;
        } catch (Exception e) {
            logger.debug("Connection exception: " + e.getMessage());
            return new Outcome.Failure(e.getMessage());
        }
    }
}
