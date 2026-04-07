package org.example.cli;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class TestCliArgSource {

    @Nested
    class SuccessfulTokenisation {

        @Test
        void singleParam() {
            Map<String, String> raw = new CliArgSource(
                new String[]{"--count", "42"}).load();

            assertThat(raw).containsExactly(entry("count", "42"));
        }

        @Test
        void multipleParams() {
            Map<String, String> raw = new CliArgSource(
                new String[]{"--a", "1", "--b", "2", "--c", "3"}).load();

            assertThat(raw)
                .containsEntry("a", "1")
                .containsEntry("b", "2")
                .containsEntry("c", "3")
                .hasSize(3);
        }

        @Test
        void emptyArgsArray() {
            Map<String, String> raw = new CliArgSource(new String[]{}).load();

            assertThat(raw).isEmpty();
        }

        @Test
        void valueContainingSpaces() {
            Map<String, String> raw = new CliArgSource(
                new String[]{"--name", "hello world"}).load();

            assertThat(raw).containsExactly(entry("name", "hello world"));
        }

        @Test
        void negativeNumberValue() {
            Map<String, String> raw = new CliArgSource(
                new String[]{"--count", "-5"}).load();

            assertThat(raw).containsExactly(entry("count", "-5"));
        }
    }

    @Nested
    class TokenisationErrors {

        @Test
        void bareToken() {
            assertThatThrownBy(() ->
                new CliArgSource(new String[]{"count", "5"}).load())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unexpected token");
        }

        @Test
        void missingValue() {
            assertThatThrownBy(() ->
                new CliArgSource(new String[]{"--count"}).load())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing value");
        }

        @Test
        void missingValueWhenNextTokenIsFlag() {
            assertThatThrownBy(() ->
                new CliArgSource(new String[]{"--a", "--b", "2"}).load())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--a");
        }

        @Test
        void emptyParamName() {
            assertThatThrownBy(() ->
                new CliArgSource(new String[]{"--", "val"}).load())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty parameter name");
        }

        @Test
        void duplicateParam() {
            assertThatThrownBy(() ->
                new CliArgSource(new String[]{"--x", "1", "--x", "2"}).load())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");
        }

        @Test
        void collectsAllErrors() {
            assertThatThrownBy(() ->
                new CliArgSource(new String[]{"bare", "--", "val"}).load())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unexpected token")
                .hasMessageContaining("empty parameter name");
        }
    }

    @Nested
    class DefensiveCopy {

        @Test
        void mutatingOriginalArrayDoesNotAffectSource() {
            String[] original = {"--a", "1"};
            CliArgSource source = new CliArgSource(original);

            original[1] = "changed";

            assertThat(source.load()).containsEntry("a", "1");
        }
    }
}
