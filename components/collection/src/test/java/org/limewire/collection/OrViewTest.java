package org.limewire.collection;

import junit.framework.Assert;
import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class OrViewTest extends BaseTestCase {

    public OrViewTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(OrViewTest.class);
    }

    /**
     * 
     * <pre>
     * Output: bf1: 1100 
     *         bf2: 1010 
     *         av:  1110
     * </pre>
     */
    public void testBasic() {
        BitSet bitSet1 = new BitSet(4);
        bitSet1.flip(0);
        bitSet1.flip(1);
        BitSet bitSet2 = new BitSet(4);
        bitSet2.flip(0);
        bitSet2.flip(2);

        BitField bitField1 = new BitFieldSet(bitSet1, 4);
        BitField bitField2 = new BitFieldSet(bitSet2, 4);

        OrView orView = new OrView(bitField1, bitField2);
        Assert.assertEquals(true, orView.get(0));
        Assert.assertEquals(true, orView.get(1));
        Assert.assertEquals(true, orView.get(2));
        Assert.assertEquals(false, orView.get(3));
        
        Assert.assertEquals(0, orView.nextSetBit(0));
        Assert.assertEquals(1, orView.nextSetBit(1));
        Assert.assertEquals(2, orView.nextSetBit(2));
        Assert.assertEquals(-1, orView.nextSetBit(3));
        
        Assert.assertEquals(3, orView.nextClearBit(0));
        Assert.assertEquals(3, orView.nextClearBit(1));
        Assert.assertEquals(3, orView.nextClearBit(2));
        Assert.assertEquals(3, orView.nextClearBit(3));
    }
  
}
