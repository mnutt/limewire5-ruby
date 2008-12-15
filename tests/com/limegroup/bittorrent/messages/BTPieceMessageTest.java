package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import junit.framework.Assert;
import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.limegroup.bittorrent.BTInterval;

public class BTPieceMessageTest extends BaseTestCase {

    public BTPieceMessageTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BTPieceMessageTest.class);
    }

    public void testPieceMessage() {

        int startIndex = 0;
        int endIndex = 16;
        int pieceIndex = 1;
        BTInterval btInterval = new BTInterval(startIndex, endIndex, pieceIndex);

        byte[] data = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17 };
        BTPieceMessage btPieceMessage = new BTPieceMessage(btInterval, data);
        Assert.assertEquals(BTMessage.PIECE, btPieceMessage.getType());
        Assert.assertTrue(btPieceMessage.isUrgent());

        ByteBuffer testPayload = ByteBuffer.allocate(8 + data.length);
        testPayload.order(ByteOrder.BIG_ENDIAN);
        testPayload.putInt(pieceIndex);
        testPayload.putInt(startIndex);
        testPayload.put(data);
        testPayload.clear();
        Assert.assertEquals(testPayload, btPieceMessage.getPayload());

    }

}
