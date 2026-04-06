package org.example.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * Immutable specification for a single command-line parameter.
 * Uses a fluent style for construction:
 *
 * <pre>{@code
 * ParamSpec.of(ParamType.PATH)
 *          .required()
 *          .validatedBy(ParamSpec.existingNonEmptyFile(), "must be an existing non-empty file")
 * }</pre>
 */
public final class ParamSpec<T> {

    private final ParamType<T> type;
    private final boolean required;
    private final Predicate<T> validator;
    private final String validationMessage;

    private ParamSpec(ParamType<T> type, boolean required, Predicate<T> validator, String validationMessage) {
        this.type = type;
        this.required = required;
        this.validator = validator;
        this.validationMessage = validationMessage;
    }

    public static <T> ParamSpec<T> of(ParamType<T> type) {
        return new ParamSpec<>(type, false, null, null);
    }

    public ParamSpec<T> required() {
        return new ParamSpec<>(type, true, validator, validationMessage);
    }

    public ParamSpec<T> validatedBy(Predicate<T> validator, String message) {
        return new ParamSpec<>(type, required, validator, message);
    }

    /** Reusable validator: Path must point to an existing, non-empty regular file. */
    public static Predicate<Path> existingNonEmptyFile() {
        return path -> {
            try {
                return Files.isRegularFile(path) && Files.size(path) > 0;
            } catch (IOException e) {
                return false;
            }
        };
    }

    ParamType<T> type()              { return type; }
    boolean isRequired()             { return required; }
    Predicate<T> validator()         { return validator; }
    String validationMessage()       { return validationMessage; }
}
