package org.example.wifi.connect;

import org.example.Platform;
import org.example.wifi.Password;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Pins the exact Linux {@code nmcli} command shape and that both the SSID and the password are
 * shell-escaped. {@code TestShellEscape} owns the escaping rules themselves.
 */
class TestLinuxConnectCommand {

    private final LinuxConnectCommand command = new LinuxConnectCommand();

    @Nested
    class BaseCases {

        @Test
        void buildsNmcliCommandWithQuotedArguments() {
            assertThat(command.build("HomeWiFi", new Password("secret")))
                .isEqualTo("nmcli dev wifi connect 'HomeWiFi' password 'secret'");
        }
    }

    @Nested
    class Escaping {

        @Test
        void escapesSingleQuoteInSsid() {
            assertThat(command.build("O'Brien", new Password("pw")))
                .isEqualTo("nmcli dev wifi connect 'O'\\''Brien' password 'pw'");
        }

        @Test
        void escapesSingleQuoteInPassword() {
            assertThat(command.build("Net", new Password("it's secret")))
                .isEqualTo("nmcli dev wifi connect 'Net' password 'it'\\''s secret'");
        }
    }

    @Nested
    class Factory {

        @Test
        void ofLinuxReturnsLinuxCommand() {
            assertThat(ConnectCommand.of(Platform.LINUX)).isInstanceOf(LinuxConnectCommand.class);
        }
    }
}
