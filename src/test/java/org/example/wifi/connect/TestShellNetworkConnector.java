package org.example.wifi.connect;

import org.example.Platform;
import org.example.io.Logger;
import org.example.matcher.MatchOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.*;

class TestShellNetworkConnector {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void suppressOutput() {
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        System.setErr(new PrintStream(new ByteArrayOutputStream()));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private Logger silentLogger() {
        return new Logger(Logger.Output.CONSOLE, false);
    }

    @Nested
    class MacBehaviour {

        @Test
        void returnsMatchWhenOutputBlank() {
            var connector = new ShellNetworkConnector(cmd -> "", Platform.MACOS, silentLogger());

            assertThat(connector.tryConnect("HomeWiFi", "secret"))
                .isInstanceOf(MatchOutcome.Match.class);
        }

        @Test
        void returnsUnavailableWhenCouldNotFind() {
            var connector = new ShellNetworkConnector(
                cmd -> "Could not find network HomeWiFi.",
                Platform.MACOS, silentLogger());

            assertThat(connector.tryConnect("HomeWiFi", "pass"))
                .isInstanceOf(MatchOutcome.Unavailable.class);
        }

        @Test
        void returnsMismatchForTmpErrOutput() {
            var connector = new ShellNetworkConnector(
                cmd -> "Failed to join network HomeWiFi.\nError: -3925  The operation couldn't be completed. tmpErr",
                Platform.MACOS, silentLogger());

            assertThat(connector.tryConnect("HomeWiFi", "wrong"))
                .isInstanceOf(MatchOutcome.Mismatch.class);
        }

        @Test
        void returnsFailureForApple80211Error() {
            var connector = new ShellNetworkConnector(
                cmd -> "Failed to join network HomeWiFi.\nError: -3912  The operation couldn't be completed. (com.apple.wifi.apple80211API.error error -3912.)",
                Platform.MACOS, silentLogger());

            assertThat(connector.tryConnect("HomeWiFi", "pass"))
                .isInstanceOf(MatchOutcome.Failure.class);
        }

        @Test
        void returnsFailureForUnrecognizedOutput() {
            var connector = new ShellNetworkConnector(
                cmd -> "some unexpected error",
                Platform.MACOS, silentLogger());

            assertThat(connector.tryConnect("HomeWiFi", "pass"))
                .isInstanceOf(MatchOutcome.Failure.class);
        }

        @Test
        void tryConnect_usesNetworksetupCommand() {
            String[] captured = new String[1];
            var connector = new ShellNetworkConnector(
                cmd -> { captured[0] = cmd; return ""; },
                Platform.MACOS, silentLogger());

            connector.tryConnect("MyNet", "pass");

            assertThat(captured[0]).contains("networksetup").contains("-setairportnetwork");
        }
    }

    @Nested
    class LinuxBehaviour {

        @Test
        void returnsMatchWhenOutputContainsSuccessfully() {
            var connector = new ShellNetworkConnector(
                cmd -> "Device 'wlan0' successfully connected.",
                Platform.LINUX, silentLogger());

            assertThat(connector.tryConnect("HomeWiFi", "secret"))
                .isInstanceOf(MatchOutcome.Match.class);
        }

        @Test
        void returnsFailureWhenOutputLacksSuccessfully() {
            var connector = new ShellNetworkConnector(
                cmd -> "Error: Connection failed.",
                Platform.LINUX, silentLogger());

            assertThat(connector.tryConnect("HomeWiFi", "wrong"))
                .isInstanceOf(MatchOutcome.Failure.class);
        }

        @Test
        void returnsMatchCaseInsensitiveForSuccessfully() {
            var connector = new ShellNetworkConnector(
                cmd -> "SUCCESSFULLY connected",
                Platform.LINUX, silentLogger());

            assertThat(connector.tryConnect("Net", "pass"))
                .isInstanceOf(MatchOutcome.Match.class);
        }

        @Test
        void tryConnect_usesNmcliCommand() {
            String[] captured = new String[1];
            var connector = new ShellNetworkConnector(
                cmd -> { captured[0] = cmd; return ""; },
                Platform.LINUX, silentLogger());

            connector.tryConnect("MyNet", "pass");

            assertThat(captured[0]).contains("nmcli").contains("password");
        }
    }

    @Nested
    class SharedBehaviour {

        @Test
        void returnsFailureOnException() {
            var connector = new ShellNetworkConnector(
                cmd -> { throw new RuntimeException("network down"); },
                Platform.MACOS, silentLogger());

            assertThat(connector.tryConnect("Net", "pass"))
                .isInstanceOf(MatchOutcome.Failure.class);
        }

        @Test
        void failureReason_containsExceptionMessage() {
            var connector = new ShellNetworkConnector(
                cmd -> { throw new RuntimeException("network down"); },
                Platform.MACOS, silentLogger());

            MatchOutcome outcome = connector.tryConnect("Net", "pass");

            assertThat(((MatchOutcome.Failure) outcome).reason()).contains("network down");
        }

        @Test
        void ssidWithSingleQuote_isProperlyEscaped() {
            String[] captured = new String[1];
            var connector = new ShellNetworkConnector(
                cmd -> { captured[0] = cmd; return ""; },
                Platform.MACOS, silentLogger());

            connector.tryConnect("O'Brien's WiFi", "pass");

            assertThat(captured[0]).contains("O'\\''Brien");
        }

        @Test
        void passwordWithSingleQuote_isProperlyEscaped() {
            String[] captured = new String[1];
            var connector = new ShellNetworkConnector(
                cmd -> { captured[0] = cmd; return ""; },
                Platform.MACOS, silentLogger());

            connector.tryConnect("Net", "it's secret");

            assertThat(captured[0]).contains("it'\\''s secret");
        }
    }
}
