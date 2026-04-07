package org.example.wifi.connect;

import org.example.matcher.MatchOutcome;
import org.example.matcher.MatchPredicate;
import org.example.wifi.Network;
import org.example.wifi.Password;

/**
 * Attempts to connect to a WiFi network and returns a typed {@link MatchOutcome}.
 *
 * <p>Extends {@link MatchPredicate}{@code <Network, Password>} so that a connector instance can
 * be passed directly as the predicate in a {@link org.example.matcher.PairMatcher} run:
 *
 * <pre>{@code
 * new PairMatcher<Network, Password>().match(networks, passwords, connector, onMatch);
 * }</pre>
 *
 * <p>Implementations must never throw; all failures must be returned as
 * {@link MatchOutcome.Failure}.
 */
public interface NetworkConnector extends MatchPredicate<Network, Password> {

    /**
     * @param network  the target network (SSID taken from {@link Network#ssid()})
     * @param password the WPA/WPA2 passphrase
     * @return a non-null {@link MatchOutcome}
     */
    @Override
    MatchOutcome test(Network network, Password password);
}
