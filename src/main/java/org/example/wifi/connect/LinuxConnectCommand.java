package org.example.wifi.connect;

import org.example.wifi.Password;

/**
 * Builds the {@code nmcli dev wifi connect 'SSID' password 'PASSWORD'} command used on Linux.
 * SSID and password are single-quote-escaped to handle special characters safely.
 */
class LinuxConnectCommand implements ConnectCommand {

    @Override
    public String build(String ssid, Password password) {
        return "nmcli dev wifi connect "
            + ShellEscape.quoted(ssid) + " password " + ShellEscape.quoted(password.value());
    }
}
