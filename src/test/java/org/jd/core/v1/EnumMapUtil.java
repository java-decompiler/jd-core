package org.jd.core.v1;

import java.util.LinkedHashMap;
import java.util.Map;

public class EnumMapUtil {

    public static <E extends Enum<E>> Map<String, E> getEnumMap(Class<E> enumClass) {
        Map<String, E> map = new LinkedHashMap<>();
        for (E e : enumClass.getEnumConstants()) {
            map.put(e.name(), e);
        }
        return map;
    }
}