package org.example.wifi.scan;

import org.example.shell.ShellRunner;
import org.example.wifi.Network;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

/**
 * {@link NetworkScanner} implementation for macOS using a bundled Swift script.
 *
 * <p>The {@code wifi_scan.swift} resource is extracted to a temp file at scan time and executed
 * via Swift. The script outputs pipe-delimited {@code SSID|signal} lines, which are then parsed
 * and deduplicated by {@link NetworkOutputParser}. The temp file is registered for deletion on
 * JVM exit via {@link java.io.File#deleteOnExit()}.
 */
public class MacOsNetworkScanner implements NetworkScanner {

    private final ShellRunner shell;

    public MacOsNetworkScanner(ShellRunner shell) {
        this.shell = shell;
    }

    @Override
    public List<Network> scan() throws Exception {
        var tmp = File.createTempFile("wifi_scan", ".swift");
        tmp.deleteOnExit();
        try (var in = MacOsNetworkScanner.class.getResourceAsStream("/wifi_scan.swift");
             var out = new FileOutputStream(tmp)) {
            if (in == null) throw new FileNotFoundException("wifi_scan.swift not found in resources");
            in.transferTo(out);
        }
        String output = shell.run("swift " + tmp.getAbsolutePath());
        return NetworkOutputParser.deduplicated(NetworkOutputParser.parseMacOs(output));
    }
}
