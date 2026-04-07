package org.example.wifi.connect;

/**
 * Builds the {@code networksetup -setairportnetwork en0 'SSID' 'PASSWORD'} command used on macOS.
 * SSID and password are single-quote-escaped to handle special characters safely.
 */
class MacOsConnectCommand implements ConnectCommand {

    @Override
    public String build(String ssid, String password) {
        return "networksetup -setairportnetwork en0 "
            + ShellEscape.quoted(ssid) + " " + ShellEscape.quoted(password);
    }
}
