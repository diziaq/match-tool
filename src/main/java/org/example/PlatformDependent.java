package org.example;

/**
 * Marks a type as having OS-specific behaviour resolved through {@link Platform}.
 *
 * <p>Every implementing interface must declare a static factory method with the signature:
 * <pre>{@code
 *     static T of(Platform platform)
 * }</pre>
 * This method is invoked reflectively by {@link Platform#resolve(Class)}.
 */
public interface PlatformDependent {}
