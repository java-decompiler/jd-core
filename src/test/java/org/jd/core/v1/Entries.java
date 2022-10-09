package org.jd.core.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Entries {
    Map<Integer, String[]> cCache = new HashMap<Integer, String[]>();

    Map<String, Entry<String, String>> entries = new HashMap<String, Entry<String, String>>();

    void test() {
        for (Entry<String, String> entry : new ArrayList<Entry<String, String>>(entries.values())) {
            System.out.println(entry);
        }
    }
}