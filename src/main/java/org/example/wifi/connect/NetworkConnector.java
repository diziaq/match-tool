package org.example.wifi.connect;

import org.example.matcher.MatchOutcome;
import org.example.matcher.MatchPredicate;
import org.example.wifi.Network;

/**
 * Attempts to connect to a WiFi network and returns a typed {@link MatchOutcome}.
 *
 * <p>Extends {@link MatchPredicate}{@code <Network, String>} so that a connector instance can be
 * passed directly as the predicate in a {@link org.example.matcher.PairMatcher} run, without any
 * adapter lambda:
 *
 * <pre>{@code
 * new PairMatcher<Network, String>().match(networks, passwords, connector, onMatch);
 * }</pre>
 *
 * <p>Implementations must never throw; all failures must be returned as
 * {@link MatchOutcome.Failure}.
 */
public interface NetworkConnector extends MatchPredicate<Network, String> {

    /**
     * Attempts to join {@code network} using {@code password}.
     *
     * @param network  the target network (SSID taken from {@link Network#ssid()})
     * @param password the WPA/WPA2 passphrase (may be empty for open networks)
     * @return a non-null {@link MatchOutcome}
     */
    @Override
    MatchOutcome test(Network network, String password);
}
