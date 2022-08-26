package org.jd.core.v1;

import java.util.Map;

public class StringMap {
    @SuppressWarnings("unused")
    private static TriConsumer<String, String, Map<String, String>> PUT_ALL = (key, value, stringStringMap) -> stringStringMap.put(key, value);
}
