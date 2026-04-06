package org.example.wifi.connect;

import org.example.io.Logger;
import org.example.shell.ShellRunner;

public class ShellNetworkConnector implements NetworkConnector {

    private final ShellRunner shell;
    private final boolean isMac;
    private final Logger logger;

    public ShellNetworkConnector(ShellRunner shell, boolean isMac, Logger logger) {
        this.shell = shell;
        this.isMac = isMac;
        this.logger = logger;
    }

    @Override
    public boolean tryConnect(String ssid, String password) {
        logger.debug("Attempting connection to '" + ssid + "'");
        try {
            String cmd = isMac
                ? "networksetup -setairportnetwork en0 " + quoted(ssid) + " " + quoted(password)
                : "nmcli dev wifi connect " + quoted(ssid) + " password " + quoted(password);
            String output = shell.run(cmd);
            boolean success = isMac ? output.isBlank() : output.toLowerCase().contains("successfully");
            logger.debug("Connection result: " + (success ? "SUCCESS" : "FAILED"));
            return success;
        } catch (Exception e) {
            logger.debug("Connection exception: " + e.getMessage());
            return false;
        }
    }

    public String connect(String ssid, String password) throws Exception {
        String cmd = isMac
            ? "networksetup -setairportnetwork en0 " + quoted(ssid) + " " + quoted(password)
            : "nmcli dev wifi connect " + quoted(ssid) + " password " + quoted(password);
        String output = shell.run(cmd);
        return output.isBlank() ? "Connected to: " + ssid : output;
    }

    private static String quoted(String s) {
        return "'" + s.replace("'", "'\\''") + "'";
    }
}
