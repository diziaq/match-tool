package org.example.shell;

import org.example.io.Logger;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * {@link ShellRunner} that delegates to {@code /bin/sh -c}. Stdout and stderr streams are drained
 * concurrently to avoid blocking on full pipe buffers. Stderr is forwarded to
 * {@link Logger#error}; the return value contains only stdout. Debug logging records the command,
 * exit code, and stdout character count.
 */
public class SystemShellRunner implements ShellRunner {

    private final Logger logger;

    public SystemShellRunner(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String run(String command) throws Exception {
        logger.debug("Executing: " + command);
        var process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
        var stdout = new StringBuilder();
        var stderr = new StringBuilder();
        try (var br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) stdout.append(line).append("\n");
        }
        try (var br = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = br.readLine()) != null) stderr.append(line).append("\n");
        }
        int exitCode = process.waitFor();
        logger.debug("Exit code: " + exitCode);
        logger.debug("Stdout (" + stdout.length() + " chars): " + stdout.toString().replace('\n', '|'));
        if (!stderr.toString().isBlank()) {
            logger.error("STDERR: " + stderr.toString().trim());
        }
        return stdout.toString();
    }
}
