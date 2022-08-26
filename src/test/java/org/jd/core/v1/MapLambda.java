package org.jd.core.v1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapLambda {
    Map<String, List<String>> map;

    Map<String, List<String>> getMap() {
        Map<String, List<String>> result = new HashMap<>(this.map.size());
        this.map.forEach((key, value) -> result.put(key, value));
        return result;
    }
}