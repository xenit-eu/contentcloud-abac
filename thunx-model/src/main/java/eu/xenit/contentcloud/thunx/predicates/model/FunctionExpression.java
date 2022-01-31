package eu.xenit.contentcloud.thunx.predicates.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public interface FunctionExpression<T> extends ThunkExpression<T> {

    Operator getOperator();

    List<ThunkExpression<?>> getTerms();

    default <R> R accept(ThunkExpressionVisitor<R> visitor) {
        return visitor.visit(this);
    }

    default String toDebugString() {
        return String.format("%s(%s)",
                this.getOperator().getKey().toUpperCase(Locale.ROOT),
                this.getTerms().stream().map(Object::toString).collect(Collectors.joining(", ")));
    }

    @RequiredArgsConstructor
    @AllArgsConstructor
    enum Operator {
        // Comparison operators
        EQUALS("eq", Boolean.class, (FunctionExpressionFactory<Boolean>) Comparison::areEqual,
                values -> values.distinct().count() <= 1),
        NOT_EQUAL_TO("neq", Boolean.class),
        GREATER_THAN("gt", Boolean.class),
        GREATER_THAN_OR_EQUAL_TO("gte", Boolean.class),
        LESS_THAN("lt", Boolean.class),
        LESS_THEN_OR_EQUAL_TO("lte", Boolean.class),

        // Logical operator
        AND("and", Boolean.class, (FunctionExpressionFactory<Boolean>) LogicalOperation::uncheckedConjunction,
                values -> {
                    return values.allMatch(Boolean.TRUE::equals);
                }),
        OR("or", Boolean.class, (FunctionExpressionFactory<Boolean>) LogicalOperation::uncheckedDisjunction,
                values -> {
                    return values.anyMatch(Boolean.TRUE::equals);
                }),
        NOT("not", Boolean.class),

        // Numeric operators
        PLUS("plus", Number.class),
        MINUS("minus", Number.class),
        DIVIDE("div", Number.class),
        MULTIPLY("mul", Number.class),
        MODULUS("mod", Number.class);

        @Getter
        @NonNull
        private final String key;

        @Getter
        @NonNull
        private final Class<?> type;

        // TODO remove getter, expose create method directly and delegate to the factory instead
        @Getter
        private FunctionExpressionFactory<?> factory;

        private FunctionExpressionEvaluator<?> evaluator;

        public static Operator resolve(@NonNull String key) {
            return Arrays.stream(Operator.values())
                    .filter(op -> Objects.equals(key, op.getKey()))
                    .findFirst()
                    .orElseThrow(() -> {
                        String message = String.format("Invalid %s key: '%s'", Operator.class.getSimpleName(), key);
                        return new IllegalArgumentException(message);
                    });
        }

        public <T> T eval(Stream<Object> values) {
            if (this.evaluator == null) {
                throw new UnsupportedOperationException("Operator '" + this.getKey() + "' does not support eval");
            }
            return (T) this.evaluator.eval(values);
        }

    }

    @FunctionalInterface
    interface FunctionExpressionFactory<T> {

        FunctionExpression create(List<ThunkExpression<?>> terms);
    }

    @FunctionalInterface
    interface FunctionExpressionEvaluator<T> {

        T eval(Stream<Object> values);
    }
}
