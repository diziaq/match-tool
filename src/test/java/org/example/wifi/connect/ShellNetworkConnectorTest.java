package org.example.wifi.connect;

import org.example.io.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class ShellNetworkConnectorTest {

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
        void tryConnect_returnsTrueWhenOutputBlank() {
            var connector = new ShellNetworkConnector(cmd -> "", true, silentLogger());

            assertTrue(connector.tryConnect("HomeWiFi", "secret"));
        }

        @Test
        void tryConnect_returnsFalseWhenOutputNotBlank() {
            var connector = new ShellNetworkConnector(cmd -> "Error: wrong password", true, silentLogger());

            assertFalse(connector.tryConnect("HomeWiFi", "wrong"));
        }

        @Test
        void tryConnect_usesNetworksetupCommand() {
            String[] captured = new String[1];
            var connector = new ShellNetworkConnector(cmd -> { captured[0] = cmd; return ""; }, true, silentLogger());

            connector.tryConnect("MyNet", "pass");

            assertTrue(captured[0].contains("networksetup"));
            assertTrue(captured[0].contains("-setairportnetwork"));
        }

        @Test
        void connect_returnsSuccessMessageWhenOutputBlank() throws Exception {
            var connector = new ShellNetworkConnector(cmd -> "", true, silentLogger());

            assertEquals("Connected to: HomeWiFi", connector.connect("HomeWiFi", "pass"));
        }

        @Test
        void connect_returnsRawOutputWhenNotBlank() throws Exception {
            var connector = new ShellNetworkConnector(cmd -> "some error\n", true, silentLogger());

            assertEquals("some error\n", connector.connect("HomeWiFi", "pass"));
        }
    }

    @Nested
    class LinuxBehaviour {

        @Test
        void tryConnect_returnsTrueWhenOutputContainsSuccessfully() {
            var connector = new ShellNetworkConnector(cmd -> "Device 'wlan0' successfully connected.", false, silentLogger());

            assertTrue(connector.tryConnect("HomeWiFi", "secret"));
        }

        @Test
        void tryConnect_returnsFalseWhenOutputLacksSuccessfully() {
            var connector = new ShellNetworkConnector(cmd -> "Error: Connection failed.", false, silentLogger());

            assertFalse(connector.tryConnect("HomeWiFi", "wrong"));
        }

        @Test
        void tryConnect_isCaseInsensitiveForSuccessfully() {
            var connector = new ShellNetworkConnector(cmd -> "SUCCESSFULLY connected", false, silentLogger());

            assertTrue(connector.tryConnect("Net", "pass"));
        }

        @Test
        void tryConnect_usesNmcliCommand() {
            String[] captured = new String[1];
            var connector = new ShellNetworkConnector(cmd -> { captured[0] = cmd; return ""; }, false, silentLogger());

            connector.tryConnect("MyNet", "pass");

            assertTrue(captured[0].contains("nmcli"));
            assertTrue(captured[0].contains("password"));
        }
    }

    @Nested
    class SharedBehaviour {

        @Test
        void tryConnect_returnsFalseOnException() {
            var connector = new ShellNetworkConnector(cmd -> { throw new RuntimeException("network down"); }, true, silentLogger());

            assertFalse(connector.tryConnect("Net", "pass"));
        }

        @Test
        void ssidWithSingleQuote_isProperlyEscaped() {
            String[] captured = new String[1];
            var connector = new ShellNetworkConnector(cmd -> { captured[0] = cmd; return ""; }, true, silentLogger());

            connector.tryConnect("O'Brien's WiFi", "pass");

            assertTrue(captured[0].contains("O'\\''Brien"));
        }

        @Test
        void passwordWithSingleQuote_isProperlyEscaped() {
            String[] captured = new String[1];
            var connector = new ShellNetworkConnector(cmd -> { captured[0] = cmd; return ""; }, true, silentLogger());

            connector.tryConnect("Net", "it's secret");

            assertTrue(captured[0].contains("it'\\''s secret"));
        }
    }
}
