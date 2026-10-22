package org.example.wifi.connect;

import org.example.engine.Outcome;
import org.example.engine.PairEvaluator;
import org.example.wifi.Network;
import org.example.wifi.Password;

/**
 * Attempts to connect to a WiFi network and returns a typed {@link Outcome}.
 *
 * <p>Extends {@link PairEvaluator}{@code <Network, Password>} so that a connector instance can be
 * passed directly as the evaluator in a {@link org.example.engine.PairingEngine} run:
 *
 * <pre>{@code
 * engine.run(networks, passwords, connector, onMatch);
 * }</pre>
 *
 * <p>Implementations must never throw; all failures must be returned as {@link Outcome.Failure}.
 */
public interface NetworkConnector extends PairEvaluator<Network, Password> {

    /**
     * @param network  the target network (SSID taken from {@link Network#ssid()})
     * @param password the WPA/WPA2 passphrase
     * @return a non-null {@link Outcome}
     */
    @Override
    Outcome evaluate(Network network, Password password);
}
