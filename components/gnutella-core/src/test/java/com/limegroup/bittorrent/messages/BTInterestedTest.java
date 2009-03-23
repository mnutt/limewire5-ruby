package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

import junit.framework.Assert;
import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class BTInterestedTest extends BaseTestCase {

    public BTInterestedTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BTInterestedTest.class);
    }

    public void testBTInterested() {
        BTInterested btInterested = BTInterested.createMessage();
        Assert.assertEquals(BTMessage.INTERESTED, btInterested.getType());
        ByteBuffer testPayload = ByteBuffer.allocate(0);
        Assert.assertEquals(testPayload, btInterested.getPayload());
        Assert.assertTrue(btInterested.isUrgent());

    }

}
