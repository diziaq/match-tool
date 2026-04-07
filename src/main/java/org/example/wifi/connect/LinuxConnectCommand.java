package org.example.wifi.connect;

class LinuxConnectCommand implements ConnectCommand {

    @Override
    public String build(String ssid, String password) {
        return "nmcli dev wifi connect "
            + ShellEscape.quoted(ssid) + " password " + ShellEscape.quoted(password);
    }
}
