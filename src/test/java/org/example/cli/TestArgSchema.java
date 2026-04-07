package org.example.cli;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class TestArgSchema {

    @Nested
    class BuilderApi {

        @Test
        void registersRequiredParam() {
            ArgSchema schema = ArgSchema.builder()
                .required("count", Integer.class, ArgSchema.INTEGER)
                .build();

            assertThat(schema.contains("count")).isTrue();
            assertThat(schema.names()).containsExactly("count");
        }

        @Test
        void registersOptionalParam() {
            ArgSchema schema = ArgSchema.builder()
                .optional("debug", Boolean.class, Boolean::parseBoolean, false)
                .build();

            assertThat(schema.contains("debug")).isTrue();
        }

        @Test
        void registersMultipleParams() {
            ArgSchema schema = ArgSchema.builder()
                .required("mode", Integer.class, ArgSchema.INTEGER)
                .optional("skip", Integer.class, ArgSchema.INTEGER, 0)
                .optional("debug", Boolean.class, Boolean::parseBoolean, false)
                .build();

            assertThat(schema.names()).containsExactly("mode", "skip", "debug");
        }

        @Test
        void preservesRegistrationOrder() {
            ArgSchema schema = ArgSchema.builder()
                .required("z", Integer.class, ArgSchema.INTEGER)
                .required("a", Integer.class, ArgSchema.INTEGER)
                .required("m", Integer.class, ArgSchema.INTEGER)
                .build();

            assertThat(schema.names()).containsExactly("z", "a", "m");
        }

        @Test
        void containsReturnsFalseForUnknown() {
            ArgSchema schema = ArgSchema.builder()
                .required("count", Integer.class, ArgSchema.INTEGER)
                .build();

            assertThat(schema.contains("other")).isFalse();
        }

        @Test
        void emptySchema() {
            ArgSchema schema = ArgSchema.builder().build();

            assertThat(schema.names()).isEmpty();
            assertThat(schema.contains("anything")).isFalse();
        }

        @Test
        void laterRegistrationOverridesEarlier() {
            ArgSchema schema = ArgSchema.builder()
                .required("x", Integer.class, ArgSchema.INTEGER)
                .optional("x", String.class, s -> s, "hi")
                .build();

            // "x" should exist once
            assertThat(schema.names()).containsExactly("x");

            // verify it's the optional (String) version by resolving through ParsedArgs
            ParsedArgs args = new ParsedArgs(schema, Map.of());
            assertThat(args.<String>get("x")).isEqualTo("hi");
        }
    }

    @Nested
    class TextFormat {

        @Test
        void parsesRequiredParam() {
            ArgSchema schema = ArgSchema.fromText("mode : Integer : required");

            assertThat(schema.contains("mode")).isTrue();
            assertThat(schema.names()).containsExactly("mode");
        }

        @Test
        void parsesOptionalWithDefault() {
            ArgSchema schema = ArgSchema.fromText("skip : Integer : optional : 5");

            ParsedArgs args = new ParsedArgs(schema, Map.of());
            assertThat((Integer) args.get("skip")).isEqualTo(5);
        }

        @Test
        void parsesOptionalWithoutDefault() {
            ArgSchema schema = ArgSchema.fromText("left : Path : optional");

            ParsedArgs args = new ParsedArgs(schema, Map.of());
            assertThat(args.<Path>get("left")).isNull();
        }

        @Test
        void parsesMultipleLines() {
            String text = """
                mode  : Integer : required
                skip  : Integer : optional : 0
                debug : Boolean : optional : false
                name  : String  : optional : hello
                """;
            ArgSchema schema = ArgSchema.fromText(text);

            assertThat(schema.names()).containsExactly("mode", "skip", "debug", "name");
        }

        @Test
        void skipsCommentsAndBlankLines() {
            String text = """
                # This is a config file

                mode : Integer : required

                # Another comment
                skip : Integer : optional : 0
                """;
            ArgSchema schema = ArgSchema.fromText(text);

            assertThat(schema.names()).containsExactly("mode", "skip");
        }

        @Test
        void supportsBooleanType() {
            ArgSchema schema = ArgSchema.fromText("debug : Boolean : optional : true");

            ParsedArgs args = new ParsedArgs(schema, Map.of());
            assertThat((Boolean) args.get("debug")).isTrue();
        }

        @Test
        void supportsStringType() {
            ArgSchema schema = ArgSchema.fromText("name : String : required");

            ParsedArgs args = new ParsedArgs(schema, Map.of("name", "world"));
            assertThat(args.<String>get("name")).isEqualTo("world");
        }

        @Test
        void supportsPathType(@TempDir Path tempDir) throws Exception {
            Path file = tempDir.resolve("data.txt");
            Files.writeString(file, "content");

            ArgSchema schema = ArgSchema.fromText("input : Path : required");

            // Path parser needs an absolute path that exists
            ParsedArgs args = new ParsedArgs(schema, Map.of("input", file.toString()));
            assertThat((Path) args.get("input")).isEqualTo(file);
        }

        @Test
        void supportsIntegerType() {
            ArgSchema schema = ArgSchema.fromText("count : Integer : required");

            ParsedArgs args = new ParsedArgs(schema, Map.of("count", "42"));
            assertThat((Integer) args.get("count")).isEqualTo(42);
        }

        @Test
        void throwsOnMalformedLine() {
            assertThatThrownBy(() -> ArgSchema.fromText("bad line"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Line 1");
        }

        @Test
        void throwsOnUnknownType() {
            assertThatThrownBy(() -> ArgSchema.fromText("x : Float : required"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown type")
                .hasMessageContaining("Float");
        }

        @Test
        void throwsOnInvalidRequiredOptional() {
            assertThatThrownBy(() -> ArgSchema.fromText("x : Integer : maybe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maybe");
        }

        @Test
        void loadsFromFile(@TempDir Path tempDir) throws Exception {
            Path schemaFile = tempDir.resolve("args.txt");
            Files.writeString(schemaFile, """
                mode : Integer : required
                debug : Boolean : optional : false
                """);

            ArgSchema schema = ArgSchema.fromText(schemaFile);

            assertThat(schema.names()).containsExactly("mode", "debug");
        }

        @Test
        void emptyTextProducesEmptySchema() {
            ArgSchema schema = ArgSchema.fromText("");
            assertThat(schema.names()).isEmpty();
        }

        @Test
        void commentsOnlyProducesEmptySchema() {
            ArgSchema schema = ArgSchema.fromText("# just a comment\n# another one");
            assertThat(schema.names()).isEmpty();
        }
    }
}
