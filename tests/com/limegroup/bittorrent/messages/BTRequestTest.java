package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import junit.framework.Assert;
import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.limegroup.bittorrent.BTInterval;

public class BTRequestTest extends BaseTestCase {

    public BTRequestTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BTRequestTest.class);
    }

    public void testBTRequest() {
        int startIndex = 0;
        int endIndex = 16;
        int pieceIndex = 0;
        BTInterval btInterval = new BTInterval(startIndex, endIndex, pieceIndex);
        BTRequest btRequest = new BTRequest(btInterval);
        Assert.assertEquals(BTMessage.REQUEST, btRequest.getType());
        Assert.assertEquals(btInterval, btRequest.getInterval());
        Assert.assertTrue(btRequest.isUrgent());
        Assert.assertEquals(12, btRequest.getPayload().limit());
        ByteBuffer testBuffer = ByteBuffer.allocate(12);
        testBuffer.order(ByteOrder.BIG_ENDIAN);
        testBuffer.putInt(pieceIndex);
        testBuffer.putInt(startIndex);
        int length = endIndex - startIndex + 1;
        testBuffer.putInt(length);
        testBuffer.clear();
        Assert.assertEquals(testBuffer, btRequest.getPayload());

    }
}
