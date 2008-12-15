package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * Tests the public methods of the UrnCache class.
 */
public final class UrnSetTest extends BaseTestCase {
    
    private static final Random RND = new Random();
    
    /**
     * Constructs a new UrnCacheTest with the specified name.
     */
    public UrnSetTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(UrnSetTest.class);
    }

    /**
     * Runs this test individually.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testEmptyness() throws Exception {
        Set<URN> s = new UrnSet();
        assertTrue(s.isEmpty());
        assertEquals(0, s.size());
        Object[] objects = s.toArray();
        assertEquals(0, objects.length);
        URN[] type = new URN[0];
        URN[] urns = s.toArray(type);
        assertSame(type, urns);
        type = new URN[1];
        urns = s.toArray(type);
        assertSame(type, urns);
        type[0] = sha1();
        assertNotNull(type[0]);
        urns = s.toArray(type);
        assertSame(urns, type);
        assertNull(urns[0]);
        
        Set<URN> clone = ((UrnSet)s).clone();
        assertNotSame(clone, s);
        assertEquals(clone, s);
        assertTrue(clone.isEmpty());
        
        Set<URN> hash = new HashSet<URN>();
        assertEquals(s, hash);
        assertTrue(s.containsAll(hash));
        assertFalse(s.removeAll(hash));
        assertFalse(s.remove(new Object()));
        assertFalse(s.remove(sha1()));
        assertFalse(s.addAll(hash));
        assertFalse(s.retainAll(hash));
        
        hash.add(sha1());
        assertNotEquals(s, hash);
        assertFalse(s.containsAll(hash));
        assertFalse(s.removeAll(hash));
        assertFalse(s.remove(sha1()));
        assertFalse(s.retainAll(hash));        
        
        Iterator<URN> i = s.iterator();
        assertFalse(i.hasNext());
        try {
            i.remove();
            fail("expected exception");
        } catch(IllegalStateException expected) {}
        try {
            i.next();
            fail("expected exception");
        } catch(NoSuchElementException expected) {}
    }
    
    public void testFullness() throws Exception {
        Set<URN> s = new UrnSet();
        URN a = sha1();
        assertTrue(s.add(a));
        URN b = sha1();
        assertFalse(s.add(b));
        assertEquals(1, s.size());
        assertFalse(s.isEmpty());
        assertTrue(s.contains(a));
        assertFalse(s.contains(b));
        assertFalse(s.remove(b));
        
        
        Object[] objects = s.toArray();
        assertEquals(1, objects.length);
        assertEquals(a, objects[0]);
        
        URN[] type = new URN[0];
        URN[] urns = s.toArray(type);
        assertNotSame(type, urns);
        assertEquals(a, urns[0]);
        
        type = new URN[1];
        urns = s.toArray(type);
        assertSame(type, urns);
        assertEquals(a, urns[0]);
        
        type = new URN[2];
        type[1] = sha1();
        urns = s.toArray(type);
        assertSame(urns, type);
        assertEquals(a, urns[0]);
        assertNull(urns[1]);
        
        Set<URN> clone = ((UrnSet)s).clone();
        assertNotSame(clone, s);
        assertEquals(clone, s);
        assertTrue(clone.contains(a));
        assertEquals(1, clone.size());
        
        Set<URN> hash = new HashSet<URN>();
        assertNotEquals(s, hash);
        assertTrue(s.containsAll(hash));
        assertFalse(s.removeAll(hash));
        assertFalse(s.addAll(hash));
        assertTrue(s.retainAll(hash));
        assertEquals(0, s.size());
        assertTrue(s.isEmpty());
        
        assertTrue(s.add(a));
        assertEquals(1, s.size());
        
        hash.add(a);
        assertEquals(s, hash);
        assertTrue(s.containsAll(hash));
        assertFalse(s.addAll(hash));
        assertFalse(s.retainAll(hash));
        assertTrue(s.removeAll(hash));
        assertTrue(s.isEmpty());
        
        assertTrue(s.add(a));
        hash.add(b);
        assertNotEquals(s, hash);
        assertFalse(s.containsAll(hash));
        assertFalse(s.addAll(hash));
        assertFalse(s.retainAll(hash));
        assertTrue(s.removeAll(hash));
        
        s = new UrnSet(a);
        assertTrue(s.contains(a));
        assertEquals(1, s.size());
        
        s = new UrnSet(hash);
        assertTrue(s.contains(a) || s.contains(b));
        assertEquals(1, s.size());
        
        s.clear();
        assertTrue(s.isEmpty());
        
        s.add(a);
        assertTrue(s.remove(a));
        assertTrue(s.isEmpty());
        s.add(a);
        
        Iterator<URN> i = s.iterator();
        assertTrue(i.hasNext());
        try {
            i.remove();
            fail("expected exception");
        } catch(IllegalStateException expected) {}
        assertEquals(a, i.next());
        try {
            i.next();
            fail("expected exception");
        } catch(NoSuchElementException expected) {}
        
        i = s.iterator();
        assertTrue(i.hasNext());
        assertEquals(a, i.next());
        i.remove();
        assertTrue(s.isEmpty());
    }
    
    public void testIteration() throws Exception {
        UrnSet s = new UrnSet();
        URN a = sha1();
        // iterate, remove the ttroot
        s.clear();
        s.add(a);
        URN ttroot = ttroot();
        s.add(ttroot);
        assertEquals(2,s.size());
        Iterator<URN> i = s.iterator();
        assertTrue(i.hasNext());
        assertEquals(a,i.next());
        assertTrue(i.hasNext());
        assertEquals(ttroot,i.next());
        assertFalse(i.hasNext());
        i.remove();
        assertEquals(1,s.size());
        assertTrue(s.contains(a));
        assertFalse(s.contains(ttroot));
        
        // iterate, remove the sha1
        s.clear();
        s.add(a);
        ttroot = ttroot();
        s.add(ttroot);
        assertEquals(2,s.size());
        i = s.iterator();
        assertTrue(i.hasNext());
        assertEquals(a,i.next());
        i.remove();
        assertTrue(i.hasNext());
        assertEquals(ttroot,i.next());
        assertFalse(i.hasNext());
        assertEquals(1,s.size());
        assertFalse(s.contains(a));
        assertTrue(s.contains(ttroot));
    }
    
    public void testContainsAll() throws Exception {
        UrnSet a = new UrnSet();
        UrnSet b = new UrnSet();
        
        URN sha1 = sha1();
        URN ttroot = ttroot();
        a.add(sha1);
        a.add(ttroot);
        b.add(sha1);
        
        assertTrue(a.containsAll(b));
        assertFalse(b.containsAll(a));
        
        Set<URN> regular = new HashSet<URN>();
        regular.add(sha1);
        
        assertTrue(a.containsAll(regular));
        assertTrue(b.containsAll(regular));
        assertTrue(regular.containsAll(b));
        assertFalse(regular.containsAll(a));
        
        regular.add(ttroot);
        assertTrue(a.containsAll(regular));
        assertFalse(b.containsAll(regular));
        assertTrue(regular.containsAll(b));
        assertTrue(regular.containsAll(a));
    }
    
    public void testRemoveAll() throws Exception {
        UrnSet a = new UrnSet();
        
        URN sha1 = sha1();
        URN ttroot = ttroot();
        a.add(sha1);
        a.add(ttroot);
        
        List<URN> l = new ArrayList<URN>();
        l.add(sha1);l.add(sha1);l.add(sha1());l.add(sha1());
        
        assertEquals(2,a.size());
        assertTrue(a.removeAll(l));
        assertEquals(1,a.size());
        assertFalse(a.contains(sha1));
        assertTrue(a.contains(ttroot));
        
        l.add(ttroot);
        a.removeAll(l);
        assertTrue(a.isEmpty());
    }
    
    public void testToArray() throws Exception {
        UrnSet a = new UrnSet();
        
        URN sha1 = sha1();
        URN ttroot = ttroot();
        a.add(sha1);
        a.add(ttroot);
        
        URN [] u = new URN[0];
        u = a.toArray(u);
        assertEquals(2,u.length);
        
        assertSame(u[0], sha1);
        assertSame(u[1], ttroot);
        
        a.remove(ttroot);
        u = a.toArray(u);
        assertEquals(2,u.length);
        
        assertSame(u[0], sha1);
        assertNull(u[1]);
        
        a.add(ttroot);
        a.remove(sha1);
        
        u = a.toArray(u);
        assertEquals(2,u.length);
        assertSame(u[0], ttroot);
        assertNull(u[1]);
        
        u = new URN[10];
        Arrays.fill(u, ttroot);
        a.clear();
        a.add(sha1);
        u = a.toArray(u);
        assertEquals(10,u.length);
        assertSame(sha1,u[0]);
        assertNull(u[1]);
        for (int i = 2; i < 10; i++)
            assertSame(u[i],ttroot);
    }
    
    public void testSerializing() throws Exception {
        Set<URN> s = new UrnSet();
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeObject(s);
        
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(in);
        s = (UrnSet)ois.readObject();
        assertInstanceof(UrnSet.class, s);
        assertTrue(s.isEmpty());
        
        URN a = sha1();
        URN b = ttroot();
        s.add(a);
        s.add(b);
        assertEquals(2, s.size());
        out.reset();
        oos = new ObjectOutputStream(out);
        oos.writeObject(s);
        
        in = new ByteArrayInputStream(out.toByteArray());
        ois = new ObjectInputStream(in);
        s = (UrnSet)ois.readObject();
        assertInstanceof(UrnSet.class, s);
        assertFalse(s.isEmpty());
        assertEquals(2, s.size());
        assertTrue(s.contains(a));
        assertTrue(s.contains(b));
        assertFalse(s.contains(sha1()));
        assertFalse(s.contains(ttroot()));
    }
    
    /** Generates a random sha1. */
    private URN sha1() throws Exception {
        byte[] b = new byte[20];
        RND.nextBytes(b);
        return URN.createSHA1UrnFromBytes(b);
    }
    
    private URN ttroot() throws Exception {
        byte []b = new byte[24];
        RND.nextBytes(b);
        return URN.createTTRootFromBytes(b);
    }
}