package io.github.jack5505.checks5505;

/**
 * A single validation failure.
 *
 * @param field         the field the rule failed on, e.g. {@code "amount"}
 * @param message       human-readable message from the {@link Check} annotation
 * @param rejectedValue the actual value that did not pass the rule, may be {@code null}
 */
public record ValidationError(String field, String message, Object rejectedValue) {
}
