package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import junit.framework.Assert;
import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.limegroup.bittorrent.BTInterval;

public class BTCancelTest extends BaseTestCase {

    public BTCancelTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BTCancelTest.class);
    }

    public void testBTCancel() {
        int startIndex = 0;
        int endIndex = 16;
        int pieceIndex = 0;
        BTInterval btInterval = new BTInterval(startIndex, endIndex, pieceIndex);
        BTCancel btCancel = new BTCancel(btInterval);
        Assert.assertEquals(BTMessage.CANCEL, btCancel.getType());
        Assert.assertEquals(btInterval, btCancel.getInterval());
        Assert.assertFalse(btCancel.isUrgent());
        Assert.assertEquals(12, btCancel.getPayload().limit());
        ByteBuffer testBuffer = ByteBuffer.allocate(12);
        testBuffer.order(ByteOrder.BIG_ENDIAN);
        testBuffer.putInt(pieceIndex);
        testBuffer.putInt(startIndex);
        int length = endIndex - startIndex + 1;
        testBuffer.putInt(length);
        testBuffer.clear();
        Assert.assertEquals(testBuffer, btCancel.getPayload());

    }
}
