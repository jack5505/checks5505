package io.github.jack5505.checks5505;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    @Test
    void validateReadsPrivateFieldFromPlainClass() {
        assertTrue(Validator.validate(new Account(42)).isValid());
        assertFalse(Validator.validate(new Account(-1)).isValid());
    }

    @Check(rule = "balance >= 0", message = "balance не может быть отрицательным")
    static final class Account {
        private final int balance;

        Account(int balance) {
            this.balance = balance;
        }
    }

    @Test
    void validateUsesGetterWhenFieldNameMatchesOnlyMethod() {
        assertTrue(Validator.validate(new Wallet(100)).isValid());
        assertFalse(Validator.validate(new Wallet(-1)).isValid());
    }

    @Check(rule = "total >= 0", message = "total не может быть отрицательным")
    static final class Wallet {
        private final int internalTotal;

        Wallet(int internalTotal) {
            this.internalTotal = internalTotal;
        }

        public int total() {
            return internalTotal;
        }
    }

    @Test
    void validateIsSafeUnderConcurrentUse() throws Exception {
        Transfer valid = new Transfer(new BigDecimal("100"), "recipient", LocalDate.now(), "TRANSFER");
        int threads = 8;
        int iterations = 5_000;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(pool.submit(() -> {
                    for (int i = 0; i < iterations; i++) {
                        if (!Validator.validate(valid).isValid()) {
                            return false;
                        }
                    }
                    return true;
                }));
            }
            for (Future<Boolean> future : futures) {
                assertTrue(future.get(), "concurrent validation must always pass on a valid object");
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
