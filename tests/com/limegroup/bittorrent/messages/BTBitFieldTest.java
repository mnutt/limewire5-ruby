package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

import junit.framework.Assert;
import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class BTBitFieldTest extends BaseTestCase {

    public BTBitFieldTest(String name) {
        super(name);
    }
    
    public static Test suite() { 
        return buildTestSuite(BTBitFieldTest.class); 
    }

    public void testBitField() {
        byte[] payload = { 1, 2, 3, 4, 5, 6, 7 };
        BTBitField bitField = new BTBitField(ByteBuffer.wrap(payload));
        Assert.assertEquals(BTMessage.BITFIELD, bitField.getType());
        byte[] testPayload = { 1, 2, 3, 4, 5, 6, 7 };
        Assert.assertEquals(ByteBuffer.wrap(testPayload), bitField.getPayload());
        Assert.assertFalse(bitField.isUrgent());
    }

}
