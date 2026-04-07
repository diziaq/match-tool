package org.example.wifi.scan;

import org.example.wifi.Network;
import java.util.List;

/**
 * Discovers currently visible WiFi networks and returns them sorted and deduplicated.
 *
 * <p>Results are sorted case-insensitively by SSID. When the same SSID appears multiple times
 * (e.g. from multiple access points), only the entry with the strongest signal is kept.
 */
public interface NetworkScanner {
    /**
     * @return an immutable-ordered list of visible networks; never {@code null}
     * @throws Exception if the underlying shell command fails to execute
     */
    List<Network> scan() throws Exception;
}
