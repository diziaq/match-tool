package org.example.wifi.connect;

import org.example.Platform;
import org.example.io.Logger;
import org.example.shell.ShellRunner;

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
    public ConnectOutcome tryConnect(String ssid, String password) {
        logger.debug("Attempting connection to '" + ssid + "'");
        try {
            String cmd = connectCommand.build(ssid, password);
            String output = shell.run(cmd);
            ConnectOutcome outcome = outputClassifier.classify(output);
            logger.debug("Connection result: " + outcome.getClass().getSimpleName() + " | output: " + output);
            return outcome;
        } catch (Exception e) {
            logger.debug("Connection exception: " + e.getMessage());
            return new ConnectOutcome.UnknownFailure(e.getMessage());
        }
    }
}
