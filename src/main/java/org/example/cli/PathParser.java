package org.example.cli;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

class PathParser implements Function<String, Path> {

    @Override
    public Path apply(String raw) {
        var path = Path.of(raw);

        if (!path.isAbsolute()) {
            path = jarDir().resolve(raw);
        }

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File not found: " + path);
        }

        return path;
    }

    private static Path jarDir() {
        try {
            var jar = Path.of(PathParser.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return jar.getParent();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot determine jar location", e);
        }
    }
}
