package org.example;

import org.example.matcher.Match;
import org.example.matcher.MatchCoordinator;
import org.example.matcher.MatchOutcome;
import org.example.matcher.MatchPredicate;
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
 * Exercises the mode-2 batch pipeline the way {@code Main} wires it: a networks file and a
 * passwords file on disk are read, then matched by a connector under the {@code effective()}
 * coordinator. The connector is a fake, so no real Wi-Fi is touched and the planted correct pair
 * is the only {@link MatchOutcome.Match}. This is the hermetic counterpart to a manual run_v2.sh
 * run against real hardware, and the automated use of the input-list format documented in
 * {@code input/README.md}.
 */
class TestBatchMatchPipeline {

    @TempDir
    Path dir;

    private Path writeLines(String name, String... lines) throws Exception {
        return Files.writeString(dir.resolve(name), String.join("\n", lines));
    }

    private List<Network> networksFrom(Path file) throws Exception {
        return Files.readAllLines(file).stream().map(Network::of).toList();
    }

    @Test
    void findsPlantedPasswordForTargetNetwork() throws Exception {
        Path networksFile = writeLines("networks.txt", "HomeLab", "GuestAP");
        Path passwordsFile = writeLines("passwords.txt", "wrong1", "correcthorse", "wrong2");
        MatchPredicate<Network, Password> connector = (network, password) ->
            network.ssid().equals("HomeLab") && password.value().equals("correcthorse")
                ? new MatchOutcome.Match()
                : new MatchOutcome.Mismatch();
        List<Match<Network, Password>> matches = new ArrayList<>();

        try (var passwords = Files.lines(passwordsFile)) {
            MatchCoordinator.<Network, Password>effective().match(
                networksFrom(networksFile), passwords.map(Password::new), connector, matches::add);
        }

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().left().ssid()).isEqualTo("HomeLab");
        assertThat(matches.getFirst().right().value()).isEqualTo("correcthorse");
    }

    /** The effective() preset stops trying a network once it matches, so later passwords are untouched. */
    @Test
    void stopsTryingNetworkAfterItMatches() throws Exception {
        Path networksFile = writeLines("networks.txt", "HomeLab");
        Path passwordsFile = writeLines("passwords.txt", "right", "after1", "after2");
        List<String> attempted = new ArrayList<>();
        MatchPredicate<Network, Password> connector = (network, password) -> {
            attempted.add(password.value());
            return password.value().equals("right")
                ? new MatchOutcome.Match()
                : new MatchOutcome.Mismatch();
        };

        try (var passwords = Files.lines(passwordsFile)) {
            MatchCoordinator.<Network, Password>effective().match(
                networksFrom(networksFile), passwords.map(Password::new), connector, m -> {});
        }

        assertThat(attempted).containsExactly("right");
    }

    @Test
    void reportsNoMatchWhenNoPasswordIsCorrect() throws Exception {
        Path networksFile = writeLines("networks.txt", "HomeLab", "GuestAP");
        Path passwordsFile = writeLines("passwords.txt", "nope1", "nope2");
        MatchPredicate<Network, Password> connector = (network, password) -> new MatchOutcome.Mismatch();
        List<Match<Network, Password>> matches = new ArrayList<>();

        try (var passwords = Files.lines(passwordsFile)) {
            MatchCoordinator.<Network, Password>effective().match(
                networksFrom(networksFile), passwords.map(Password::new), connector, matches::add);
        }

        assertThat(matches).isEmpty();
    }
}
