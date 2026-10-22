package org.example.wifi.connect;

import org.example.Platform;
import org.example.matcher.MatchOutcome;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Pins how {@code nmcli} output maps to a {@link MatchOutcome}. nmcli does not expose granular
 * error codes, so the only distinction is the word "successfully" anywhere in the output; every
 * other result is a failure, and Unavailable is never produced on Linux.
 */
class TestLinuxOutputClassifier {

    private final LinuxOutputClassifier classifier = new LinuxOutputClassifier();

    @Nested
    class BaseCases {

        @Test
        void treatsSuccessfullyOutputAsMatch() {
            assertThat(classifier.classify("Device 'wlan0' successfully connected."))
                .isInstanceOf(MatchOutcome.Match.class);
        }

        @ParameterizedTest(name = "\"{0}\" is a match")
        @ValueSource(strings = {"successfully", "SUCCESSFULLY", "Successfully", "sUcCeSsFuLlY"})
        void matchesSuccessfullyCaseInsensitively(String output) {
            assertThat(classifier.classify(output)).isInstanceOf(MatchOutcome.Match.class);
        }
    }

    @Nested
    class Failures {

        @Test
        void treatsOtherOutputAsFailure() {
            assertThat(classifier.classify("Error: Connection activation failed."))
                .isInstanceOf(MatchOutcome.Failure.class);
        }

        @Test
        void treatsBlankOutputAsFailure() {
            assertThat(classifier.classify("")).isInstanceOf(MatchOutcome.Failure.class);
        }

        @Test
        void carriesRawOutputInFailureReason() {
            MatchOutcome outcome = classifier.classify("activation failed");

            assertThat(outcome).isInstanceOfSatisfying(MatchOutcome.Failure.class,
                failure -> assertThat(failure.reason()).isEqualTo("activation failed"));
        }
    }

    @Nested
    class Factory {

        @Test
        void ofLinuxReturnsLinuxClassifier() {
            assertThat(OutputClassifier.of(Platform.LINUX)).isInstanceOf(LinuxOutputClassifier.class);
        }
    }
}
