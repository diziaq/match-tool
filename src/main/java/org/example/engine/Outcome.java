package org.example.engine;

/**
 * Typed result of evaluating one pair in a {@link PairingEngine} run.
 *
 * <p>The four cases cover the decision space for any pairing scenario and drive the engine's
 * inner-loop policies:
 *
 * <ul>
 *   <li>{@link Match} — the pair is accepted.</li>
 *   <li>{@link Mismatch} — the pair was evaluated and definitively rejected; try the next candidate.</li>
 *   <li>{@link Skip} — the current outer item is a dead end; the engine may drop its remaining
 *       candidates and advance to the next outer item. In the WiFi plugin this is a network that is
 *       not visible: no password will help.</li>
 *   <li>{@link Failure} — an unexpected technical error prevented a clean verdict; {@code reason}
 *       carries the raw output or exception message, and the engine may retry.</li>
 * </ul>
 */
public sealed interface Outcome
    permits Outcome.Match, Outcome.Mismatch, Outcome.Skip, Outcome.Failure {

    /** The pair is accepted. */
    record Match() implements Outcome {}

    /** The pair was evaluated and definitively rejected. */
    record Mismatch() implements Outcome {}

    /**
     * The current outer item cannot be paired with anything right now; drop its remaining
     * candidates. {@code reason} is never {@code null} but may be blank.
     */
    record Skip(String reason) implements Outcome {}

    /**
     * A technical error prevented a verdict; the engine may retry. {@code reason} is never
     * {@code null} but may be blank.
     */
    record Failure(String reason) implements Outcome {}
}
