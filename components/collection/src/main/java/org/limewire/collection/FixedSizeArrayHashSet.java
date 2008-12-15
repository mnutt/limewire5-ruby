package org.limewire.collection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Provides a fixed size {@link HashSet} with index access. The replacement
 * policy is FIFO (First in, First out) and the iteration order is from newest 
 * to oldest.
 * <p>
 * Adding an existing element resets the FIFO order; the newly added element is
 * now the "last in" element.  
 * <p>
 * <code>FixedSizeArrayHashSet</code> does not support the null element.
 * <pre> 
    FixedSizeArrayHashSet&lt;String&gt; fsah = new FixedSizeArrayHashSet&lt;String&gt;(4);

    fsah.add("Abby");
    fsah.add("Bob");
    fsah.add("Chris");
    fsah.add("Dan");
    fsah.add("Eric");
    fsah.add("Fred");
    System.out.println(fsah);
    
    if(!fsah.add("Chris"))
        System.out.println("Tried to add Chris again, but it already exists in the collection (though Chris was put to the first item).");
    System.out.println(fsah);

    System.out.println("Index access: " + fsah.get(0));

    fsah.remove("Chris");
    System.out.println(fsah);
    Output:
        [Fred, Eric, Dan, Chris]
        Tried to add Chris again, but it already exists in the collection (though Chris was put to the first item).
        [Chris, Fred, Eric, Dan]
        Index access: Chris
        [Fred, Eric, Dan]

   </pre>    
 */
public class FixedSizeArrayHashSet<E> extends HashSet<E> implements RandomAccessCollection<E> {

    private transient FixedSizeArrayHashMap<E,Object> map;

    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();
    
    
    /**
     * creates a FixedSizeArrayHashSet with the specified maximum capacity.
     */
    public FixedSizeArrayHashSet(int maxCapacity) {
        map = new FixedSizeArrayHashMap<E, Object>(maxCapacity);
    }

    /**
     * Creates a <code>FixedSizeArrayHashSet</code> with maximum capacity the 
     * size of the provided collection and adds all the elements of that 
     * collection.
     */
    public FixedSizeArrayHashSet(Collection<? extends E> c) {
        this(c.size());
        addAll(c);
    }
    
    /**
     * Creates a <code>FixedSizeArrayHashSet</code> with the provided maximum capacity and
     * adds elements from the provided collection.  If the capacity is less than
     * the size of the collection, elements will get ejected with FIFO policy.
     */
    public FixedSizeArrayHashSet(int maxCapacity, Collection<? extends E> c) {
        this(maxCapacity);
        addAll(c);
    }

    public FixedSizeArrayHashSet(int maxCapacity, int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        map = new FixedSizeArrayHashMap<E, Object>(maxCapacity);
    }

    public FixedSizeArrayHashSet(int maxCapacity, int initialCapacity) {
        super(initialCapacity);
        map = new FixedSizeArrayHashMap<E, Object>(maxCapacity);
    }


    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @SuppressWarnings({"SuspiciousMethodCalls"})
    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean add(E o) {
        return map.put(o, PRESENT)==null;
    }

    @Override
    public boolean remove(Object o) {
    return map.remove(o)==PRESENT;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object clone() {
       FixedSizeArrayHashSet<E> newSet = (FixedSizeArrayHashSet<E>) super.clone();
       newSet.map = (FixedSizeArrayHashMap<E, Object>) map.clone();
       return newSet;
    }
    
    public E get(int i) {
        return map.getKeyAt(i);
    }
}
