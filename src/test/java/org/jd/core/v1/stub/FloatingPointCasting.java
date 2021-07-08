package org.jd.core.v1.stub;

public class FloatingPointCasting {
    private final long l = 9223372036854775806L;
    private final Long L = 9223372036854775806L;

    long getLong() {
        return 9223372036854775806L;
    }

    void test1() {
        long b = (long) (double) getLong();
        System.out.println(b == getLong()); // Prints "false"
    }
    void test2() {
        long b = (long) (double) l;
        System.out.println(b == l); // Prints "false"
    }
    void test3() {
        long b = (long) (double) L;
        System.out.println(b == L); // Prints "false"
    }
}
