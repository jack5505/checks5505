package io.github.jack5505.checks5505;

import java.util.List;

/**
 * The outcome of validating one object: a list of all failures found.
 *
 * <p>The contract of this library: validation never throws for business-rule
 * failures — it collects them all and returns them. An empty list means
 * the object is valid.</p>
 *
 * @param errors all validation errors found, never {@code null}
 */
public record ValidationResult(List<ValidationError> errors) {

    public ValidationResult {
        errors = List.copyOf(errors);
    }

    /** @return {@code true} when no rule failed */
    public boolean isValid() {
        return errors.isEmpty();
    }

    /** @return a valid (empty) result */
    public static ValidationResult valid() {
        return new ValidationResult(List.of());
    }
}
