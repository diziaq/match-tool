package org.example.cli;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class TestParsedArgs {

    @Nested
    class LazyEvaluation {

        @Test
        void malformedValueDoesNotFailAtConstruction() {
            ArgSchema schema = ArgSchema.builder()
                .required("count", Integer.class, ArgSchema.INTEGER)
                .build();

            // construction succeeds — no parsing yet
            ParsedArgs args = new ParsedArgs(schema, Map.of("count", "not-a-number"));

            // fails on first get
            assertThatThrownBy(() -> args.get("count"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--count")
                .hasMessageContaining("Available parameters");
        }

        @Test
        void unusedMalformedParamNeverFails() {
            ArgSchema schema = ArgSchema.builder()
                .optional("count", Integer.class, ArgSchema.INTEGER, 0)
                .optional("name", String.class, s -> s, "default")
                .build();

            ParsedArgs args = new ParsedArgs(schema, Map.of("count", "bad", "name", "hello"));

            // only access the valid param — no error
            assertThat(args.<String>get("name")).isEqualTo("hello");
        }

        @Test
        void resolvedValueIsCachedOnSubsequentGets() {
            int[] callCount = {0};
            ArgSchema schema = ArgSchema.builder()
                .required("count", Integer.class, raw -> {
                    callCount[0]++;
                    return Integer.parseInt(raw);
                })
                .build();

            ParsedArgs args = new ParsedArgs(schema, Map.of("count", "42"));

            args.get("count");
            args.get("count");
            args.get("count");

            assertThat(callCount[0]).isEqualTo(1);
        }

        @Test
        void requiredButMissingFailsAtGetTime() {
            ArgSchema schema = ArgSchema.builder()
                .required("mode", Integer.class, ArgSchema.INTEGER)
                .build();

            ParsedArgs args = new ParsedArgs(schema, Map.of());

            assertThatThrownBy(() -> args.get("mode"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required but not provided")
                .hasMessageContaining("Available parameters");
        }

        @Test
        void multipleErrorsReportedIndependentlyPerGet() {
            ArgSchema schema = ArgSchema.builder()
                .required("count", Integer.class, ArgSchema.INTEGER)
                .required("factor", Integer.class, ArgSchema.INTEGER)
                .build();

            ParsedArgs args = new ParsedArgs(schema, Map.of("count", "bad", "factor", "also-bad"));

            assertThatThrownBy(() -> args.get("count"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--count");

            assertThatThrownBy(() -> args.get("factor"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--factor");
        }

        @Test
        void extraKeysInSourceAreIgnored() {
            ArgSchema schema = ArgSchema.builder()
                .required("a", String.class, s -> s)
                .build();

            // "b" is in source but not in schema — no error
            ParsedArgs args = new ParsedArgs(schema, Map.of("a", "1", "b", "2"));

            assertThat(args.<String>get("a")).isEqualTo("1");
        }
    }

    @Nested
    class ValueResolution {

        @Test
        void parsesIntegerFromRaw() {
            ArgSchema schema = ArgSchema.builder()
                .required("count", Integer.class, ArgSchema.INTEGER)
                .build();

            ParsedArgs args = new ParsedArgs(schema, Map.of("count", "42"));
            assertThat((Integer) args.get("count")).isEqualTo(42);
        }

        @Test
        void parsesPathFromRaw(@TempDir Path tempDir) throws Exception {
            Path file = tempDir.resolve("data.txt");
            Files.writeString(file, "hello");

            ArgSchema schema = ArgSchema.builder()
                .required("input", Path.class, ArgSchema.PATH)
                .build();

            ParsedArgs args = new ParsedArgs(schema, Map.of("input", file.toString()));
            assertThat((Path) args.get("input")).isEqualTo(file);
        }

        @Test
        void parsesNegativeInteger() {
            ArgSchema schema = ArgSchema.builder()
                .required("level", Integer.class, ArgSchema.INTEGER)
                .build();

            ParsedArgs args = new ParsedArgs(schema, Map.of("level", "-5"));
            assertThat((Integer) args.get("level")).isEqualTo(-5);
        }

        @Test
        void returnsDefaultForAbsentOptional() {
            ArgSchema schema = ArgSchema.builder()
                .optional("count", Integer.class, ArgSchema.INTEGER, 99)
                .build();

            ParsedArgs args = new ParsedArgs(schema, Map.of());
            assertThat((Integer) args.get("count")).isEqualTo(99);
        }

        @Test
        void providedValueOverridesDefault() {
            ArgSchema schema = ArgSchema.builder()
                .optional("count", Integer.class, ArgSchema.INTEGER, 99)
                .build();

            ParsedArgs args = new ParsedArgs(schema, Map.of("count", "7"));
            assertThat((Integer) args.get("count")).isEqualTo(7);
        }

        @Test
        void returnsNullDefaultForOptionalPath() {
            ArgSchema schema = ArgSchema.builder()
                .optional("input", Path.class, ArgSchema.PATH, null)
                .build();

            ParsedArgs args = new ParsedArgs(schema, Map.of());
            assertThat(args.<Path>get("input")).isNull();
        }

        @Test
        void customValidationViaParseFunction() {
            ArgSchema schema = ArgSchema.builder()
                .required("port", Integer.class, raw -> {
                    int n = Integer.parseInt(raw);
                    if (n < 1 || n > 65535) throw new IllegalArgumentException("must be 1–65535");
                    return n;
                })
                .build();

            ParsedArgs args = new ParsedArgs(schema, Map.of("port", "99999"));

            assertThatThrownBy(() -> args.get("port"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--port")
                .hasMessageContaining("must be 1–65535");
        }

        @Test
        void nonExistentPathFailsAtGetTime(@TempDir Path tempDir) {
            Path missing = tempDir.resolve("nonexistent.csv");

            ArgSchema schema = ArgSchema.builder()
                .required("input", Path.class, ArgSchema.PATH)
                .build();

            ParsedArgs args = new ParsedArgs(schema, Map.of("input", missing.toString()));

            assertThatThrownBy(() -> args.get("input"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--input");
        }
    }

    @Nested
    class UsageGuide {

        @Test
        void errorIncludesAllRegisteredParams() {
            ArgSchema schema = ArgSchema.builder()
                .required("mode", Integer.class, ArgSchema.INTEGER)
                .optional("skip", Integer.class, ArgSchema.INTEGER, 0)
                .optional("debug", Boolean.class, Boolean::parseBoolean, false)
                .build();

            ParsedArgs args = new ParsedArgs(schema, Map.of("mode", "bad"));

            assertThatThrownBy(() -> args.get("mode"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Available parameters")
                .hasMessageContaining("--mode")
                .hasMessageContaining("--skip")
                .hasMessageContaining("--debug")
                .hasMessageContaining("required")
                .hasMessageContaining("optional");
        }

        @Test
        void unknownParamGetIncludesGuide() {
            ArgSchema schema = ArgSchema.builder()
                .required("count", Integer.class, ArgSchema.INTEGER)
                .build();

            ParsedArgs args = new ParsedArgs(schema, Map.of("count", "3"));

            assertThatThrownBy(() -> args.get("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown parameter: --unknown")
                .hasMessageContaining("Available parameters")
                .hasMessageContaining("--count");
        }

        @Test
        void missingRequiredIncludesGuide() {
            ArgSchema schema = ArgSchema.builder()
                .required("mode", Integer.class, ArgSchema.INTEGER)
                .optional("debug", Boolean.class, Boolean::parseBoolean, false)
                .build();

            ParsedArgs args = new ParsedArgs(schema, Map.of());

            assertThatThrownBy(() -> args.get("mode"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required but not provided")
                .hasMessageContaining("--mode")
                .hasMessageContaining("--debug");
        }
    }

    @Nested
    class ConstructorVariants {

        @Test
        void constructsFromArgSource() {
            ArgSchema schema = ArgSchema.builder()
                .required("name", String.class, s -> s)
                .build();

            ArgSource source = new MapArgSource(Map.of("name", "hello"));
            ParsedArgs args = new ParsedArgs(schema, source);

            assertThat(args.<String>get("name")).isEqualTo("hello");
        }

        @Test
        void constructsFromRawMap() {
            ArgSchema schema = ArgSchema.builder()
                .required("name", String.class, s -> s)
                .build();

            ParsedArgs args = new ParsedArgs(schema, Map.of("name", "hello"));

            assertThat(args.<String>get("name")).isEqualTo("hello");
        }

        @Test
        void constructsFromCliArgSource() {
            ArgSchema schema = ArgSchema.builder()
                .required("count", Integer.class, ArgSchema.INTEGER)
                .optional("debug", Boolean.class, Boolean::parseBoolean, false)
                .build();

            ParsedArgs args = new ParsedArgs(schema, new CliArgSource(
                new String[]{"--count", "10", "--debug", "true"}));

            assertThat((Integer) args.get("count")).isEqualTo(10);
            assertThat((Boolean) args.get("debug")).isTrue();
        }

        @Test
        void constructsFromPropertiesArgSource() {
            ArgSchema schema = ArgSchema.builder()
                .required("count", Integer.class, ArgSchema.INTEGER)
                .build();

            java.util.Properties props = new java.util.Properties();
            props.setProperty("count", "7");

            ParsedArgs args = new ParsedArgs(schema, new PropertiesArgSource(props));

            assertThat((Integer) args.get("count")).isEqualTo(7);
        }
    }

    @Nested
    class TextSchemaIntegration {

        @Test
        void textSchemaWithMapSource() {
            ArgSchema schema = ArgSchema.fromText("""
                mode  : Integer : required
                skip  : Integer : optional : 0
                debug : Boolean : optional : false
                """);

            ParsedArgs args = new ParsedArgs(schema, Map.of("mode", "1"));

            assertThat((Integer) args.get("mode")).isEqualTo(1);
            assertThat((Integer) args.get("skip")).isEqualTo(0);
            assertThat((Boolean) args.get("debug")).isFalse();
        }

        @Test
        void textSchemaWithCliSource() {
            ArgSchema schema = ArgSchema.fromText("""
                count : Integer : required
                name  : String  : optional : anon
                """);

            ParsedArgs args = new ParsedArgs(schema,
                new CliArgSource(new String[]{"--count", "5", "--name", "bob"}));

            assertThat((Integer) args.get("count")).isEqualTo(5);
            assertThat(args.<String>get("name")).isEqualTo("bob");
        }
    }
}
