# checks5505

[![Maven Central](https://img.shields.io/maven-central/v/io.github.jack5505/checks5505)](https://central.sonatype.com/artifact/io.github.jack5505/checks5505)
[![CI](https://github.com/jack5505/checks5505/actions/workflows/ci.yml/badge.svg)](https://github.com/jack5505/checks5505/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21%2B-blue)](https://adoptium.net)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Annotation-based cross-field business rule validation for Java 21+.

## Installation

The library is published on Maven Central. Requires Java 21+.

**Gradle (Kotlin DSL):**

```kotlin
implementation("io.github.jack5505:checks5505:0.2.0")
```

**Gradle (Groovy DSL):**

```groovy
implementation 'io.github.jack5505:checks5505:0.2.0'
```

**Maven:**

```xml
<dependency>
  <groupId>io.github.jack5505</groupId>
  <artifactId>checks5505</artifactId>
  <version>0.2.0</version>
</dependency>
```

No extra repositories are needed. Then annotate your class with `@Check` and call `Validator.validate(...)` — see [Quick example](#quick-example).

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

## Supported expressions

Expressions are intentionally tiny:

- `field OP constant` — operators: `==`, `!=`, `>`, `<`, `>=`, `<=`
- `field == null` / `field != null`
- `true` / `false`
- constants: numbers (`0`, `3.14`), strings (`'TRANSFER'`), booleans

Composite expressions (`&&`, `||`), arithmetic, and i18n message keys are planned for v0.3 — see [Roadmap](#roadmap).

## How it differs from Bean Validation

- **Cross-field rules** — `when` + `rule` compare fields with constants and each other, on the class level
- **All failures at once** — `ValidationResult` collects everything; no exception on the first error
- **Structured errors** — `field`, `message`, and `rejectedValue` per error, ready to serialize to JSON
- **Compile-time safety net** — `Validator.compile()` turns expression typos into test failures

## Performance

Since v0.2 the rules of a class are compiled once and cached (via `ClassValue`),
so validation no longer parses expressions or resolves fields reflectively on
every call.

JMH throughput (OpenJDK 26, Apple Silicon):

- valid object, 4 rules: 0.83M → 20.6M validations/s (≈25×)
- invalid object, 3 failures: 0.53M → 6.9M validations/s (≈13×)

Run the benchmark yourself:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk ./gradlew jmh
```

Raw baseline numbers live in `benchmarks/baseline-v0.1.txt`.

We also benchmarked a MethodHandle-based implementation of field access:
it added only ~2% on the happy path, so we kept the simpler reflective access.

## Roadmap

- v0.2 — performance: rules compiled once per class and cached (see [Performance](#performance))
- v0.3 — composite expressions (`&&`, `||`), i18n message keys
- v0.4 — field-path support for nested objects

## Building

```bash
./gradlew test
```

Requires JDK 21+ to run; the build compiles to Java 21 bytecode.

## License

[MIT](LICENSE)
