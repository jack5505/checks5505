package io.github.jack5505.checks5505;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares one cross-field business rule on a class.
 *
 * <p>Example:</p>
 * <pre>{@code
 * @Check(when = "type == 'TRANSFER'", rule = "amount > 0", message = "amount должен быть больше 0")
 * @Check(rule = "recipient != null", message = "получатель обязателен")
 * public record Transfer(BigDecimal amount, String recipient, String type) {}
 * }</pre>
 *
 * <p>The rule is only evaluated when the {@code when} condition holds.
 * The field name is taken from the left-hand side of the {@code rule} expression.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Check.List.class)
public @interface Check {

    /**
     * Condition that must hold for the rule to be evaluated.
     * {@code "true"} (the default) means the rule always applies.
     */
    String when() default "true";

    /**
     * The rule expression, e.g. {@code "amount > 0"} or {@code "recipient != null"}.
     */
    String rule();

    /**
     * Human-readable message attached to the {@link ValidationError}
     * when the rule fails.
     */
    String message();

    /**
     * Container annotation required by Java for repeatable annotations.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        Check[] value();
    }
}
