package org.jd.core.v1.stub;

import java.util.ArrayList;
import java.util.List;

public class AutoUnboxingInLoop {
    void test(List<?> paramList, Object obj) {
        List<Object> list1 = new ArrayList<>();
        List<Object> list2 = new ArrayList<>();

        Integer hashCode = obj.hashCode();
        List<Object> list;
        while (hashCode != -1) {
            list = new ArrayList<>();
            Object next;
            for (int j = 0; j < paramList.size(); j++) {
                next = paramList.listIterator(j).next();
                if (next != null) {
                    Object elem = paramList.listIterator(j).next();
                    String str = String.valueOf(elem.hashCode());
                    if (hashCode.equals(Integer.parseInt(str))) {
                        list.add(elem);
                    }
                }
            }

            list2.addAll(list1);
            hashCode--;
        }
        List<Object> l = new ArrayList<>();
        Object next;
        for (int i = 0; i < list2.size(); i++) {
            next = list2.listIterator(i).next();
            if (next != null) {
                l.add(next);
            }
        }
    }
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings
    public static Integer getInteger() {
        Integer index = (int) Math.random();
        if (index > 0) {
            index--;
        } else {
            index = null;
        }
        return index;
    }
}
