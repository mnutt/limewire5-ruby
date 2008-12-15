package org.limewire.collection;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class XorViewTest extends BaseTestCase {

    private XorView xov;

    public XorViewTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(XorViewTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        BitSet bs1 = new BitSet();
        bs1.set(1);
        
        BitSet bs2 = new BitSet();
        bs2.set(2);
        
        BitSet bs3 = new BitSet();
        bs3.set(3);
        
        BitField bf1 = new BitFieldSet(bs1, 5);//bf1: 01000 
        BitField bf2 = new BitFieldSet(bs2, 5);//bf2: 00100 
        BitField bf3 = new BitFieldSet(bs3, 5);//bf3: 00010
               
        xov = new XorView(bf1, bf2, bf3);//xov: 01110    
    }
    
    public void testGet(){
        assertFalse(xov.get(0));
        assertTrue(xov.get(1));
        assertTrue(xov.get(2));
        assertTrue(xov.get(3));
        assertFalse(xov.get(4));
    }
    
    public void testMaxSize() {
        assertEquals(5, xov.maxSize());
    }

}
