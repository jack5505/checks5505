package io.github.jack5505.checks5505;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entry point of the library: validates objects against {@link Check} rules
 * declared on their classes.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * ValidationResult result = Validator.validate(transfer);
 * if (!result.isValid()) { ... }
 * }</pre>
 *
 * <p>Validation never throws for business-rule failures — all failures are
 * collected into the returned {@link ValidationResult}. Exceptions are only
 * thrown for configuration errors (malformed expressions, missing fields).</p>
 */
public final class Validator {

    private Validator() {
    }

    /**
     * Compiled rules per class. {@link ClassValue} is the right cache here:
     * the JVM manages entries and drops them together with the class, so a
     * long-running application with hot class reloading cannot leak memory.
     */
    private static final ClassValue<CompiledChecks> CACHE = new ClassValue<>() {
        @Override
        protected CompiledChecks computeValue(Class<?> type) {
            return CompiledChecks.compile(type);
        }
    };

    /**
     * Validates {@code target} against all {@link Check} rules on its class
     * and collects every failure.
     *
     * @throws NullPointerException when {@code target} is {@code null}
     */
    public static ValidationResult validate(Object target) {
        Objects.requireNonNull(target, "target must not be null");

        List<ValidationError> errors = new ArrayList<>();
        CACHE.get(target.getClass()).validate(target, errors);
        return new ValidationResult(errors);
    }

    /**
     * Verifies that every expression in every {@link Check} on {@code type}
     * is well-formed and references existing fields. Call this in a unit test:
     *
     * <pre>{@code
     * @Test
     * void rulesCompile() { Validator.compile(Transfer.class); }
     * }</pre>
     *
     * so a typo in a rule fails the build instead of exploding in production.
     *
     * @throws IllegalStateException when any expression is malformed
     *                               or references a missing field
     */
    public static void compile(Class<?> type) {
        Objects.requireNonNull(type, "type must not be null");
        // Compiling every rule eagerly: malformed expressions and missing
        // fields surface here, not on a validation call.
        CACHE.get(type);
    }
}
