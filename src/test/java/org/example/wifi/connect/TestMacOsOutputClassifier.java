package org.example.wifi.connect;

import org.example.Platform;
import org.example.engine.Outcome;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Pins how {@code networksetup} output maps to an {@link Outcome}. The error strings are real
 * macOS messages; they are wire literals and must not be "tidied" to follow the code. The sibling
 * {@code TestShellNetworkConnector} checks the same mapping through the connector; this suite owns
 * the classifier in isolation, including the factory dispatch.
 */
class TestMacOsOutputClassifier {

    private final MacOsOutputClassifier classifier = new MacOsOutputClassifier();

    @Nested
    class BaseCases {

        /** networksetup prints nothing on a successful join. */
        @Test
        void treatsBlankOutputAsMatch() {
            assertThat(classifier.classify("")).isInstanceOf(Outcome.Match.class);
        }

        @Test
        void treatsWhitespaceOnlyOutputAsMatch() {
            assertThat(classifier.classify("   \n")).isInstanceOf(Outcome.Match.class);
        }

        @Test
        void treatsNetworkNotVisibleAsSkip() {
            assertThat(classifier.classify("Could not find network MyNet."))
                .isInstanceOf(Outcome.Skip.class);
        }
    }

    @Nested
    class JoinFailures {

        /** Wrong password: a join failure with no apple80211API marker is a definitive mismatch. */
        @Test
        void treatsPlainJoinFailureAsMismatch() {
            assertThat(classifier.classify(
                "Failed to join network MyNet.\nError: -3925 tmpErr"))
                .isInstanceOf(Outcome.Mismatch.class);
        }

        /** Security-type mismatch (error -3912): no password can fix it, so it is a failure. */
        @Test
        void treatsApple80211JoinFailureAsFailure() {
            assertThat(classifier.classify(
                "Failed to join network MyNet.\n(com.apple.wifi.apple80211API.error error -3912.)"))
                .isInstanceOf(Outcome.Failure.class);
        }

        @Test
        void treatsUnrecognisedOutputAsFailure() {
            assertThat(classifier.classify("some unexpected error"))
                .isInstanceOf(Outcome.Failure.class);
        }

        @Test
        void carriesRawOutputInFailureReason() {
            Outcome outcome = classifier.classify("boom");

            assertThat(outcome).isInstanceOfSatisfying(Outcome.Failure.class,
                failure -> assertThat(failure.reason()).isEqualTo("boom"));
        }
    }

    @Nested
    class Factory {

        @Test
        void ofMacOsReturnsMacClassifier() {
            assertThat(OutputClassifier.of(Platform.MACOS)).isInstanceOf(MacOsOutputClassifier.class);
        }
    }
}
