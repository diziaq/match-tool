package org.example.shell;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Consumer;

public class SystemShellRunner implements ShellRunner {

    private final Consumer<String> debug;

    public SystemShellRunner(Consumer<String> debug) {
        this.debug = debug;
    }

    @Override
    public String run(String command) throws Exception {
        debug.accept("Executing: " + command);
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
        debug.accept("Exit code: " + exitCode);
        debug.accept("Stdout (" + stdout.length() + " chars): " + stdout.toString().replace('\n', '|'));
        if (!stderr.toString().isBlank()) {
            debug.accept("Stderr: " + stderr);
            System.err.println("STDERR: " + stderr);
        }
        return stdout.toString();
    }
}
