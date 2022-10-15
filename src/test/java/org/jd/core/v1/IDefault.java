package org.jd.core.v1;

public interface IDefault {
    @SuppressWarnings("unused")
    default void test(Object... o) {}
    void test(Object[] o1, Object... o2);
}
