package org.jd.core.v1.stub;

@SuppressWarnings("all")
public class ArrayMethodOverloading {

    void use(Object[] o) {}

    void use(Object o) {}

    void test1() {
        use("string");
    }

    void test2() {
        use((Object) new Object[]{""});
    }

    void test3() {
        use(null);
    }

    void test4() {
        use((Object) null);
    }
}
