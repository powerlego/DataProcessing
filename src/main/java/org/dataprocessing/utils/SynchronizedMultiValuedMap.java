package org.dataprocessing.utils;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiSet;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.AbstractMultiValuedMapDecorator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author Nicholas Curl
 */
public class SynchronizedMultiValuedMap<K, V> extends AbstractMultiValuedMapDecorator<K, V> {

    /**
     * The instance of the logger
     */
    private static final Logger logger = LogManager.getLogger(SynchronizedMultiValuedMap.class);

    /**
     * Constructor that wraps (not copies).
     *
     * @param map the map to decorate, must not be null
     *
     * @throws NullPointerException if the map is null
     */
    public SynchronizedMultiValuedMap(MultiValuedMap<K, V> map) {
        super(map);
    }

    @Override
    public synchronized int size() {
        return super.size();
    }

    @Override
    public synchronized boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return super.containsKey(key);
    }

    @Override
    public synchronized boolean containsValue(Object value) {
        return super.containsValue(value);
    }

    @Override
    public synchronized boolean containsMapping(Object key, Object value) {
        return super.containsMapping(key, value);
    }

    @Override
    public synchronized Collection<V> get(K key) {
        return super.get(key);
    }

    @Override
    public synchronized Collection<V> remove(Object key) {
        return super.remove(key);
    }

    @Override
    public synchronized boolean removeMapping(Object key, Object item) {
        return super.removeMapping(key, item);
    }

    @Override
    public synchronized void clear() {
        super.clear();
    }

    @Override
    public synchronized boolean put(K key, V value) {
        return super.put(key, value);
    }

    @Override
    public synchronized Set<K> keySet() {
        return super.keySet();
    }

    @Override
    public synchronized Collection<Entry<K, V>> entries() {
        return super.entries();
    }

    @Override
    public synchronized MultiSet<K> keys() {
        return super.keys();
    }

    @Override
    public synchronized Collection<V> values() {
        return super.values();
    }

    @Override
    public synchronized Map<K, Collection<V>> asMap() {
        return super.asMap();
    }

    @Override
    public synchronized boolean putAll(K key, Iterable<? extends V> values) {
        return super.putAll(key, values);
    }

    @Override
    public synchronized boolean putAll(Map<? extends K, ? extends V> map) {
        return super.putAll(map);
    }

    @Override
    public synchronized boolean putAll(MultiValuedMap<? extends K, ? extends V> map) {
        return super.putAll(map);
    }

    @Override
    public synchronized MapIterator<K, V> mapIterator() {
        return super.mapIterator();
    }

    @Override
    public boolean equals(Object object) {
        return super.equals(object);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
