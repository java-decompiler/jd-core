package org.jd.core.v1;

public class LambdaStackWalker1 {
    public Class<?> getCallerClass(Class<?> sentinelClass) {
        return StackWalker.getInstance().walk(s -> s.map(StackWalker.StackFrame::getDeclaringClass).dropWhile(clazz -> !sentinelClass.equals(clazz)).findFirst().orElse(null));
    }
}