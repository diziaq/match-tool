package org.example.wifi.scan;

import org.example.wifi.Network;
import java.util.List;

public interface NetworkScanner {
    List<Network> scan() throws Exception;
}
