package org.limewire.collection;

import junit.framework.Assert;
import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class NandViewTest extends BaseTestCase {

    public NandViewTest(String name) {
        super(name);
    }
    
    public static Test suite() { 
        return buildTestSuite(NandViewTest.class); 
    } 
     
    /**
     * 
     * <pre>
     * Output: bf1: 1100 
     *         bf2: 1010 
     *         av:  0111
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

        NandView nandView = new NandView(bitField1, bitField2);
        Assert.assertEquals(false, nandView.get(0));
        Assert.assertEquals(true, nandView.get(1));
        Assert.assertEquals(true, nandView.get(2));
        Assert.assertEquals(true, nandView.get(3));

        Assert.assertEquals(1, nandView.nextSetBit(0));
        Assert.assertEquals(1, nandView.nextSetBit(1));
        Assert.assertEquals(2, nandView.nextSetBit(2));
        Assert.assertEquals(3, nandView.nextSetBit(3));

        Assert.assertEquals(0, nandView.nextClearBit(0));
        Assert.assertEquals(-1, nandView.nextClearBit(1));
        Assert.assertEquals(-1, nandView.nextClearBit(2));
        Assert.assertEquals(-1, nandView.nextClearBit(3));
    }

}
