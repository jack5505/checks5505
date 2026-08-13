package io.github.jack5505.checks5505;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * JMH benchmarks for the v0.1 hot path: {@link Validator#validate(Object)}.
 *
 * <p>Run with: {@code JAVA_HOME=/opt/homebrew/opt/openjdk ./gradlew jmh}.
 * Override JMH parameters with {@code -PjmhArgs="-f 2 -i 10"}.</p>
 */
@State(Scope.Benchmark)
public class ValidatorBenchmark {

    @Check(when = "type == 'TRANSFER'", rule = "amount > 0", message = "amount must be positive")
    @Check(when = "type == 'TRANSFER'", rule = "recipient != null", message = "recipient is required")
    @Check(when = "type == 'TRANSFER'", rule = "date != null", message = "date is required")
    @Check(rule = "type != null", message = "type is required")
    public record Transfer(BigDecimal amount, String recipient, LocalDate date, String type) {}

    private final Transfer valid =
            new Transfer(new BigDecimal("100"), "alice", LocalDate.of(2026, 8, 13), "TRANSFER");

    private final Transfer invalid =
            new Transfer(new BigDecimal("-5"), null, null, "TRANSFER");

    /** Happy path: all four rules pass. */
    @Benchmark
    public ValidationResult validTransfer() {
        return Validator.validate(valid);
    }

    /** Failure path: three rules fail and every error is collected. */
    @Benchmark
    public ValidationResult invalidTransfer() {
        return Validator.validate(invalid);
    }
}
