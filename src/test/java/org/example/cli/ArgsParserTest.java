package org.example.cli;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class ArgsParserTest {

    @TempDir Path tempDir;

    @Nested
    class SuccessfulParsing {

        @Test
        void parsesIntegerParam() throws Exception {
            var parser = new ArgsParser(Map.of("count", ParamSpec.of(ParamType.INTEGER)));

            ParsedArgs result = parser.parse(new String[]{"--count", "42"});

            assertEquals(42, (Integer) result.get("count"));
        }

        @Test
        void parsesPathParam() throws Exception {
            Path file = tempDir.resolve("something.txt");
            Files.writeString(file, "data");
            var parser = new ArgsParser(Map.of("dir", ParamSpec.of(ParamType.PATH)));

            ParsedArgs result = parser.parse(new String[]{"--dir", file.toString()});

            assertEquals(file, (Path) result.get("dir"));
        }

        @Test
        void parsesMultipleParams() throws Exception {
            Path file = tempDir.resolve("x.txt");
            Files.writeString(file, "data");
            var parser = new ArgsParser(Map.of(
                "count", ParamSpec.of(ParamType.INTEGER),
                "input", ParamSpec.of(ParamType.PATH)
            ));

            ParsedArgs result = parser.parse(new String[]{"--count", "5", "--input", file.toString()});

            assertEquals(5, (Integer) result.get("count"));
            assertEquals(file, (Path) result.get("input"));
        }

        @Test
        void emptyArgs_withNoRequiredParams() throws Exception {
            var parser = new ArgsParser(Map.of("count", ParamSpec.of(ParamType.INTEGER)));

            ParsedArgs result = parser.parse(new String[]{});

            assertFalse(result.has("count"));
        }

        @Test
        void optionalParam_absentWhenNotProvided() throws Exception {
            var parser = new ArgsParser(Map.of("count", ParamSpec.of(ParamType.INTEGER)));

            ParsedArgs result = parser.parse(new String[]{});

            assertTrue(result.getOptional("count").isEmpty());
        }

        @Test
        void optionalParam_presentWhenProvided() throws Exception {
            var parser = new ArgsParser(Map.of("count", ParamSpec.of(ParamType.INTEGER)));

            ParsedArgs result = parser.parse(new String[]{"--count", "7"});

            assertEquals(7, result.<Integer>getOptional("count").orElseThrow());
        }

        @Test
        void pathValidation_passesForExistingNonEmptyFile() throws Exception {
            Path file = tempDir.resolve("data.txt");
            Files.writeString(file, "hello");

            var parser = new ArgsParser(Map.of(
                "input", ParamSpec.of(ParamType.PATH)
                                  .required()
                                  .validatedBy(ParamSpec.existingNonEmptyFile(), "must be an existing non-empty file")
            ));

            assertDoesNotThrow(() -> parser.parse(new String[]{"--input", file.toString()}));
        }

        @Test
        void negativeIntegerIsAccepted() throws Exception {
            var parser = new ArgsParser(Map.of("level", ParamSpec.of(ParamType.INTEGER)));

            ParsedArgs result = parser.parse(new String[]{"--level", "-5"});

            assertEquals(-5, (Integer) result.get("level"));
        }
    }

    @Nested
    class ValidationErrors {

        @Test
        void throwsOnUnknownParam() {
            var parser = new ArgsParser(Map.of("known", ParamSpec.of(ParamType.INTEGER)));

            var ex = assertThrows(ArgsParseException.class,
                () -> parser.parse(new String[]{"--unknown", "val"}));
            assertTrue(ex.getMessage().contains("--unknown"));
        }

        @Test
        void throwsOnMissingRequiredParam() {
            var parser = new ArgsParser(Map.of("input", ParamSpec.of(ParamType.PATH).required()));

            var ex = assertThrows(ArgsParseException.class,
                () -> parser.parse(new String[]{}));
            assertTrue(ex.getMessage().contains("--input"));
        }

        @Test
        void throwsOnInvalidInteger() {
            var parser = new ArgsParser(Map.of("count", ParamSpec.of(ParamType.INTEGER)));

            var ex = assertThrows(ArgsParseException.class,
                () -> parser.parse(new String[]{"--count", "not-a-number"}));
            assertTrue(ex.getMessage().contains("--count"));
        }

        @Test
        void throwsOnMissingValue() {
            var parser = new ArgsParser(Map.of("count", ParamSpec.of(ParamType.INTEGER)));

            assertThrows(ArgsParseException.class,
                () -> parser.parse(new String[]{"--count"}));
        }

        @Test
        void throwsOnDuplicateParam() {
            var parser = new ArgsParser(Map.of("count", ParamSpec.of(ParamType.INTEGER)));

            assertThrows(ArgsParseException.class,
                () -> parser.parse(new String[]{"--count", "1", "--count", "2"}));
        }

        @Test
        void throwsOnPathValidationFailure_fileNotFound(@TempDir Path dir) {
            Path missing = dir.resolve("nonexistent.csv");

            var parser = new ArgsParser(Map.of(
                "input", ParamSpec.of(ParamType.PATH).required()
            ));

            var ex = assertThrows(ArgsParseException.class,
                () -> parser.parse(new String[]{"--input", missing.toString()}));
            assertTrue(ex.getMessage().contains("--input"));
        }

        @Test
        void throwsOnPathValidationFailure_emptyFile() throws IOException {
            Path empty = tempDir.resolve("empty.csv");
            Files.createFile(empty);

            var parser = new ArgsParser(Map.of(
                "input", ParamSpec.of(ParamType.PATH)
                                  .validatedBy(ParamSpec.existingNonEmptyFile(), "must be an existing non-empty file")
            ));

            assertThrows(ArgsParseException.class,
                () -> parser.parse(new String[]{"--input", empty.toString()}));
        }

        @Test
        void throwsOnMissingValueWhenNextTokenIsFlag() {
            var parser = new ArgsParser(Map.of(
                "a", ParamSpec.of(ParamType.INTEGER),
                "b", ParamSpec.of(ParamType.INTEGER)
            ));

            var ex = assertThrows(ArgsParseException.class,
                () -> parser.parse(new String[]{"--a", "--b"}));
            assertTrue(ex.getMessage().contains("--a"));
        }

        @Test
        void throwsOnBareTokenWithoutDashDash() {
            var parser = new ArgsParser(Map.of("count", ParamSpec.of(ParamType.INTEGER)));

            assertThrows(ArgsParseException.class,
                () -> parser.parse(new String[]{"count", "5"}));
        }
    }

    @Nested
    class ParsedArgsAccess {

        @Test
        void get_throwsNoSuchElementForAbsentParam() throws Exception {
            var parser = new ArgsParser(Map.of("count", ParamSpec.of(ParamType.INTEGER)));
            ParsedArgs result = parser.parse(new String[]{});

            assertThrows(NoSuchElementException.class, () -> result.get("count"));
        }

        @Test
        void has_returnsTrueWhenPresent() throws Exception {
            var parser = new ArgsParser(Map.of("count", ParamSpec.of(ParamType.INTEGER)));
            ParsedArgs result = parser.parse(new String[]{"--count", "3"});

            assertTrue(result.has("count"));
        }

        @Test
        void has_returnsFalseWhenAbsent() throws Exception {
            var parser = new ArgsParser(Map.of("count", ParamSpec.of(ParamType.INTEGER)));
            ParsedArgs result = parser.parse(new String[]{});

            assertFalse(result.has("count"));
        }
    }

    @Nested
    class CustomTypeExtensibility {

        @Test
        void customStringType_canBeAddedAsNewParamType() throws Exception {
            ParamType<String> STRING = new ParamType<>("String", s -> s);
            var parser = new ArgsParser(Map.of("mode", ParamSpec.of(STRING)));

            ParsedArgs result = parser.parse(new String[]{"--mode", "list"});

            assertEquals("list", result.<String>get("mode"));
        }

        @Test
        void customValidatorOnInteger() throws Exception {
            var parser = new ArgsParser(Map.of(
                "port", ParamSpec.of(ParamType.INTEGER)
                                 .validatedBy(n -> n >= 1 && n <= 65535, "must be a valid port number")
            ));

            assertDoesNotThrow(() -> parser.parse(new String[]{"--port", "8080"}));

            var ex = assertThrows(ArgsParseException.class,
                () -> parser.parse(new String[]{"--port", "99999"}));
            assertTrue(ex.getMessage().contains("must be a valid port number"));
        }
    }
}
