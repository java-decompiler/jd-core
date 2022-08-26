package org.jd.core.v1;

import org.apache.logging.log4j.util.IndexedStringMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexedStringMapLambda {
    IndexedStringMap map;

    @SuppressWarnings("unchecked")
    Map<String, List<String>> getMap() {
        Map<String, List<String>> result = new HashMap<>(this.map.size());
        this.map.forEach((key, value) -> result.put(key, (List<String>) value));
        return result;
    }
}