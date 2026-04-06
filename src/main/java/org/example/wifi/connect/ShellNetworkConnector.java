package org.example.wifi.connect;

import org.example.Platform;
import org.example.io.Logger;
import org.example.shell.ShellRunner;

public class ShellNetworkConnector implements NetworkConnector {

    private final ShellRunner shell;
    private final Platform platform;
    private final Logger logger;

    public ShellNetworkConnector(ShellRunner shell, Platform platform, Logger logger) {
        this.shell = shell;
        this.platform = platform;
        this.logger = logger;
    }

    @Override
    public ConnectOutcome tryConnect(String ssid, String password) {
        logger.debug("Attempting connection to '" + ssid + "'");
        try {
            String cmd = platform.resolve(ConnectCommand.class).build(ssid, password);
            String output = shell.run(cmd);
            ConnectOutcome outcome = platform.resolve(OutputClassifier.class).classify(output);
            logger.debug("Connection result: " + outcome.getClass().getSimpleName() + " | output: " + output);
            return outcome;
        } catch (Exception e) {
            logger.debug("Connection exception: " + e.getMessage());
            return new ConnectOutcome.UnknownFailure(e.getMessage());
        }
    }
}
