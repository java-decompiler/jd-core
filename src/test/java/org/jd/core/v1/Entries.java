package org.jd.core.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Entries {
    Map<String, Entry<String, String>> entries = new HashMap<String, Entry<String, String>>();

    void test() {
        ArrayList<Entry<String, String>> arrayList = new ArrayList<Entry<String, String>>(entries.values());
        for (Entry<String, String> entry : arrayList) {
            System.out.println(entry);
        }
    }
}