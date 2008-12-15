package org.limewire.collection;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class NotViewTest extends BaseTestCase {
    private NotView nv;
    private BitField bf;
    
    public NotViewTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(NotViewTest.class);
    }        
    
    @Override
    protected void setUp() throws Exception {

        BitSet bs = new BitSet();
        bs.set(1);
        bs.set(3);
        bs.set(4);
        bs.set(6);
        
        bf = new BitFieldSet(bs, 9); //bf 010110100
        
        nv = new NotView(bf);        //nv 101001011
        
    }
    
    public void testCardinality(){        
        assertEquals(nv.cardinality(), 5);        
    }
    
    public void testGet(){
        
        assertNotSame(nv.get(0), bf.get(0));
        assertNotSame(nv.get(1), bf.get(1));
        assertNotSame(nv.get(2), bf.get(2));
        assertNotSame(nv.get(3), bf.get(3));
        assertNotSame(nv.get(4), bf.get(4));
        assertNotSame(nv.get(5), bf.get(5));
        assertNotSame(nv.get(6), bf.get(6));
        assertNotSame(nv.get(7), bf.get(7));
        assertNotSame(nv.get(8), bf.get(8));          
    }   
    
    public void testMaxSize() {
        assertEquals(9, nv.maxSize());
    }
    
    public void testNextClearBit(){       

        //index 012345678
        //nv  = 101001011 
        assertEquals(nv.nextClearBit(0), 1);        
        assertEquals(nv.nextClearBit(1), 1);
        assertEquals(nv.nextClearBit(2), 3);
        assertEquals(nv.nextClearBit(3), 3);
        assertEquals(nv.nextClearBit(4), 4);
        assertEquals(nv.nextClearBit(5), 6);        
        assertEquals(nv.nextClearBit(6), 6);
        assertEquals(nv.nextClearBit(7), -1);
        assertEquals(nv.nextClearBit(8), -1);            
    }
    
    public void testNextSetBit(){       
        
        //index 012345678
        //nv  = 101001011 
        assertEquals(nv.nextSetBit(0), 0);        
        assertEquals(nv.nextSetBit(1), 2);
        assertEquals(nv.nextSetBit(2), 2);
        assertEquals(nv.nextSetBit(3), 5);
        assertEquals(nv.nextSetBit(4), 5);
        assertEquals(nv.nextSetBit(5), 5);        
        assertEquals(nv.nextSetBit(6), 7);
        assertEquals(nv.nextSetBit(7), 7);
        assertEquals(nv.nextSetBit(8), 8);        
    }

}
