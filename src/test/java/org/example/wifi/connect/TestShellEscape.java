package org.example.wifi.connect;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * ShellEscape wraps an argument in POSIX single quotes so the shell treats the whole value as one
 * literal token. The injection tests exist to stop a regression that would reopen shell injection
 * through an SSID or a password; they must keep passing even if the quoting is rewritten.
 */
class TestShellEscape {

    @Nested
    class BaseCases {

        @Test
        void wrapsPlainValueInSingleQuotes() {
            assertThat(ShellEscape.quoted("HomeWiFi")).isEqualTo("'HomeWiFi'");
        }

        @Test
        void wrapsEmptyValueInEmptyQuotes() {
            assertThat(ShellEscape.quoted("")).isEqualTo("''");
        }

        @Test
        void keepsSpacesInsideOneToken() {
            assertThat(ShellEscape.quoted("my network")).isEqualTo("'my network'");
        }
    }

    @Nested
    class Escaping {

        /** The only character that needs work inside single quotes is the single quote itself. */
        @Test
        void escapesEmbeddedSingleQuote() {
            assertThat(ShellEscape.quoted("O'Brien")).isEqualTo("'O'\\''Brien'");
        }

        @Test
        void escapesEverySingleQuote() {
            assertThat(ShellEscape.quoted("a'b'c")).isEqualTo("'a'\\''b'\\''c'");
        }
    }

    @Nested
    class Injection {

        /** Command separators stay literal because the whole value is inside single quotes. */
        @Test
        void neutralisesCommandSeparators() {
            assertThat(ShellEscape.quoted("net; rm -rf /")).isEqualTo("'net; rm -rf /'");
        }

        /** The dangerous shape: close the quote, then inject. The lone quote must be escaped. */
        @Test
        void neutralisesQuoteBreakoutAttempt() {
            String escaped = ShellEscape.quoted("'; rm -rf / #");

            assertThat(escaped).isEqualTo("''\\''; rm -rf / #'");
            assertThat(escaped).startsWith("'").endsWith("'");
        }
    }
}
