package org.example.matcher;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PairMatcherTest {

    private final PairMatcher<String, String> matcher = new PairMatcher<>();

    @Test
    void callsOnMatchForMatchingPair() {
        List<Match<String, String>> matched = new ArrayList<>();

        matcher.match(
            List.of("a"),
            Stream.of("1"),
            (l, r) -> l.equals("a") && r.equals("1"),
            matched::add
        );

        assertEquals(1, matched.size());
        assertEquals("a", matched.get(0).left());
        assertEquals("1", matched.get(0).right());
    }

    @Test
    void doesNotCallOnMatchWhenPredicateFalse() {
        List<Match<String, String>> matched = new ArrayList<>();

        matcher.match(
            List.of("a"),
            Stream.of("x"),
            (l, r) -> false,
            matched::add
        );

        assertTrue(matched.isEmpty());
    }

    @Test
    void crossesAllLeftsWithAllRights() {
        List<String> attempted = new ArrayList<>();

        matcher.match(
            List.of("L1", "L2"),
            Stream.of("R1", "R2", "R3"),
            (l, r) -> { attempted.add(l + "+" + r); return false; },
            m -> {}
        );

        assertEquals(6, attempted.size());
        assertTrue(attempted.contains("L1+R1"));
        assertTrue(attempted.contains("L1+R2"));
        assertTrue(attempted.contains("L1+R3"));
        assertTrue(attempted.contains("L2+R1"));
        assertTrue(attempted.contains("L2+R2"));
        assertTrue(attempted.contains("L2+R3"));
    }

    @Test
    void iteratesRightsOuterLoopLeftsInner() {
        List<String> order = new ArrayList<>();

        matcher.match(
            List.of("L1", "L2"),
            Stream.of("R1", "R2"),
            (l, r) -> { order.add(r + "-" + l); return false; },
            m -> {}
        );

        assertEquals(List.of("R1-L1", "R1-L2", "R2-L1", "R2-L2"), order);
    }

    @Test
    void emptyLefts_noMatchesAttempted() {
        List<Match<String, String>> matched = new ArrayList<>();

        matcher.match(List.of(), Stream.of("R1", "R2"), (l, r) -> true, matched::add);

        assertTrue(matched.isEmpty());
    }

    @Test
    void emptyRights_noMatchesAttempted() {
        List<Match<String, String>> matched = new ArrayList<>();

        matcher.match(List.of("L1"), Stream.empty(), (l, r) -> true, matched::add);

        assertTrue(matched.isEmpty());
    }

    @Test
    void multipleMatchesAllReported() {
        List<Match<String, String>> matched = new ArrayList<>();

        matcher.match(
            List.of("a", "b"),
            Stream.of("1", "2"),
            (l, r) -> true,
            matched::add
        );

        assertEquals(4, matched.size());
    }

    @Test
    void worksWithIntegerTypes() {
        List<Match<Integer, Integer>> matched = new ArrayList<>();
        PairMatcher<Integer, Integer> intMatcher = new PairMatcher<>();

        intMatcher.match(
            List.of(2, 3),
            Stream.of(4, 6, 9),
            (l, r) -> r % l == 0,
            matched::add
        );

        assertEquals(3, matched.size());
    }

    @Test
    void onMatchReceivesCorrectPair() {
        List<Match<String, String>> matched = new ArrayList<>();

        matcher.match(
            List.of("net"),
            Stream.of("pass"),
            (l, r) -> true,
            matched::add
        );

        assertEquals(new Match<>("net", "pass"), matched.get(0));
    }
}
