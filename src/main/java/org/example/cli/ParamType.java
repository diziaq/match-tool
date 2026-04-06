package org.example.cli;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * Describes how to convert a raw string argument to a typed value.
 * New types can be added by instantiating this class with a custom converter.
 *
 * <pre>{@code
 * ParamType<String> STRING = new ParamType<>("String", Function.identity());
 * }</pre>
 */
public final class ParamType<T> {

    public static final ParamType<Path>    PATH    = new ParamType<>("Path", new PathParser());
    public static final ParamType<Integer> INTEGER = new ParamType<>("Integer", new IntegerParser());

    private final String name;
    private final Function<String, T> converter;

    public ParamType(String name, Function<String, T> converter) {
        this.name = name;
        this.converter = converter;
    }

    public T convert(String raw) {
        return converter.apply(raw);
    }

    public String name() {
        return name;
    }
}
