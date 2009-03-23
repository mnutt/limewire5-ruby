package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import junit.framework.Assert;
import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class BTHaveTest extends BaseTestCase {

    public BTHaveTest(String name) {
        super(name);
    }
    
    
    public static Test suite() {
        return buildTestSuite(BTHaveTest.class);
    }

    public void testBTHave() {
        int pieceNum = 1;
        BTHave btHave = new BTHave(pieceNum);
        Assert.assertEquals(BTMessage.HAVE, btHave.getType());
        Assert.assertEquals(pieceNum, btHave.getPieceNum());
        ByteBuffer testPayload = ByteBuffer.allocate(4);
        testPayload.order(ByteOrder.BIG_ENDIAN);
        testPayload.putInt(pieceNum);
        testPayload.clear();
        Assert.assertEquals(testPayload, btHave.getPayload());
    }

}
