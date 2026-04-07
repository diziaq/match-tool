package org.example.wifi.connect;

class MacOsConnectCommand implements ConnectCommand {

    @Override
    public String build(String ssid, String password) {
        return "networksetup -setairportnetwork en0 "
            + ShellEscape.quoted(ssid) + " " + ShellEscape.quoted(password);
    }
}
