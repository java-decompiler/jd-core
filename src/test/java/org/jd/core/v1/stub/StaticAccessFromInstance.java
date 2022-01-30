package org.jd.core.v1.stub;

import java.io.File;
import java.util.Map;
import java.util.Objects;

import javax.swing.JOptionPane;

public class StaticAccessFromInstance {

    private Object name;
    private Object value;
    private Map<Object, Objects> values;
    private File f;
    private JOptionPane p;
    
    public static final String SEP =  "" + File.separatorChar;
    
    @SuppressWarnings({ "static-access", "unused" })
    protected Object getValue(int p1, int p2, int p3) { // Non-redundant test for ALOAD
        Objects objects = null;
        if (name != null) {
            objects = values.get(name);
            System.out.println("before try");
            try {
                if (objects == null || objects.isNull(objects.toString()) || this.p.ICON_PROPERTY.isEmpty() || this.f.separator.isEmpty()) {
                    System.err.println("Error !!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("after try");
       } else {
            objects = values.get(value);
            System.out.println("before try");
            try {
                if (objects == null || objects.isNull(objects.toString()) || this.SEP.isEmpty()) {
                    System.err.println("Error !!!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("after try");
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
