package org.example.wifi.scan;

import org.example.shell.ShellRunner;
import org.example.wifi.Network;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

public class MacOsNetworkScanner implements NetworkScanner {

    private final ShellRunner shell;

    public MacOsNetworkScanner(ShellRunner shell) {
        this.shell = shell;
    }

    @Override
    public List<Network> scan() throws Exception {
        File tmp = File.createTempFile("wifi_scan", ".swift");
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
