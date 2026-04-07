package org.example.wifi.connect;

import org.example.Platform;
import org.example.io.Logger;
import org.example.matcher.MatchOutcome;
import org.example.shell.ShellRunner;
import org.example.wifi.Network;
import org.example.wifi.Password;

/**
 * {@link NetworkConnector} that delegates to a {@link ShellRunner}.
 *
 * <p>Resolves the correct {@link ConnectCommand} and {@link OutputClassifier} for the current
 * platform at construction time. Any exception thrown by the shell runner is caught and returned as
 * {@link MatchOutcome.Failure}, so callers (particularly batch mode) never crash on a single bad
 * attempt.
 */
public class ShellNetworkConnector implements NetworkConnector {

    private final ShellRunner shell;
    private final Logger logger;
    private final ConnectCommand connectCommand;
    private final OutputClassifier outputClassifier;

    public ShellNetworkConnector(ShellRunner shell, Platform platform, Logger logger) {
        this.shell = shell;
        this.logger = logger;
        this.connectCommand = platform.resolve(ConnectCommand.class);
        this.outputClassifier = platform.resolve(OutputClassifier.class);
    }

    @Override
    public MatchOutcome test(Network network, Password password) {
        String ssid = network.ssid();
        logger.debug("Attempting connection to '" + ssid + "'");
        try {
            String cmd = connectCommand.build(ssid, password);
            String output = shell.run(cmd);
            MatchOutcome outcome = outputClassifier.classify(output);
            logger.debug("Connection result: " + outcome.getClass().getSimpleName() + " | output: " + output);
            return outcome;
        } catch (Exception e) {
            logger.debug("Connection exception: " + e.getMessage());
            return new MatchOutcome.Failure(e.getMessage());
        }
    }
}
