package org.jd.core.v1.stub;

import java.util.Map;
import java.util.Objects;

public class StaticAccessFromInstance {

    private Object name;
    private Object value;
    private Map<Object, Objects> values;

    @SuppressWarnings({ "static-access", "unused" })
    protected Object getValue(int p1, int p2, int p3) { // Non-redundant test for ALOAD
        Objects objects = null;
        if (name != null) {
            objects = values.get(name);
            if (objects == null || objects.isNull(objects.toString())) {
                System.err.println("Error !!");
            }
        } else {
            objects = values.get(value);
            if (objects == null || objects.isNull(objects.toString())) {
                System.err.println("Error !!!");
            }
        }
        return objects;
    }

    @SuppressWarnings({ "static-access", "unused" })
    protected Object getValue(int p1, int p2) { // Non-redundant test for ALOAD_3
        Objects objects = null;
        if (name != null) {
            objects = values.get(name);
            if (objects == null || objects.isNull(objects.toString())) {
                System.err.println("Error !!");
            }
        } else {
            objects = values.get(value);
            if (objects == null || objects.isNull(objects.toString())) {
                System.err.println("Error !!!");
            }
        }
        return objects;
    }

    @SuppressWarnings({ "static-access", "unused" })
    protected Object getValue(int p1) { // Non-redundant test for ALOAD_2
        Objects objects = null;
        if (name != null) {
            objects = values.get(name);
            if (objects == null || objects.isNull(objects.toString())) {
                System.err.println("Error !!");
            }
        } else {
            objects = values.get(value);
            if (objects == null || objects.isNull(objects.toString())) {
                System.err.println("Error !!!");
            }
        }
        return objects;
    }

    @SuppressWarnings("static-access")
    protected Object getValue() { // Non-redundant test for ALOAD_1
        Objects objects = null;
        if (name != null) {
            objects = values.get(name);
            if (objects == null || objects.isNull(objects.toString())) {
                System.err.println("Error !!");
            }
        } else {
            objects = values.get(value);
            if (objects == null || objects.isNull(objects.toString())) {
                System.err.println("Error !!!");
            }
        }
        return objects;
    }

}
