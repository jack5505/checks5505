package io.github.jack5505.checks5505;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reads a field value from a validated object. Resolved once per class/field
 * pair at compile time, then reused on every validation call — no reflection
 * lookup on the hot path.
 */
interface FieldAccessor {

    Object get(Object target);

    /**
     * Resolves the accessor for {@code fieldName} on {@code type}:
     * a declared field first (even a private one), then a public method
     * with the same name (a record component accessor or a getter).
     *
     * @throws IllegalStateException when neither a field nor a method exists
     */
    static FieldAccessor resolve(Class<?> type, String fieldName) {
        try {
            Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            return new FieldBased(fieldName, field);
        } catch (NoSuchFieldException ignored) {
            // fall through to method lookup
        }
        try {
            Method accessor = type.getMethod(fieldName);
            return new MethodBased(fieldName, accessor);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Field '" + fieldName + "' does not exist on " +
                    type.getSimpleName() + ". Check the expression for a typo.");
        }
    }

    /** Direct field read; {@code setAccessible} is done once, at resolution time. */
    final class FieldBased implements FieldAccessor {
        private final String fieldName;
        private final Field field;

        FieldBased(String fieldName, Field field) {
            this.fieldName = fieldName;
            this.field = field;
        }

        @Override
        public Object get(Object target) {
            try {
                return field.get(target);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot access field '" + fieldName + "'", e);
            }
        }
    }

    /** Accessor-method read, e.g. a record component accessor or a getter. */
    final class MethodBased implements FieldAccessor {
        private final String fieldName;
        private final Method method;

        MethodBased(String fieldName, Method method) {
            this.fieldName = fieldName;
            this.method = method;
        }

        @Override
        public Object get(Object target) {
            try {
                return method.invoke(target);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot read field '" + fieldName + "'", e);
            }
        }
    }
}
