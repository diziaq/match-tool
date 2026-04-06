package org.example.cli;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class TestArgsParser {

    @TempDir Path tempDir;

    @Nested
    class SuccessfulParsing {

        @Test
        void parsesIntegerParam() throws Exception {
            ParsedArgs result = new ArgsParser()
                .registerRequired("count", Integer.class, ArgsParser.INTEGER)
                .parse(new String[]{"--count", "42"});

            assertThat((Integer) result.get("count")).isEqualTo(42);
        }

        @Test
        void parsesPathParam() throws Exception {
            Path file = tempDir.resolve("data.txt");
            Files.writeString(file, "hello");

            ParsedArgs result = new ArgsParser()
                .registerRequired("input", Path.class, ArgsParser.PATH)
                .parse(new String[]{"--input", file.toString()});

            assertThat((Path) result.get("input")).isEqualTo(file);
        }

        @Test
        void parsesMultipleParams() throws Exception {
            Path file = tempDir.resolve("x.csv");
            Files.writeString(file, "data");

            ParsedArgs result = new ArgsParser()
                .registerRequired("count", Integer.class, ArgsParser.INTEGER)
                .registerRequired("input", Path.class,   ArgsParser.PATH)
                .parse(new String[]{"--count", "5", "--input", file.toString()});

            assertThat((Integer) result.get("count")).isEqualTo(5);
            assertThat((Path) result.get("input")).isEqualTo(file);
        }

        @Test
        void optionalParam_usesDefaultWhenAbsent() throws Exception {
            ParsedArgs result = new ArgsParser()
                .registerOptional("count", Integer.class, ArgsParser.INTEGER, 99)
                .parse(new String[]{});

            assertThat((Integer) result.get("count")).isEqualTo(99);
        }

        @Test
        void optionalParam_usesProvidedValueWhenPresent() throws Exception {
            ParsedArgs result = new ArgsParser()
                .registerOptional("count", Integer.class, ArgsParser.INTEGER, 99)
                .parse(new String[]{"--count", "7"});

            assertThat((Integer) result.get("count")).isEqualTo(7);
        }

        @Test
        void negativeIntegerIsAccepted() throws Exception {
            ParsedArgs result = new ArgsParser()
                .registerRequired("level", Integer.class, ArgsParser.INTEGER)
                .parse(new String[]{"--level", "-5"});

            assertThat((Integer) result.get("level")).isEqualTo(-5);
        }

        @Test
        void customStringType() throws Exception {
            ParsedArgs result = new ArgsParser()
                .registerRequired("mode", String.class, s -> s)
                .parse(new String[]{"--mode", "list"});

            assertThat(result.<String>get("mode")).isEqualTo("list");
        }
    }

    @Nested
    class ValidationErrors {

        @Test
        void throwsOnUnknownParam() {
            assertThatThrownBy(() ->
                new ArgsParser()
                    .registerRequired("known", Integer.class, ArgsParser.INTEGER)
                    .parse(new String[]{"--unknown", "val"}))
                .isInstanceOf(ArgsParser.ParseException.class)
                .hasMessageContaining("--unknown");
        }

        @Test
        void throwsOnMissingRequiredParam() {
            assertThatThrownBy(() ->
                new ArgsParser()
                    .registerRequired("input", Path.class, ArgsParser.PATH)
                    .parse(new String[]{}))
                .isInstanceOf(ArgsParser.ParseException.class)
                .hasMessageContaining("--input");
        }

        @Test
        void throwsOnInvalidInteger() {
            assertThatThrownBy(() ->
                new ArgsParser()
                    .registerRequired("count", Integer.class, ArgsParser.INTEGER)
                    .parse(new String[]{"--count", "not-a-number"}))
                .isInstanceOf(ArgsParser.ParseException.class)
                .hasMessageContaining("--count");
        }

        @Test
        void throwsOnMissingValue() {
            assertThatThrownBy(() ->
                new ArgsParser()
                    .registerRequired("count", Integer.class, ArgsParser.INTEGER)
                    .parse(new String[]{"--count"}))
                .isInstanceOf(ArgsParser.ParseException.class);
        }

        @Test
        void throwsOnDuplicateParam() {
            assertThatThrownBy(() ->
                new ArgsParser()
                    .registerRequired("count", Integer.class, ArgsParser.INTEGER)
                    .parse(new String[]{"--count", "1", "--count", "2"}))
                .isInstanceOf(ArgsParser.ParseException.class);
        }

        @Test
        void throwsOnMissingValueWhenNextTokenIsFlag() {
            assertThatThrownBy(() ->
                new ArgsParser()
                    .registerRequired("a", Integer.class, ArgsParser.INTEGER)
                    .registerRequired("b", Integer.class, ArgsParser.INTEGER)
                    .parse(new String[]{"--a", "--b"}))
                .isInstanceOf(ArgsParser.ParseException.class)
                .hasMessageContaining("--a");
        }

        @Test
        void throwsOnBareTokenWithoutDashDash() {
            assertThatThrownBy(() ->
                new ArgsParser()
                    .registerRequired("count", Integer.class, ArgsParser.INTEGER)
                    .parse(new String[]{"count", "5"}))
                .isInstanceOf(ArgsParser.ParseException.class);
        }

        @Test
        void throwsOnNonExistentPath() {
            Path missing = tempDir.resolve("nonexistent.csv");

            assertThatThrownBy(() ->
                new ArgsParser()
                    .registerRequired("input", Path.class, ArgsParser.PATH)
                    .parse(new String[]{"--input", missing.toString()}))
                .isInstanceOf(ArgsParser.ParseException.class)
                .hasMessageContaining("--input");
        }

        @Test
        void collectsAllErrorsAndReportsAtOnce() {
            assertThatThrownBy(() ->
                new ArgsParser()
                    .registerRequired("count",  Integer.class, ArgsParser.INTEGER)
                    .registerRequired("factor", Integer.class, ArgsParser.INTEGER)
                    .registerRequired("level",  Integer.class, ArgsParser.INTEGER)
                    .parse(new String[]{"--count", "bad", "--factor", "also-bad"}))
                .isInstanceOf(ArgsParser.ParseException.class)
                .hasMessageContaining("--count")
                .hasMessageContaining("--factor")
                .hasMessageContaining("--level");
        }

        @Test
        void customValidationViaParseFunction() {
            assertThatThrownBy(() ->
                new ArgsParser()
                    .registerRequired("port", Integer.class, raw -> {
                        int n = Integer.parseInt(raw);
                        if (n < 1 || n > 65535) throw new IllegalArgumentException("must be 1–65535");
                        return n;
                    })
                    .parse(new String[]{"--port", "99999"}))
                .isInstanceOf(ArgsParser.ParseException.class)
                .hasMessageContaining("--port")
                .hasMessageContaining("must be 1–65535");
        }
    }

    @Nested
    class ParsedArgsAccess {

        @Test
        void get_throwsForUnknownName() throws Exception {
            ParsedArgs result = new ArgsParser()
                .registerRequired("count", Integer.class, ArgsParser.INTEGER)
                .parse(new String[]{"--count", "3"});

            assertThatThrownBy(() -> result.get("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void get_returnsResolvedValue() throws Exception {
            ParsedArgs result = new ArgsParser()
                .registerRequired("count", Integer.class, ArgsParser.INTEGER)
                .parse(new String[]{"--count", "3"});

            assertThat((Integer) result.get("count")).isEqualTo(3);
        }

        @Test
        void get_returnsDefaultValueForOptional() throws Exception {
            ParsedArgs result = new ArgsParser()
                .registerOptional("mode", String.class, s -> s, "default-mode")
                .parse(new String[]{});

            assertThat(result.<String>get("mode")).isEqualTo("default-mode");
        }
    }
}
