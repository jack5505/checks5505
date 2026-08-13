package io.github.jack5505.checks5505;

import java.util.ArrayList;
import java.util.List;

/**
 * All compiled rules of one class, immutable. Built once per class by
 * {@link Validator}'s {@link ClassValue} cache, then reused for every
 * validation call.
 */
final class CompiledChecks {

    private final List<CompiledCheck> checks;

    private CompiledChecks(List<CompiledCheck> checks) {
        this.checks = checks;
    }

    /** Parses every {@link Check} on {@code type} into its compiled form. */
    static CompiledChecks compile(Class<?> type) {
        List<CompiledCheck> compiled = new ArrayList<>();
        for (Check check : findChecks(type)) {
            CompiledExpression when = CompiledExpression.compile(type, check.when());
            CompiledExpression rule = CompiledExpression.compile(type, check.rule());
            compiled.add(new CompiledCheck(when, rule, check.message()));
        }
        return new CompiledChecks(List.copyOf(compiled));
    }

    /** Evaluates every rule and collects all failures into {@code errors}. */
    void validate(Object target, List<ValidationError> errors) {
        for (CompiledCheck check : checks) {
            check.validate(target, errors);
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

    /** One compiled rule: a when-condition, a rule expression and a message. */
    private static final class CompiledCheck {
        private final CompiledExpression when;
        private final CompiledExpression rule;
        private final String message;

        private CompiledCheck(CompiledExpression when, CompiledExpression rule, String message) {
            this.when = when;
            this.rule = rule;
            this.message = message;
        }

        private void validate(Object target, List<ValidationError> errors) {
            if (!when.evaluate(target)) {
                return;
            }
            if (rule.evaluate(target)) {
                return;
            }
            String field = rule.fieldName();
            Object rejectedValue = field == null ? null : rule.accessor().get(target);
            errors.add(new ValidationError(field, message, rejectedValue));
        }
    }
}
