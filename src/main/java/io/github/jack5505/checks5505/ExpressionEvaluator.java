package io.github.jack5505.checks5505;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates tiny rule expressions against a Java object.
 *
 * <p>v0.1 supports exactly three forms:</p>
 * <ul>
 *   <li>{@code "field OP constant"} — e.g. {@code "amount > 0"}, {@code "type == 'TRANSFER'"},
 *       operators: {@code == != > < >= <=}</li>
 *   <li>{@code "field != null"} / {@code "field == null"}</li>
 *   <li>{@code "true"} / {@code "false"}</li>
 * </ul>
 *
 * <p>Any expression that does not match these forms, or references a field
 * that does not exist, throws {@link IllegalStateException} with a readable
 * message. This is intentional: rules are parsed eagerly, so a typo in an
 * expression fails fast instead of silently at runtime.</p>
 */
final class ExpressionEvaluator {

    /** field OP constant — field: identifier, op: comparison, constant: anything non-empty */
    private static final Pattern COMPARISON = Pattern.compile("^(\\w+)\\s*(==|!=|>=|<=|>|<)\\s*(\\S.*)$");

    private ExpressionEvaluator() {
    }

    /**
     * Evaluates {@code expression} against {@code target}.
     *
     * @throws IllegalStateException when the expression is malformed
     *                               or references a missing field
     */
    static boolean evaluate(Object target, String expression) {
        String expr = expression == null ? "" : expression.trim();

        if ("true".equals(expr)) {
            return true;
        }
        if ("false".equals(expr)) {
            return false;
        }

        Matcher matcher = COMPARISON.matcher(expr);
        if (!matcher.matches()) {
            throw new IllegalStateException("Unsupported expression: '" + expression + "'. " +
                    "v0.1 supports: field OP constant, field == null, field != null, true, false");
        }

        String fieldName = matcher.group(1);
        String operator = matcher.group(2);
        String rawConstant = matcher.group(3);

        Object actual = readField(target, fieldName);
        Object expected = parseConstant(rawConstant);

        return compare(actual, operator, expected, fieldName, expression);
    }

    /**
     * Verifies that {@code expression} is well-formed and references a field
     * that exists on {@code targetClass}. Used by {@link Validator#compile(Class)}
     * so that rule typos fail fast, without needing an object instance.
     *
     * @throws IllegalStateException when the expression is malformed
     *                               or references a missing field
     */
    static void verify(Class<?> targetClass, String expression) {
        String expr = expression == null ? "" : expression.trim();
        if ("true".equals(expr) || "false".equals(expr)) {
            return;
        }
        Matcher matcher = COMPARISON.matcher(expr);
        if (!matcher.matches()) {
            throw new IllegalStateException("Unsupported expression: '" + expression + "'. " +
                    "v0.1 supports: field OP constant, field == null, field != null, true, false");
        }
        if (!hasField(targetClass, matcher.group(1))) {
            throw new IllegalStateException("Field '" + matcher.group(1) + "' does not exist on " +
                    targetClass.getSimpleName() + ". Check the expression for a typo.");
        }
        parseConstant(matcher.group(3));
    }

    /**
     * Returns the field name from an expression's left-hand side,
     * or {@code null} for literal expressions like {@code "true"}.
     */
    static String fieldNameOf(String expression) {
        if (expression == null) {
            return null;
        }
        Matcher matcher = COMPARISON.matcher(expression.trim());
        return matcher.matches() ? matcher.group(1) : null;
    }

    /** Checks whether a class has a field or an accessor method with the given name. */
    private static boolean hasField(Class<?> targetClass, String fieldName) {
        try {
            targetClass.getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException ignored) {
            try {
                targetClass.getMethod(fieldName);
                return true;
            } catch (NoSuchMethodException e) {
                return false;
            }
        }
    }

    /** Reads a field value: first as a declared field, then via its accessor method. */
    static Object readField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (NoSuchFieldException ignored) {
            // fall through to accessor lookup
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access field '" + fieldName + "'", e);
        }

        try {
            Method accessor = target.getClass().getMethod(fieldName);
            return accessor.invoke(target);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Field '" + fieldName + "' does not exist on " +
                    target.getClass().getSimpleName() + ". Check the expression for a typo.");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read field '" + fieldName + "'", e);
        }
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

    /** Applies the comparison operator between actual and expected values. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean compare(Object actual, String operator, Object expected,
                                   String fieldName, String expression) {
        switch (operator) {
            case "==":
                return java.util.Objects.equals(actual, expected);
            case "!=":
                return !java.util.Objects.equals(actual, expected);
            case ">":
            case "<":
            case ">=":
            case "<=":
                if (!(actual instanceof Comparable)) {
                    throw new IllegalStateException("Field '" + fieldName + "' is not comparable " +
                            "but expression '" + expression + "' uses '" + operator + "'");
                }
                int result;
                try {
                    result = ((Comparable) actual).compareTo(convertNumber(expected, actual));
                } catch (ClassCastException e) {
                    throw new IllegalStateException("Cannot compare field '" + fieldName + "' of type " +
                            actual.getClass().getSimpleName() + " with constant '" + expected + "'", e);
                }
                return switch (operator) {
                    case ">" -> result > 0;
                    case "<" -> result < 0;
                    case ">=" -> result >= 0;
                    default -> result <= 0;
                };
            default:
                throw new IllegalStateException("Unknown operator: '" + operator + "'");
        }
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
