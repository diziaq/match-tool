package org.example.wifi.connect;

import org.example.Platform;
import org.example.wifi.Password;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Pins the exact macOS {@code networksetup} command shape and that both the SSID and the password
 * are shell-escaped. The escaping cases guard against command injection through a crafted network
 * name or password; {@code TestShellEscape} owns the escaping rules themselves.
 */
class TestMacOsConnectCommand {

    private final MacOsConnectCommand command = new MacOsConnectCommand();

    @Nested
    class BaseCases {

        @Test
        void buildsNetworkSetupCommandWithQuotedArguments() {
            assertThat(command.build("HomeWiFi", new Password("secret")))
                .isEqualTo("networksetup -setairportnetwork en0 'HomeWiFi' 'secret'");
        }
    }

    @Nested
    class Escaping {

        @Test
        void escapesSingleQuoteInSsid() {
            assertThat(command.build("O'Brien", new Password("pw")))
                .isEqualTo("networksetup -setairportnetwork en0 'O'\\''Brien' 'pw'");
        }

        @Test
        void escapesSingleQuoteInPassword() {
            assertThat(command.build("Net", new Password("it's secret")))
                .isEqualTo("networksetup -setairportnetwork en0 'Net' 'it'\\''s secret'");
        }
    }

    @Nested
    class Factory {

        @Test
        void ofMacOsReturnsMacCommand() {
            assertThat(ConnectCommand.of(Platform.MACOS)).isInstanceOf(MacOsConnectCommand.class);
        }
    }
}
