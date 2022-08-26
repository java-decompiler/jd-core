package org.jd.core.v1;

public class LambdaStackWalker2 {
    public Class<?> getCallerClass1() {
        return StackWalker.getInstance().walk(s -> s.findFirst()).map(s -> s.getDeclaringClass()).orElse(null);
    }

    public Class<?> getCallerClass2() {
        return StackWalker.getInstance().walk(s -> s.findFirst()).map(StackWalker.StackFrame::getDeclaringClass).orElse(null);
    }
}