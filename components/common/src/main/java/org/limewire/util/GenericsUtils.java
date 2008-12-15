package org.limewire.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides a means to ensure <code>Maps</code>, <code>Collections</code>,
 * <code>Sets</code> and <code>Lists</code> contain objects of a specific 
 * type only. <code>GenericsUtils</code> and its static methods are intended 
 * for checking the type-safety of de-serialized objects.
 */
public class GenericsUtils {
    
/**The mode {@link GenericsUtils} should use when scanning through objects.*/
    public enum ScanMode {
        /** Throw an exception on bad objects. */
        EXCEPTION,
        /** Remove the bad objects in place. */
        REMOVE,
        /** Create a new copy without the bad objects (if necessary) */
        NEW_COPY_REMOVED
    }

    private GenericsUtils() {}
    
    /**
     * Utility method for calling scanForMap(o, k, v, mode, null).
     * If NEW_COPY_REMOVED is the ScanMode, this will throw a NullPointerException.
     */
    public static <K, V> Map<K, V> scanForMap(Object o, Class<K> k, Class<V> v, ScanMode mode) {
        if(mode == ScanMode.NEW_COPY_REMOVED)
            throw new IllegalArgumentException("must use scanForMap(Object, Class, Class, ScanMode, Class");
        else
            return scanForMap(o, k, v, mode, null);
    }
    
    /**
     * Scans the object 'o' to make sure that it is a map,
     * all keys are type K and all values are type V.
     * If o is not a map, a ClassCastException is thrown.
     * 
     * The given ScanMode is used while scanning.  If the ScanMode
     * is NEW_COPY_REMOVED, then a Class must be given to create the copy
     * with bad elements removed, if necessary.
     * 
     * @param o
     * @param remove
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> scanForMap(Object o, Class<K> k, Class<V> v, ScanMode mode, Class<? extends Map> createFromThis) {
        if(o instanceof Map) {
            Map map = (Map)o;
            Map copy = null;
            for(Iterator i = map.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();
                Object key = entry.getKey();
                Object value = entry.getValue();
                if(key == null || value == null ||
                   !k.isAssignableFrom(key.getClass()) ||
                   !v.isAssignableFrom(value.getClass())) {
                    switch(mode) {
                    case EXCEPTION:
                    	StringBuilder errorReport = new StringBuilder();
                    	if (key == null)
                    		errorReport.append("key is null ");
                    	else if (!k.isAssignableFrom(key.getClass()))
                            errorReport.append("key class not assignable ")
                                    .append(key.getClass()).append(" to ").append(k);
                    	if (value == null)
                            errorReport.append("value is null for key ").append(key);
                    	else if (!v.isAssignableFrom(value.getClass()))
                            errorReport.append("value class not assignable ")
                                    .append(value.getClass()).append(" to ").append(v);
                        throw new ClassCastException(errorReport.toString());
                    case REMOVE:
                        i.remove();
                        break;
                    case NEW_COPY_REMOVED:
                        if(copy == null) {
                            copy = newInstance(createFromThis);
                            copy.putAll(map);
                        }
                        copy.remove(key);
                        break;
                    }
                }
            }
            if(copy != null)
                return copy;
            else
                return map;
        } else {
            throw new ClassCastException();
        }
    }

    /**
     * Utility method for calling scanForCollection(o, v, mode, null).
     * If NEW_COPY_REMOVED is the ScanMode, this will throw a NullPointerException.
     */
    public static <V> Collection<V> scanForCollection(Object o, Class<V> v, ScanMode mode) {
        if(mode == ScanMode.NEW_COPY_REMOVED)
            throw new IllegalArgumentException("must use scanForCollection(Object, Class, ScanMode, Class");
        else
            return scanForCollection(o, v, mode, null);
    }
    
    /**
     * Scans the object 'o' to make sure that it is a Collection,
     * and all values are type V.
     * If o is not a Collection, a ClassCastException is thrown.
     * 
     * The given ScanMode is used while scanning.  If the ScanMode
     * is NEW_COPY_REMOVED, then a Class must be given to create the copy
     * with bad elements removed, if necessary.
     * 
     * @param o
     * @param remove
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <V> Collection<V> scanForCollection(Object o, Class<V> v, ScanMode mode, Class<? extends Collection> createFromThis) {
        if(o instanceof Collection) {
            Collection c = (Collection)o;
            Collection copy = null;
            for(Iterator i = c.iterator(); i.hasNext(); ) {
                Object value = i.next();
                if(value == null || !v.isAssignableFrom(value.getClass())) {
                    switch(mode) {
                    case EXCEPTION:
                        throw new ClassCastException("wanted an instanceof: " + v + ", but was: " + (value == null ? "null" : value.getClass()));
                    case REMOVE:
                        i.remove();
                        break;
                    case NEW_COPY_REMOVED:
                        if(copy == null) {
                            copy = newInstance(createFromThis);
                            copy.addAll(c);
                        }
                        copy.remove(value);
                        break;
                    }
                }
            }
            
            if(copy != null)
                return copy;
            else
                return c;
        } else {
            throw new ClassCastException();
        }
        
    }
    
    /**
     * Utility method for calling scanForSet(o, v, mode, null).
     * If NEW_COPY_REMOVED is the ScanMode, this will throw a NullPointerException.
     */
    public static <V> Set<V> scanForSet(Object o, Class<V> v, ScanMode mode) {
        if(mode == ScanMode.NEW_COPY_REMOVED)
            throw new IllegalArgumentException("must use scanForSet(Object, Class, ScanMode, Class");
        else
            return scanForSet(o, v, mode, null);
    }
    
    /**
     * Scans the object 'o' to make sure that it is a Set,
     * and all values are type V.
     * If o is not a Set, a ClassCastException is thrown.
     * 
     * The given ScanMode is used while scanning.  If the ScanMode
     * is NEW_COPY_REMOVED, then a Class must be given to create the copy
     * with bad elements removed, if necessary.
     * 
     * @param o
     * @param remove
     * @return
     */
    public static <V> Set<V> scanForSet(Object o, Class<V> v, ScanMode mode, Class<? extends Set> createFromThis) {
        if(o instanceof Set) {
            return (Set<V>)scanForCollection(o, v, mode, createFromThis);
        } else {
            throw new ClassCastException();
        }
    }
    
    /**
     * Utility method for calling scanForList(o, v, mode, null).
     * If NEW_COPY_REMOVED is the ScanMode, this will throw a NullPointerException.
     */
    public static <V> List<V> scanForList(Object o, Class<V> v, ScanMode mode) {
        if(mode == ScanMode.NEW_COPY_REMOVED)
            throw new IllegalArgumentException("must use scanForList(Object, Class, ScanMode, Class");
        else
            return scanForList(o, v, mode, null);
    }
    
    /**
     * Scans the object 'o' to make sure that it is a List,
     * and all values are type V.
     * If o is not a List, a ClassCastException is thrown.
     * 
     * The given ScanMode is used while scanning.  If the ScanMode
     * is NEW_COPY_REMOVED, then a Class must be given to create the copy
     * with bad elements removed, if necessary.
     * 
     * @param o
     * @param remove
     * @return
     */
    public static <V> List<V> scanForList(Object o, Class<V> v, ScanMode mode, Class<? extends List> createFromThis) {
        if(o instanceof List) {
            return (List<V>)scanForCollection(o, v, mode, createFromThis);
        } else {
            throw new ClassCastException();
        }
    }
    
    
    /** Constructs a new class from this. */
    private static <T> T newInstance(Class<? extends T> creator) {
        try {
            return creator.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
