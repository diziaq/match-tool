package org.example;

import org.example.wifi.scan.LinuxNetworkScanner;
import org.example.wifi.scan.MacOsNetworkScanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TestPlatform {

    private String savedOsName;

    @BeforeEach
    void saveOsName() { savedOsName = System.getProperty("os.name"); }

    @AfterEach
    void restoreOsName() { System.setProperty("os.name", savedOsName); }

    @Nested
    class Detection {

        @Test
        void detectsMacOs_forMacOsX() {
            System.setProperty("os.name", "Mac OS X");

            assertThat(Platform.detect()).isEqualTo(Platform.MACOS);
        }

        @Test
        void detectsMacOs_forMacOs() {
            System.setProperty("os.name", "macOS");

            assertThat(Platform.detect()).isEqualTo(Platform.MACOS);
        }

        @Test
        void detectsLinux_forLinux() {
            System.setProperty("os.name", "Linux");

            assertThat(Platform.detect()).isEqualTo(Platform.LINUX);
        }

        @Test
        void throwsForWindows() {
            System.setProperty("os.name", "Windows 11");

            assertThatThrownBy(Platform::detect)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Windows 11")
                .hasMessageContaining("Unsupported");
        }

        @Test
        void throwsForEmptyOsName() {
            System.setProperty("os.name", "");

            assertThatThrownBy(Platform::detect)
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void toStringIncludesOsName() {
            System.setProperty("os.name", "Mac OS X");

            assertThat(Platform.detect().toString()).contains("MACOS");
        }
    }

    @Nested
    class Factories {

        private final org.example.shell.ShellRunner noop = cmd -> "";

        @Test
        void newScanner_returnsMacOsScanner() {
            assertThat(Platform.MACOS.newScanner(noop)).isInstanceOf(MacOsNetworkScanner.class);
        }

        @Test
        void newScanner_returnsLinuxScanner() {
            assertThat(Platform.LINUX.newScanner(noop)).isInstanceOf(LinuxNetworkScanner.class);
        }
    }
}
