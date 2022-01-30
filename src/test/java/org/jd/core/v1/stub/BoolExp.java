package org.jd.core.v1.stub;

import java.awt.Color;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JToolBar;

public class BoolExp {

    public static final String CONST1 = "CONST1";
    public static final String CONST2 = "CONST2";
    public static final String CONST3 = "CONST3";
    public static final String CONST4 = "CONST4";

    private Object field1;
    private Object field2;
    private Object field3;
    private Object field4;
    private Object field5;
    private Object field6;
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings
    @SuppressWarnings({ "unchecked", "unlikely-arg-type" })
    public boolean isValid(@SuppressWarnings("rawtypes") Vector v){
        if(field1 == null || "".equals(field1)){
            v.add( new String ("Field #1 is not valid"));
        }

        if(field2 == null || "".equals(field2)){
            v.add(new String("Field #2 is not valid"));
        }

        if(field3 == null || "".equals(field3)){
            v.add( new String("Field #3 is not valid"));
        }
        
        if(field4 == null || "".equals(field4 == null)){
            v.add(new String("Field #4 is not valid"));
        }

        if(field5 == null || "".equals(field5)){
            v.add(new String("Field #5 is not valid"));
        }

        if(field6 == null || "".equals(field6)){
            v.add(new String("Field #6 is not valid"));
        }

        return v.isEmpty();
    }

    public static void addButton(JToolBar toolBar, JButton button) {
        toolBar.add(button, Math.random() == 0 && toolBar.getComponentCount() > (Math.random() > 0.5 ? 1 : 0));
    }

    @SuppressWarnings("static-access")
    public boolean isValidChoice(String s) {
        boolean bool = true;
        if ( (Color.BLACK.equals(getColorChoice(s)) &&
              (this.CONST1.equals(s) || this.CONST2.equals(s))) ||
            (Color.WHITE.equals(getColorChoice(s)) &&
             (this.CONST3.equals(s) || this.CONST4.equals(s)))) {

            bool = false;
        }
        return bool;
    }

    private static Color getColorChoice(String s) {
        return "BLACK".equals(s) ? Color.BLACK : Color.WHITE;
    }
}
