package org.dataprocessing.utils;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.AbstractListValuedMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Nicholas Curl
 */
public class ArrayListTreeMap<K extends Comparable<? super K>, V> extends AbstractListValuedMap<K, V> {

    /**
     * The instance of the logger
     */
    private static final Logger logger = LogManager.getLogger(ArrayListTreeMap.class);

    private static final int DEFAULT_INITIAL_LIST_CAPACITY = 3;

    private final int initialListCapacity;

    public ArrayListTreeMap() {
        this(DEFAULT_INITIAL_LIST_CAPACITY);
    }

    public ArrayListTreeMap(final int initialListCapacity) {
        super(new TreeMap<K, ArrayList<V>>());
        this.initialListCapacity = initialListCapacity;
    }


    public ArrayListTreeMap(final MultiValuedMap<? extends K, ? extends V> map) {
        this(DEFAULT_INITIAL_LIST_CAPACITY);
        super.putAll(map);
    }

    public ArrayListTreeMap(final Map<? extends K, ? extends V> map) {
        this(DEFAULT_INITIAL_LIST_CAPACITY);
        super.putAll(map);
    }

    /**
     * Creates a new value collection using the provided factory.
     *
     * @return a new list
     */
    @Override
    protected ArrayList<V> createCollection() {
        return new ArrayList<>(initialListCapacity);
    }

    public void trimToSize() {
        for (final Collection<V> coll : getMap().values()) {
            final ArrayList<V> list = (ArrayList<V>) coll;
            list.trimToSize();
        }
    }
}
