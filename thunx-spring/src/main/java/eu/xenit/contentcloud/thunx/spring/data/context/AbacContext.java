package eu.xenit.contentcloud.thunx.spring.data.context;

import eu.xenit.contentcloud.thunx.predicates.model.Expression;

public class AbacContext {

    private static ThreadLocal<Expression<Boolean>> currentAbacContext = new InheritableThreadLocal<Expression<Boolean>>();

    public static Expression<Boolean> getCurrentAbacContext() {
        return currentAbacContext.get();
    }

    public static void setCurrentAbacContext(Expression<Boolean> expression) {
        currentAbacContext.set(expression);
    }

    public static void clear() {
        currentAbacContext.set(null);
    }
}
