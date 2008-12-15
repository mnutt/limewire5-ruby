package org.limewire.collection;

import junit.framework.Assert;
import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class NorViewTest extends BaseTestCase {

    public NorViewTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(NorViewTest.class);
    }

    /**
     * 
     * <pre>
     * Output: bf1: 1100 
     *         bf2: 1010 
     *         av:  0001
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

        NorView norView = new NorView(bitField1, bitField2);
        Assert.assertEquals(false, norView.get(0));
        Assert.assertEquals(false, norView.get(1));
        Assert.assertEquals(false, norView.get(2));
        Assert.assertEquals(true, norView.get(3));

        Assert.assertEquals(3, norView.nextSetBit(0));
        Assert.assertEquals(3, norView.nextSetBit(1));
        Assert.assertEquals(3, norView.nextSetBit(2));
        Assert.assertEquals(3, norView.nextSetBit(3));

        Assert.assertEquals(0, norView.nextClearBit(0));
        Assert.assertEquals(1, norView.nextClearBit(1));
        Assert.assertEquals(2, norView.nextClearBit(2));
        Assert.assertEquals(-1, norView.nextClearBit(3));
    }

}
