package org.limewire.inspection;

import java.util.HashMap;
import java.util.Map;

/**
 * An utility class for adding inspection histograms.
 * Typed for convenience.  
 * <p>
 * Note1: the bencoded keys are going to use the String.valueOf(K).
 * <p>
 * Note2: deliberately not using Weak map, so don't use with objects that
 * you can't afford leaked.  
 */
public class InspectionHistogram<K> implements Inspectable {

    private final Map<K, Long> counts = new HashMap<K, Long>();
    
    /**
     * Counts single occurrence of K. 
     */
    public synchronized long count(K occurence) {
        return count(occurence, 1);
    }
    
    /**
     * Adds <code>value</code> to values under K.
     */
    public synchronized long count(K key, long value) {
        Long already = counts.get(key);
        if (already == null) {
            already = 0L;
        }
        counts.put(key, already + value);
        return already + value;
    }
    
    @Override
    public synchronized Map<K, Long> inspect() {
        return new HashMap<K, Long>(counts);
    }
    
    @Override
    public String toString() {
        return inspect().toString();
    }
}
