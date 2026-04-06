package org.example.shell;

import org.example.io.Logger;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SystemShellRunner implements ShellRunner {

    private final Logger logger;

    public SystemShellRunner(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String run(String command) throws Exception {
        logger.debug("Executing: " + command);
        Process process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
        StringBuilder stdout = new StringBuilder(), stderr = new StringBuilder();
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
