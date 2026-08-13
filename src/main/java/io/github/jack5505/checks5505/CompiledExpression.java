package io.github.jack5505.checks5505;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A rule expression parsed once into an immutable, reusable form.
 *
 * <p>Parsing — regex, constant conversion, field resolution — happens at
 * compile time (first {@code validate()} or {@code compile()} call for the
 * class). {@link #evaluate} only reads the field and compares, so the hot
 * path contains no regex, no reflection lookup and no constant parsing.</p>
 */
final class CompiledExpression {

    /** field OP constant — field: identifier, op: comparison, constant: anything non-empty */
    private static final Pattern COMPARISON = Pattern.compile("^(\\w+)\\s*(==|!=|>=|<=|>|<)\\s*(\\S.*)$");

    /** The expression as written in the annotation, used in error messages. */
    private final String source;
    /** Field name from the left-hand side, or {@code null} for literals. */
    private final String fieldName;
    /** Comparison operator, or {@code null} for literals. */
    private final Operator operator;
    /** Parsed constant (Long, BigDecimal, String, Boolean or null), parsed once. */
    private final Object constant;
    /** Resolved field reader, or {@code null} for literals. */
    private final FieldAccessor accessor;
    /** Value for literal expressions {@code "true"}/{@code "false"}, otherwise {@code null}. */
    private final Boolean literal;

    private CompiledExpression(String source, String fieldName, Operator operator,
                               Object constant, FieldAccessor accessor, Boolean literal) {
        this.source = source;
        this.fieldName = fieldName;
        this.operator = operator;
        this.constant = constant;
        this.accessor = accessor;
        this.literal = literal;
    }

    /**
     * Parses {@code expression} and resolves its field accessor against {@code type}.
     *
     * @throws IllegalStateException when the expression is malformed
     *                               or references a missing field
     */
    static CompiledExpression compile(Class<?> type, String expression) {
        String expr = expression == null ? "" : expression.trim();

        if ("true".equals(expr)) {
            return new CompiledExpression(expression, null, null, null, null, Boolean.TRUE);
        }
        if ("false".equals(expr)) {
            return new CompiledExpression(expression, null, null, null, null, Boolean.FALSE);
        }

        Matcher matcher = COMPARISON.matcher(expr);
        if (!matcher.matches()) {
            throw new IllegalStateException("Unsupported expression: '" + expression + "'. " +
                    "v0.1 supports: field OP constant, field == null, field != null, true, false");
        }

        String fieldName = matcher.group(1);
        Operator operator = Operator.parse(matcher.group(2));
        Object constant = parseConstant(matcher.group(3));
        FieldAccessor accessor = FieldAccessor.resolve(type, fieldName);
        return new CompiledExpression(expression, fieldName, operator, constant, accessor, null);
    }

    /**
     * Evaluates the expression against {@code target}. Never throws for
     * business-rule failures — exceptions are only for configuration errors
     * (incomparable types, inaccessible fields).
     */
    boolean evaluate(Object target) {
        if (literal != null) {
            return literal;
        }
        Object actual = accessor.get(target);
        return operator.apply(actual, constant, fieldName, source);
    }

    /** Field name from the expression's left-hand side, or {@code null} for literals. */
    String fieldName() {
        return fieldName;
    }

    /** Accessor used to read the field, or {@code null} for literals. */
    FieldAccessor accessor() {
        return accessor;
    }

    /** Parses the constant on the right-hand side of an expression. */
    private static Object parseConstant(String raw) {
        String value = raw.trim();

        if ("null".equals(value)) {
            return null;
        }
        if ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        }
        if ("true".equals(value) || "false".equals(value)) {
            return Boolean.parseBoolean(value);
        }
        if (value.matches("-?\\d+")) {
            return Long.parseLong(value);
        }
        if (value.matches("-?\\d+\\.\\d+")) {
            return new BigDecimal(value);
        }
        throw new IllegalStateException("Unsupported constant: '" + raw + "'");
    }

    /** Comparison operators between the field value and the parsed constant. */
    enum Operator {
        EQ, NEQ, GT, LT, GTE, LTE;

        static Operator parse(String token) {
            return switch (token) {
                case "==" -> EQ;
                case "!=" -> NEQ;
                case ">" -> GT;
                case "<" -> LT;
                case ">=" -> GTE;
                case "<=" -> LTE;
                default -> throw new IllegalStateException("Unknown operator: '" + token + "'");
            };
        }

        boolean apply(Object actual, Object expected, String fieldName, String expression) {
            return switch (this) {
                case EQ -> Objects.equals(actual, expected);
                case NEQ -> !Objects.equals(actual, expected);
                default -> {
                    int result = compare(actual, expected, fieldName, expression, symbol());
                    yield switch (this) {
                        case GT -> result > 0;
                        case LT -> result < 0;
                        case GTE -> result >= 0;
                        default -> result <= 0;
                    };
                }
            };
        }

        private String symbol() {
            return switch (this) {
                case GT -> ">";
                case LT -> "<";
                case GTE -> ">=";
                default -> "<=";
            };
        }

        /** Compares the field value with the constant, converting the constant to the field's numeric type. */
        @SuppressWarnings({"rawtypes", "unchecked"})
        private static int compare(Object actual, Object expected, String fieldName,
                                   String expression, String symbol) {
            if (!(actual instanceof Comparable)) {
                throw new IllegalStateException("Field '" + fieldName + "' is not comparable " +
                        "but expression '" + expression + "' uses '" + symbol + "'");
            }
            int result;
            try {
                result = ((Comparable) actual).compareTo(convertNumber(expected, actual));
            } catch (ClassCastException e) {
                throw new IllegalStateException("Cannot compare field '" + fieldName + "' of type " +
                        actual.getClass().getSimpleName() + " with constant '" + expected + "'", e);
            }
            return result;
        }

        /**
         * Converts the expected constant to the numeric type of the actual value,
         * so that {@code amount > 0} works for int, long, BigDecimal, double.
         */
        private static Number convertNumber(Object expected, Object actual) {
            if (!(expected instanceof Number number)) {
                return (Number) expected;
            }
            if (actual instanceof BigDecimal) {
                return number instanceof BigDecimal bd ? bd : BigDecimal.valueOf(number.doubleValue());
            }
            if (actual instanceof Double || actual instanceof Float) {
                return number.doubleValue();
            }
            if (actual instanceof Long) {
                return number.longValue();
            }
            if (actual instanceof Integer || actual instanceof Short || actual instanceof Byte) {
                return number.intValue();
            }
            return number;
        }
    }
}
