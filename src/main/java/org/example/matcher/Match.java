package org.example.matcher;

public record Match<L, R>(L left, R right) {
    @Override
    public String toString() {
        return left + " <-> " + right;
    }
}
