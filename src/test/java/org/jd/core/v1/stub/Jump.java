package org.jd.core.v1.stub;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Jump {
    @SuppressWarnings("static-access")
    public static boolean computeFlag(String paramObj1, Object paramObj2) {
        boolean flag;
        if (paramObj1 == null || Objects.isNull(paramObj2)) {
            flag = false;
        } else if (paramObj2.equals(paramObj1.toString())) {
            flag = true;
        } else {
            flag = false;
            List<Object> list = Collections.emptyList();
            label: {
                for (Object elem : list) {
                    if ("".equals(elem.toString())) {
                        for (String string : elem.toString().split("")) {
                            if (computeFlag(paramObj1.valueOf(string), paramObj2)) {
                                flag = true;
                                break label;
                            }
                        }
                    }
                }
            }
        }
        return flag;
    }
    
    @SuppressWarnings("static-access")
    public static boolean computeFlag2(String paramObj1, Object paramObj2) {
        boolean flag;
        if (paramObj1 == null || Objects.isNull(paramObj2)) {
            flag = false;
        } else if (paramObj2.equals(paramObj1.toString())) {
            flag = true;
        } else {
            flag = false;
            List<Object> list = Collections.emptyList();
            label: {
                for (Object elem : list) {
                    if ("".equals(elem.toString())) {
                        for (String string : elem.toString().split("")) {
                            if (computeFlag(paramObj1.valueOf(string), paramObj2)) {
                                flag = true;
                                break label;
                            }
                        }
                    }
                }
            }
            System.out.println("this is not the return statement yet");
        }
        return flag;
    }
    
    @SuppressWarnings("static-access")
    public static boolean computeFlag3(String paramObj1, Object paramObj2) {
        boolean flag;
        if (paramObj1 == null || Objects.isNull(paramObj2)) {
            flag = false;
        } else if (paramObj2.equals(paramObj1.toString())) {
            flag = true;
        } else {
            flag = false;
            List<Object> list = Collections.emptyList();
            label: {
                for (Object elem : list) {
                    if ("".equals(elem.toString())) {
                        for (String string : elem.toString().split("")) {
                            if (computeFlag(paramObj1.valueOf(string), paramObj2)) {
                                flag = true;
                                break label;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("this is not the return statement yet");
        return flag;
    }
}
