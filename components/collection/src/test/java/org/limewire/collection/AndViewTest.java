package org.limewire.collection;

import junit.framework.Test;

import org.limewire.util.AssertComparisons;
import org.limewire.util.BaseTestCase;

public class AndViewTest extends BaseTestCase {

    public AndViewTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AndViewTest.class);
    }

    public void testBasic() {
        BitSet bitSet1 = new BitSet(4);
        bitSet1.flip(0);
        bitSet1.flip(1);
        BitSet bitSet2 = new BitSet(4);
        bitSet2.flip(0);
        bitSet2.flip(2);

        BitField bitField1 = new BitFieldSet(bitSet1, 4);
        BitField bitField2 = new BitFieldSet(bitSet2, 4);

        AndView andView = new AndView(bitField1, bitField2);
        AssertComparisons.assertEquals(true, andView.get(0));
        AssertComparisons.assertEquals(false, andView.get(1));
        AssertComparisons.assertEquals(false, andView.get(2));
        AssertComparisons.assertEquals(false, andView.get(3));
        
        AssertComparisons.assertEquals(0, andView.nextSetBit(0));
        AssertComparisons.assertEquals(-1, andView.nextSetBit(1));
        AssertComparisons.assertEquals(-1, andView.nextSetBit(2));
        AssertComparisons.assertEquals(-1, andView.nextSetBit(3));
        
        AssertComparisons.assertEquals(1, andView.nextClearBit(0));
        AssertComparisons.assertEquals(1, andView.nextClearBit(1));
        AssertComparisons.assertEquals(2, andView.nextClearBit(2));
        AssertComparisons.assertEquals(3, andView.nextClearBit(3));
    }
  
}
