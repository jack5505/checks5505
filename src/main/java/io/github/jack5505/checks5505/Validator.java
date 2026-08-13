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
     * Validates {@code target} against all {@link Check} rules on its class
     * and collects every failure.
     *
     * @throws NullPointerException when {@code target} is {@code null}
     */
    public static ValidationResult validate(Object target) {
        Objects.requireNonNull(target, "target must not be null");

        List<Check> checks = findChecks(target.getClass());
        List<ValidationError> errors = new ArrayList<>();

        for (Check check : checks) {
            boolean condition = ExpressionEvaluator.evaluate(target, check.when());
            if (!condition) {
                continue;
            }
            boolean rulePassed = ExpressionEvaluator.evaluate(target, check.rule());
            if (!rulePassed) {
                String field = ExpressionEvaluator.fieldNameOf(check.rule());
                Object rejectedValue = field == null ? null : ExpressionEvaluator.readField(target, field);
                errors.add(new ValidationError(field, check.message(), rejectedValue));
            }
        }
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
        for (Check check : findChecks(type)) {
            ExpressionEvaluator.verify(type, check.when());
            ExpressionEvaluator.verify(type, check.rule());
        }
    }

    /** Collects repeatable {@link Check} annotations (single or contained in {@link Check.List}). */
    private static List<Check> findChecks(Class<?> type) {
        List<Check> checks = new ArrayList<>();

        Check single = type.getAnnotation(Check.class);
        if (single != null) {
            checks.add(single);
        }
        Check.List container = type.getAnnotation(Check.List.class);
        if (container != null) {
            checks.addAll(List.of(container.value()));
        }
        return checks;
    }
}
