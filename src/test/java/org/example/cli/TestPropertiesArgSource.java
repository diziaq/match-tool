package org.example.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.*;

class TestPropertiesArgSource {

    @Test
    void loadsFromPropertiesObject() {
        Properties props = new Properties();
        props.setProperty("count", "42");
        props.setProperty("name", "hello");

        Map<String, String> raw = new PropertiesArgSource(props).load();

        assertThat(raw)
            .containsEntry("count", "42")
            .containsEntry("name", "hello");
    }

    @Test
    void emptyProperties() {
        Map<String, String> raw = new PropertiesArgSource(new Properties()).load();
        assertThat(raw).isEmpty();
    }

    @Test
    void defensiveCopyAtConstruction() {
        Properties props = new Properties();
        props.setProperty("a", "1");

        PropertiesArgSource source = new PropertiesArgSource(props);

        props.setProperty("a", "changed");

        assertThat(source.load()).containsEntry("a", "1");
    }

    @Test
    void loadsFromFile(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("config.properties");
        Files.writeString(file, "count=10\nname=world\n");

        Map<String, String> raw = PropertiesArgSource.fromFile(file).load();

        assertThat(raw)
            .containsEntry("count", "10")
            .containsEntry("name", "world");
    }

    @Test
    void loadsFromFileWithWhitespace(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("config.properties");
        Files.writeString(file, "count = 10\nname = world\n");

        Map<String, String> raw = PropertiesArgSource.fromFile(file).load();

        assertThat(raw)
            .containsEntry("count", "10")
            .containsEntry("name", "world");
    }

    @Test
    void throwsOnMissingFile(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("does-not-exist.properties");

        assertThatThrownBy(() -> PropertiesArgSource.fromFile(missing))
            .isInstanceOf(java.io.IOException.class);
    }
}
