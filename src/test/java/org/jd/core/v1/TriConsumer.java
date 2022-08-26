package org.jd.core.v1;
public interface TriConsumer<K, V, S> {
    void accept(K k, V v, S s);
}