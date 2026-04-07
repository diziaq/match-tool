package org.example.wifi.scan;

import org.example.shell.ShellRunner;
import org.example.wifi.Network;
import java.util.List;

/**
 * {@link NetworkScanner} implementation for Linux using {@code nmcli}.
 *
 * <p>Runs {@code nmcli -t -f SSID,SIGNAL dev wifi list}, which outputs colon-delimited
 * {@code SSID:signal} lines in terse mode. The output is then parsed and deduplicated by
 * {@link NetworkOutputParser}.
 */
public class LinuxNetworkScanner implements NetworkScanner {

    private final ShellRunner shell;

    public LinuxNetworkScanner(ShellRunner shell) {
        this.shell = shell;
    }

    @Override
    public List<Network> scan() throws Exception {
        String output = shell.run("nmcli -t -f SSID,SIGNAL dev wifi list");
        return NetworkOutputParser.deduplicated(NetworkOutputParser.parseLinux(output));
    }
}
