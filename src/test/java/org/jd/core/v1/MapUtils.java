package org.jd.core.v1;

import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.TransformerUtils;

import java.util.Map;

interface MapUtils {
    <K, W, E> void populateMap(MultiMap<K, W> map, Iterable<? extends E> elements, Transformer<E, K> keyTransformer, Transformer<E, W> valueTransformer);

    <K, W, E> void populateMap(Map<K, W> map, Iterable<? extends E> elements, Transformer<E, K> keyTransformer, Transformer<E, W> valueTransformer);

    default <K, W> void populateMap(MultiMap<K, W> map, Iterable<? extends W> elements, Transformer<W, K> keyTransformer) {
        populateMap(map, elements, keyTransformer, TransformerUtils.nopTransformer());
    }

    default <K, W> void populateMap(Map<K, W> map, Iterable<? extends W> elements, Transformer<W, K> keyTransformer) {
        populateMap(map, elements, keyTransformer, TransformerUtils.nopTransformer());
    }
}