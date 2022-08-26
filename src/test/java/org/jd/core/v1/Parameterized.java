package org.jd.core.v1;

public class Parameterized {

    Class<?> clazz;
    RunnersFactory runnersFactory;

    Parameterized(Class<?> clazz) {
        this(clazz, new RunnersFactory(clazz));
    }

    Parameterized(Class<?> clazz, RunnersFactory runnersFactory) {
        this.clazz = clazz;
        this.runnersFactory = runnersFactory;
    }

    static class RunnersFactory {

        Class<?> clazz;

        private RunnersFactory(Class<?> clazz) {
            this.clazz = clazz;
        }
    }
}
