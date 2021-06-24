package org.jd.core.v1.stub;

@SuppressWarnings("all") public class InitializedArrayInTernaryOperator {
    Class[] test0(int i) {
        return (i == 0) ? new Class[] { Object.class } : null;
    }
    Class[] test2(int i) {
        return (i == 0) ? new Class[] { Object.class, String.class, Number.class } : null;
    }
    Class[][] test3(int i) {
        return (i == 0) ? new Class[][] { { Object.class }, { String.class, Number.class} } : null;
    }
    Class[] test4(int i) {
        return (i == 0) ? null : new Class[] { Object.class };
    }
    Class[] test5(int i) {
        return (i == 0) ? null : new Class[] { Object.class, String.class, Number.class };
    }
    Class[][] test6(int i) {
        return (i == 0) ? null : new Class[][] { { Object.class }, { String.class, Number.class} };
    }
    Class[] test7(int i) {
        return (i == 0) ? new Class[] { Object.class } : new Class[] { String.class, Number.class };
    }
}
