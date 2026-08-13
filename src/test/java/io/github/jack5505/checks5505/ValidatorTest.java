package io.github.jack5505.checks5505;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorTest {

    @Check(when = "type == 'TRANSFER'", rule = "amount > 0", message = "amount должен быть больше 0")
    @Check(when = "type == 'TRANSFER'", rule = "recipient != null", message = "получатель обязателен")
    @Check(when = "type == 'TRANSFER'", rule = "date != null", message = "дата обязательна")
    record Transfer(BigDecimal amount, String recipient, LocalDate date, String type) {
    }

    @Check(rule = "count >= 0", message = "count не может быть отрицательным")
    record Counter(int count) {
    }

    @Test
    void validTransferPasses() {
        Transfer transfer = new Transfer(new BigDecimal("100"), "recipient", LocalDate.now(), "TRANSFER");

        ValidationResult result = Validator.validate(transfer);

        assertTrue(result.isValid(), "expected no errors, got: " + result.errors());
    }

    @Test
    void invalidTransferCollectsAllErrors() {
        Transfer transfer = new Transfer(new BigDecimal("-100"), null, null, "TRANSFER");

        ValidationResult result = Validator.validate(transfer);

        assertFalse(result.isValid());
        assertEquals(3, result.errors().size(), "all three broken rules must be reported at once");
    }

    @Test
    void ruleDoesNotApplyWhenConditionIsFalse() {
        Transfer transfer = new Transfer(new BigDecimal("-100"), null, null, "DEPOSIT");

        ValidationResult result = Validator.validate(transfer);

        assertTrue(result.isValid(), "TRANSFER-only rules must not apply to DEPOSIT");
    }

    @Test
    void errorContainsFieldMessageAndRejectedValue() {
        Transfer transfer = new Transfer(new BigDecimal("-100"), "recipient", LocalDate.now(), "TRANSFER");

        ValidationResult result = Validator.validate(transfer);

        assertEquals(1, result.errors().size());
        ValidationError error = result.errors().get(0);
        assertEquals("amount", error.field());
        assertEquals("amount должен быть больше 0", error.message());
        assertEquals(new BigDecimal("-100"), error.rejectedValue());
    }

    @Test
    void ruleWithoutWhenAlwaysApplies() {
        ValidationResult negative = Validator.validate(new Counter(-5));
        ValidationResult positive = Validator.validate(new Counter(5));

        assertFalse(negative.isValid());
        assertTrue(positive.isValid());
    }

    @Test
    void compilePassesForValidRules() {
        Validator.compile(Transfer.class);
        Validator.compile(Counter.class);
    }

    @Test
    void compileFailsFastOnUnknownField() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> Validator.compile(BrokenTransfer.class));
        assertTrue(e.getMessage().contains("amountt"), "message should name the typo'd field: " + e.getMessage());
    }

    @Check(rule = "amountt > 0", message = "typo")
    record BrokenTransfer(BigDecimal amount) {
    }

    @Test
    void compileFailsFastOnUnsupportedExpression() {
        assertThrows(IllegalStateException.class, () -> Validator.compile(WeirdRule.class));
    }

    @Check(rule = "amount > 0 && amount < 100", message = "not supported in v0.1")
    record WeirdRule(BigDecimal amount) {
    }

    @Test
    void validateReturnsEmptyErrorListWhenValid() {
        ValidationResult result = Validator.validate(new Counter(1));

        assertEquals(List.of(), result.errors());
    }
}
