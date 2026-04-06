package org.example.wifi.scan;

import org.example.shell.ShellRunner;
import org.example.wifi.Network;
import java.util.List;

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
