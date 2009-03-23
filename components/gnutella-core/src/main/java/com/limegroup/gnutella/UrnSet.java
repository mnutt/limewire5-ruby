package com.limegroup.gnutella;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A Set specifically for URNs.
 * This is not backed by a HashSet because there are
 * so few URN types that it doesn't need to be.
 * 
 * This currently only supports SHA1 and TTROOT URNs. If further
 * UrnTypes are created, this class will have to be updated.
 * (Note that there'll have to be MANY changes.)
 * 
 * If the set already has a URN of the specified type added to it,
 * further additions of that type will be rejected.
 */
public class UrnSet implements Set<URN>, Iterable<URN>, Cloneable, Serializable {
    
    private static final long serialVersionUID = -1065284624401321676L;

    /** The sole URNs this knows about. */
    private URN sha1, ttroot;
    
    /** Constructs an empty UrnSet. */
    public UrnSet() {}
    
    /** Constructs a UrnSet with the given URN. */
    public UrnSet(URN urn) {
        add(urn);
    }
    
    /** Constructs a UrnSet with URNs from the given collection. */
    public UrnSet(Collection<? extends URN> c) {
        addAll(c);
    }
    
    @Override
    public String toString() {
        return isEmpty() ? "{Empty UrnSet}" : "UrnSet of: " + sha1;
    }
    
    /** Clones this set. */
    @Override
    public UrnSet clone() {
        UrnSet c = new UrnSet();
        c.sha1 = sha1;
        return c;
    }
    
    /** Returns the hashcode for this UrnSet. */
    @Override
    public int hashCode() {
        return sha1 == null ? 0 : sha1.hashCode();
    }
    
    URN getSHA1() {
        return sha1;
    }
    
    URN getTTRoot() {
        return ttroot;
    }
    
    /**
     * Determines if this set contains all the same objects
     * as another set.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Set))
            return false;

        Collection c = (Collection) o;
        if (c.size() != size())
            return false;
        
        try {
            return containsAll(c);
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
    }

    /**
     * Attempts to add the given URN into this set.
     * If the set already contained a URN of the same type,
     * the new URN is rejected and this returns false.
     * 
     * If the URN's type is not supported, the URN is rejected
     * and this returns false.
     * 
     * @param o
     * @return
     */
    public boolean add(URN o) {
        if(o.isSHA1() && sha1 == null) {
            sha1 = o;
            return true;
        } else if (o.isTTRoot() && ttroot == null) {
            ttroot = o;
            return true;
        }
        return false;
    }

    /**
     * Attempts to add all the URNs in the given collection into this
     * set.  If the set was modified as a result of any of the additions,
     * this will return true.  Otherwise, it will return false.
     * 
     * @param c
     * @return
     */
    public boolean addAll(Collection<? extends URN> c) {
        boolean ret = false;
        for(URN urn : c)
            ret |= add(urn);
        return ret;
    }

    public void clear() {
        sha1 = null;
        ttroot = null;
    }

    public boolean contains(Object o) {
        return o.equals(sha1) || o.equals(ttroot);
    }

    public boolean containsAll(Collection<?> c) {
        if(c.size() > 2)
            return false;
        if(c.isEmpty())
            return true;
        if(isEmpty())
            return false;
        
        boolean ret = true;
        for (Object o : c)
            ret &= contains(o);
        return ret;
    }

    public boolean isEmpty() {
        return sha1 == null && ttroot == null;
    }

    public Iterator<URN> iterator() {
        return new UrnIterator();
    }

    public boolean remove(Object o) {
        if(sha1 != null && o.equals(sha1)) {
            sha1 = null;
            return true;
        }
        
        if(ttroot != null && o.equals(ttroot)) {
            ttroot = null;
            return true;
        }
        
        return false;
    }

    public boolean removeAll(Collection<?> c) {
        if(sha1 == null && ttroot == null || c.isEmpty())
            return false;
        
        boolean ret = false;
        for(Object o : c) {
            ret |= remove(o);
            if (isEmpty())
                break;
        }
        
        return ret;
    }

    public boolean retainAll(Collection<?> c) {
        boolean ret = false;
        if(sha1 != null && !c.contains(sha1)) {
            sha1 = null;
            ret = true;
        }
        if(ttroot != null && !c.contains(ttroot)) {
            ttroot = null;
            ret = true;
        }
        
        return ret;
    }

    public int size() {
        int ret = 0;
        if (sha1 != null)
            ret++;
        if (ttroot != null)
            ret++;
        return ret;
    }

    public Object[] toArray() {
        switch(size()) {
        case 0: return new Object[0];
        case 1: Object o = sha1 != null ? sha1 : ttroot;
            return new Object[]{o};
        case 2: return new Object[]{sha1, ttroot};
        default:
            throw new IllegalStateException();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        int size = size();
        if (a.length < size)
            a = (T[])Array.newInstance(a.getClass().getComponentType(), size);
        
        switch(size) {
        case 1 :
            URN present = sha1 != null ? sha1 : ttroot;
                    a[0] = (T)present;
                    break;
        case 2 :
            a[0] = (T)sha1;
            a[1] = (T)ttroot;
        }
        
        if(a.length > size)
            a[size] = null;
        return a;
    }
    
    /** Iterator that returns each of the Urn Types in turn. */
    private class UrnIterator implements Iterator<URN> {
        private boolean givenSHA1,givenTTRoot;
        
        public boolean hasNext() {
            return (!givenSHA1 && sha1 != null) ||
               (!givenTTRoot && ttroot != null);
        }

        public URN next() {
            if (!hasNext())
                throw new NoSuchElementException();
            
            if (!givenSHA1 && sha1 != null) {
                givenSHA1 = true;
                return sha1;
            }
            if (!givenTTRoot && ttroot != null) {
                givenTTRoot = true;
                return ttroot;
            }
            throw new IllegalStateException();
        }

        public void remove() {
            if (!(givenSHA1 || givenTTRoot))
                throw new IllegalStateException();
            
            if (givenTTRoot) 
                ttroot = null;
            else if (givenSHA1) 
                sha1 = null;
        }

    }

    public static URN getSha1(Set<? extends URN> urns) {
        if(urns instanceof UrnSet) {
            return ((UrnSet)urns).sha1;
        } else {
            for(URN urn : urns) {
                if(urn.isSHA1()) {
                    return urn;
                }
            }
            return null;
        }
    }

}
