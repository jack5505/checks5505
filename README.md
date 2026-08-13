# checks5505

Annotation-based cross-field business rule validation for Java 21+.

## The problem

Bean Validation answers *"is this field non-null / a valid email?"* — but real backend services are full of rules like:

> *"If the type is TRANSFER, then the amount must be positive AND the recipient must be set AND the date must not be in the past."*

Today that lives in service code: nested `if` forests, an exception thrown on the first failure, error messages that are just strings and cannot be parsed by an API client.

`checks5505` moves those rules onto the class itself, evaluates all of them, and returns **every** failure at once.

## Quick example

```java
@Check(when = "type == 'TRANSFER'", rule = "amount > 0",        message = "amount must be positive")
@Check(when = "type == 'TRANSFER'", rule = "recipient != null", message = "recipient is required")
@Check(when = "type == 'TRANSFER'", rule = "date != null",      message = "date is required")
public record Transfer(BigDecimal amount, String recipient, LocalDate date, String type) {}
```

```java
ValidationResult result = Validator.validate(transfer);

if (!result.isValid()) {
    for (ValidationError error : result.errors()) {
        // error.field()          → "amount"
        // error.message()        → "amount must be positive"
        // error.rejectedValue()  → -100
    }
}
```

All three broken rules are reported in one call — no exceptions, no fail-fast, machine-readable errors ready for a JSON API response.

## Three ways to use it

**1. Conditional rules — the core feature**

```java
@Check(when = "type == 'TRANSFER'", rule = "amount > 0", message = "amount must be positive")
```

The rule only applies when the `when` condition holds. Deposit objects are simply not affected by transfer rules.

**2. Always-on rules**

```java
@Check(rule = "count >= 0", message = "count cannot be negative")
public record Counter(int count) {}
```

Without `when`, the rule always applies.

**3. Fail-fast rule checking at build time**

```java
@Test
void rulesCompile() {
    Validator.compile(Transfer.class);
}
```

A typo in a field name or an unsupported expression fails this test instead of exploding in production. Call `compile` for every class that carries `@Check` rules.

## What v0.1 supports

Expressions are intentionally tiny:

- `field OP constant` — operators: `==`, `!=`, `>`, `<`, `>=`, `<=`
- `field == null` / `field != null`
- `true` / `false`
- constants: numbers (`0`, `3.14`), strings (`'TRANSFER'`), booleans

Composite expressions (`&&`, `||`), arithmetic, and i18n message keys are planned for v0.2 — see [Roadmap](#roadmap).

## How it differs from Bean Validation

- **Cross-field rules** — `when` + `rule` compare fields with constants and each other, on the class level
- **All failures at once** — `ValidationResult` collects everything; no exception on the first error
- **Structured errors** — `field`, `message`, and `rejectedValue` per error, ready to serialize to JSON
- **Compile-time safety net** — `Validator.compile()` turns expression typos into test failures

## Roadmap

- v0.2 — composite expressions (`&&`, `||`), i18n message keys, reflection caching
- v0.3 — Maven Central release, field-path support for nested objects

## Building

```bash
./gradlew test
```

Requires JDK 21+ to run; the build compiles to Java 21 bytecode.

## License

This project is not yet licensed.
