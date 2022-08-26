package org.jd.core.v1;

import java.util.Collections;
import java.util.Iterator;

public class TestBoundsLambda {
    static final Iterator<Class<?>> wrapped = Collections.<Class<?>>emptySet().iterator();

    public static Iterable<Class<?>> hierarchy() {
        return () -> {
            return new Iterator<Class<?>>() {
                Iterator<Class<?>> interfaces = Collections.<Class<?>>emptySet().iterator(); // Ljava/util/Iterator;

                @Override
                public boolean hasNext() {
                    return interfaces.hasNext() || wrapped.hasNext();
                }

                @Override
                public Class<?> next() {
                    if (interfaces.hasNext()) {
                        final Class<?> nextInterface = interfaces.next();
                        return nextInterface;
                    }
                    final Class<?> nextSuperclass = wrapped.next();
                    return nextSuperclass;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

            };
        };
    }
}
