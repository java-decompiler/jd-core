package org.jd.core.v1.stub;

public class StringConcatenation {

    public static final String SEMI_COLON = ";";

    @SuppressWarnings("static-access")
    public String toString(Object o1, Object o2) {
        final boolean b1 = o1 != null;
        final boolean b2 = o2 != null;
        final String insert = new StringBuilder("INSERT INTO EMPLOYEE(first_name,last_name,address,phone")
                .append((b1 ? "" : ",b1")).append((b2 ? "" : ",b2")).append(") VALUES(?,?,?,?")
                .append((b1 ? "" : ",?")).append((b2 ? "" : ",?")).append(")").append(this.SEMI_COLON)
                .toString();
        return insert;
    }
}
