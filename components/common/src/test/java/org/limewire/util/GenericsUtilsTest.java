package org.limewire.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.limewire.util.GenericsUtils.ScanMode;

public class GenericsUtilsTest extends TestCase {
    
    // TODO: test maps
    private static enum Kind { COLLECTION, LIST, SET }
    private static enum Type { NORMAL, HAS_EXTRA, EMPTY }
    
    public GenericsUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        for(Kind kind : Kind.values()) {
            for(ScanMode mode : ScanMode.values()) {
                for(Type type : Type.values()) {
                    suite.addTest(newTest(kind, mode, type));
                }
            }
        }
        return suite;
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    private static Test newTest(final Kind kind, final ScanMode scanMode, final Type type) {
        return new TestCase("Test for: " + kind + ", mode: " + scanMode + ", of type: " + type) {
            @Override
            protected void runTest() throws Throwable {
                try {
                    switch(kind) {
                    case COLLECTION:
                        if(scanMode != ScanMode.NEW_COPY_REMOVED) {
                            scanResult(GenericsUtils.scanForCollection(listFor(type), Number.class, scanMode), type);
                        } else {
                            scanResult(GenericsUtils.scanForCollection(listFor(type), Number.class, scanMode, TypeSafeNumberList.class), type);
                        }
                        break;
                    case LIST:
                        if(scanMode != ScanMode.NEW_COPY_REMOVED) {
                            scanResult(GenericsUtils.scanForList(listFor(type), Number.class, scanMode), type);
                        } else {
                            scanResult(GenericsUtils.scanForList(listFor(type), Number.class, scanMode, TypeSafeNumberList.class), type);
                        }
                        break;
                    case SET:
                        if(scanMode != ScanMode.NEW_COPY_REMOVED) {
                            scanResult(GenericsUtils.scanForSet(setFor(type), Number.class, scanMode), type);
                        } else {
                            scanResult(GenericsUtils.scanForSet(setFor(type), Number.class, scanMode, TypeSafeNumberSet.class), type);
                        }
                        break;
                    }
                } catch(ClassCastException cce) {
                    if(scanMode != ScanMode.EXCEPTION) {
                        throw cce;
                    }
                    assertEquals("wanted an instanceof: class java.lang.Number, but was: [NotANumber] of type: java.lang.String", cce.getMessage());
                }
            }
        };
    }
    
    private static void scanResult(Collection result, Type type) {
        switch(type) {
        case EMPTY:
            assertEquals(0, result.size());
            break;
        case HAS_EXTRA:
            assertEquals(3, result.size());
            if(result instanceof Set) {
                assertTrue(result.contains(1));
                assertTrue(result.contains(2));
                assertTrue(result.contains(4));
            } else {
                Iterator i = result.iterator();
                assertEquals(1, i.next());
                assertEquals(2, i.next());
                assertEquals(4, i.next());
                assertFalse(i.hasNext());
            }
            break;
        case NORMAL:
            assertEquals(4, result.size());
            if(result instanceof Set) {
                assertTrue(result.contains(1));
                assertTrue(result.contains(2));
                assertTrue(result.contains(3));
                assertTrue(result.contains(4));
            } else {
                Iterator i = result.iterator();
                assertEquals(1, i.next());
                assertEquals(2, i.next());
                assertEquals(3, i.next());
                assertEquals(4, i.next());
                assertFalse(i.hasNext());
            }
            break;
        }
    }
    
    @SuppressWarnings("unchecked")
    private static List listFor(Type type) {
        ArrayList l = new ArrayList();
        switch(type) {
        case EMPTY:
            break;
        case HAS_EXTRA:
            l.add(1); l.add(2); l.add("NotANumber"); l.add(4);
            break;
        case NORMAL:
            l.add(1); l.add(2); l.add(3); l.add(4); 
            break;
        }
        return l;
    }
    
    
    @SuppressWarnings("unchecked")
    private static Set setFor(Type type) {
        HashSet l = new HashSet();
        switch(type) {
        case EMPTY:
            break;
        case HAS_EXTRA:
            l.add(1); l.add(2); l.add("NotANumber"); l.add(4);
            break;
        case NORMAL:
            l.add(1); l.add(2); l.add(3); l.add(4); 
            break;
        }
        return l;
    }
        
    
    public static class TypeSafeNumberList extends ArrayList<Number> {
        @Override
        public boolean add(Number e) {
            if (!Number.class.isInstance(e)) {
                throw new IllegalStateException();
            }
            return super.add(e);
        }
        
        @Override
        public void add(int index, Number element) {
            if (!Number.class.isInstance(element)) {
                throw new IllegalStateException();
            }
            super.add(index, element);
        }
        
        @Override
        public boolean addAll(Collection<? extends Number> c) {
            boolean added = false;
            for(Number n : c) {
                added |= add(n);
            }
            return added;
        }
    }
    
    public static class TypeSafeNumberSet extends HashSet<Number> {
        @Override
        public boolean add(Number e) {
            if (!Number.class.isInstance(e)) {
                throw new IllegalStateException();
            }
            return super.add(e);
        }
        
        @Override
        public boolean addAll(Collection<? extends Number> c) {
            boolean added = false;
            for(Number n : c) {
                added |= add(n);
            }
            return added;
        }       
        
    }
}


