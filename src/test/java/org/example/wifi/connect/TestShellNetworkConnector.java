package org.example.wifi.connect;

import org.example.Platform;
import org.example.io.Logger;
import org.example.engine.Outcome;
import org.example.wifi.Network;
import org.example.wifi.Password;
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

    /** Wraps a bare SSID string into a Network (signal unknown — treated as Normal). */
    private static Network net(String ssid) { return Network.of(ssid); }

    /** Wraps a bare string into a Password. */
    private static Password pwd(String value) { return new Password(value); }

    @Nested
    class MacBehaviour {

        @Test
        void returnsMatchWhenOutputBlank() {
            var connector = new ShellNetworkConnector(cmd -> "", Platform.MACOS, silentLogger());

            assertThat(connector.evaluate(net("HomeWiFi"), pwd("secret")))
                .isInstanceOf(Outcome.Match.class);
        }

        @Test
        void returnsSkipWhenCouldNotFind() {
            var connector = new ShellNetworkConnector(
                cmd -> "Could not find network HomeWiFi.",
                Platform.MACOS, silentLogger());

            assertThat(connector.evaluate(net("HomeWiFi"), pwd("pass")))
                .isInstanceOf(Outcome.Skip.class);
        }

        @Test
        void returnsMismatchForTmpErrOutput() {
            var connector = new ShellNetworkConnector(
                cmd -> "Failed to join network HomeWiFi.\nError: -3925  The operation couldn't be completed. tmpErr",
                Platform.MACOS, silentLogger());

            assertThat(connector.evaluate(net("HomeWiFi"), pwd("wrong")))
                .isInstanceOf(Outcome.Mismatch.class);
        }

        @Test
        void returnsFailureForApple80211Error() {
            var connector = new ShellNetworkConnector(
                cmd -> "Failed to join network HomeWiFi.\nError: -3912  The operation couldn't be completed. (com.apple.wifi.apple80211API.error error -3912.)",
                Platform.MACOS, silentLogger());

            assertThat(connector.evaluate(net("HomeWiFi"), pwd("pass")))
                .isInstanceOf(Outcome.Failure.class);
        }

        @Test
        void returnsFailureForUnrecognizedOutput() {
            var connector = new ShellNetworkConnector(
                cmd -> "some unexpected error",
                Platform.MACOS, silentLogger());

            assertThat(connector.evaluate(net("HomeWiFi"), pwd("pass")))
                .isInstanceOf(Outcome.Failure.class);
        }

        @Test
        void test_usesNetworkSetupCommand() {
            String[] captured = new String[1];
            var connector = new ShellNetworkConnector(
                cmd -> { captured[0] = cmd; return ""; },
                Platform.MACOS, silentLogger());

            connector.evaluate(net("MyNet"), pwd("pass"));

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

            assertThat(connector.evaluate(net("HomeWiFi"), pwd("secret")))
                .isInstanceOf(Outcome.Match.class);
        }

        @Test
        void returnsFailureWhenOutputLacksSuccessfully() {
            var connector = new ShellNetworkConnector(
                cmd -> "Error: Connection failed.",
                Platform.LINUX, silentLogger());

            assertThat(connector.evaluate(net("HomeWiFi"), pwd("wrong")))
                .isInstanceOf(Outcome.Failure.class);
        }

        @Test
        void returnsMatchCaseInsensitiveForSuccessfully() {
            var connector = new ShellNetworkConnector(
                cmd -> "SUCCESSFULLY connected",
                Platform.LINUX, silentLogger());

            assertThat(connector.evaluate(net("Net"), pwd("pass")))
                .isInstanceOf(Outcome.Match.class);
        }

        @Test
        void test_usesNmcliCommand() {
            String[] captured = new String[1];
            var connector = new ShellNetworkConnector(
                cmd -> { captured[0] = cmd; return ""; },
                Platform.LINUX, silentLogger());

            connector.evaluate(net("MyNet"), pwd("pass"));

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

            assertThat(connector.evaluate(net("Net"), pwd("pass")))
                .isInstanceOf(Outcome.Failure.class);
        }

        @Test
        void failureReason_containsExceptionMessage() {
            var connector = new ShellNetworkConnector(
                cmd -> { throw new RuntimeException("network down"); },
                Platform.MACOS, silentLogger());

            Outcome outcome = connector.evaluate(net("Net"), pwd("pass"));

            assertThat(((Outcome.Failure) outcome).reason()).contains("network down");
        }

        @Test
        void ssidWithSingleQuote_isProperlyEscaped() {
            String[] captured = new String[1];
            var connector = new ShellNetworkConnector(
                cmd -> { captured[0] = cmd; return ""; },
                Platform.MACOS, silentLogger());

            connector.evaluate(net("O'Brien's WiFi"), pwd("pass"));

            assertThat(captured[0]).contains("O'\\''Brien");
        }

        @Test
        void passwordWithSingleQuote_isProperlyEscaped() {
            String[] captured = new String[1];
            var connector = new ShellNetworkConnector(
                cmd -> { captured[0] = cmd; return ""; },
                Platform.MACOS, silentLogger());

            connector.evaluate(net("Net"), pwd("it's secret"));

            assertThat(captured[0]).contains("it'\\''s secret");
        }

        @Test
        void networkSsid_isUsedInCommand() {
            String[] captured = new String[1];
            var connector = new ShellNetworkConnector(
                cmd -> { captured[0] = cmd; return ""; },
                Platform.MACOS, silentLogger());

            connector.evaluate(Network.of("TargetSSID", -45), pwd("pass"));

            assertThat(captured[0]).contains("TargetSSID");
        }
    }
}
