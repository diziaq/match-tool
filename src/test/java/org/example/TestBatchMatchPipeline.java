package org.example;

import org.example.engine.Outcome;
import org.example.engine.Pair;
import org.example.engine.PairEvaluator;
import org.example.engine.PairingEngine;
import org.example.engine.Source;
import org.example.wifi.LinesFileSource;
import org.example.wifi.Network;
import org.example.wifi.Password;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Exercises the WiFi batch pipeline the way {@code WifiPlugin} wires it: an SSID file and a password
 * file on disk are read through {@link LinesFileSource} and matched by the {@link PairingEngine} with
 * the plugin's policy set. The evaluator is a fake, so no real Wi-Fi is touched and the planted
 * correct pair is the only {@link Outcome.Match}. This is the hermetic counterpart to a manual
 * run_v2.sh run against real hardware.
 */
class TestBatchMatchPipeline {

    @TempDir
    Path dir;

    private Path writeLines(String name, String... lines) throws Exception {
        return Files.writeString(dir.resolve(name), String.join("\n", lines));
    }

    private Source<Network> networks(Path file) {
        return new LinesFileSource(file).map(Network::of);
    }

    private Source<Password> passwords(Path file) {
        return new LinesFileSource(file).map(Password::new);
    }

    private PairingEngine<Network, Password> wifiEngine() {
        return PairingEngine.<Network, Password>builder()
            .budget(1)
            .maxMatchesPerOuter(1)
            .breakInnerOnSkip()
            .breakInnerAfterFailures(3)
            .build();
    }

    @Test
    void findsPlantedPasswordForTargetNetwork() throws Exception {
        Path networksFile = writeLines("networks.txt", "HomeLab", "GuestAP");
        Path passwordsFile = writeLines("passwords.txt", "wrong1", "correcthorse", "wrong2");
        PairEvaluator<Network, Password> connector = (network, password) ->
            network.ssid().equals("HomeLab") && password.value().equals("correcthorse")
                ? new Outcome.Match()
                : new Outcome.Mismatch();
        List<Pair<Network, Password>> matches = new ArrayList<>();

        wifiEngine().run(networks(networksFile), passwords(passwordsFile), connector, matches::add);

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().a().ssid()).isEqualTo("HomeLab");
        assertThat(matches.getFirst().b().value()).isEqualTo("correcthorse");
    }

    /** maxMatchesPerOuter(1) stops trying passwords for a network once it connects. */
    @Test
    void stopsTryingNetworkAfterItMatches() throws Exception {
        Path networksFile = writeLines("networks.txt", "HomeLab");
        Path passwordsFile = writeLines("passwords.txt", "right", "after1", "after2");
        List<String> attempted = new ArrayList<>();
        PairEvaluator<Network, Password> connector = (network, password) -> {
            attempted.add(password.value());
            return password.value().equals("right") ? new Outcome.Match() : new Outcome.Mismatch();
        };

        wifiEngine().run(networks(networksFile), passwords(passwordsFile), connector, m -> {});

        assertThat(attempted).containsExactly("right");
    }

    @Test
    void reportsNoMatchWhenNoPasswordIsCorrect() throws Exception {
        Path networksFile = writeLines("networks.txt", "HomeLab", "GuestAP");
        Path passwordsFile = writeLines("passwords.txt", "nope1", "nope2");
        PairEvaluator<Network, Password> connector = (network, password) -> new Outcome.Mismatch();
        List<Pair<Network, Password>> matches = new ArrayList<>();

        wifiEngine().run(networks(networksFile), passwords(passwordsFile), connector, matches::add);

        assertThat(matches).isEmpty();
    }
}
