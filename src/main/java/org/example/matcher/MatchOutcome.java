package org.example.matcher;

/**
 * Typed result of a single attempt in a {@link PairMatcher} run.
 *
 * <p>The four cases cover the full decision space for any credential-spray or matching scenario:
 *
 * <ul>
 *   <li>{@link Match} — the pair satisfied the predicate; a credential was accepted.</li>
 *   <li>{@link Mismatch} — the pair was tested and definitively rejected
 *       (e.g. wrong password — no point trying the same credential again).</li>
 *   <li>{@link Unavailable} — the target could not be reached at the time of the attempt;
 *       the caller may skip all remaining candidates for this target entirely.</li>
 *   <li>{@link Failure} — an unexpected technical error prevented a clean determination;
 *       {@code reason} carries the raw output or exception message for diagnostics.</li>
 * </ul>
 */
public sealed interface MatchOutcome
    permits MatchOutcome.Match,
            MatchOutcome.Mismatch,
            MatchOutcome.Unavailable,
            MatchOutcome.Failure {

    /** The attempt succeeded — the left/right pair is a confirmed match. */
    record Match() implements MatchOutcome {}

    /**
     * The attempt was made but the target definitively rejected the candidate
     * (e.g. wrong password, authentication refused by the network).
     */
    record Mismatch() implements MatchOutcome {}

    /**
     * The target was unreachable or not present at the time of the attempt.
     * The batch loop should skip all remaining candidates for this target rather than
     * continuing to try — the bottleneck is target visibility, not the credential.
     */
    record Unavailable() implements MatchOutcome {}

    /**
     * An unexpected technical error occurred (e.g. security-type mismatch, unrecognised
     * command output, or a shell exception). {@code reason} contains the raw output or
     * the exception message; it is never {@code null} but may be blank.
     */
    record Failure(String reason) implements MatchOutcome {}
}
