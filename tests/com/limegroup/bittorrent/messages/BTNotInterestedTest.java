package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

import junit.framework.Assert;
import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class BTNotInterestedTest extends BaseTestCase {

    public BTNotInterestedTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BTNotInterestedTest.class);
    }

    public void testBTNotInterested() {
        BTNotInterested btNotInterested = BTNotInterested.createMessage();
        Assert.assertEquals(BTMessage.NOT_INTERESTED, btNotInterested.getType());
        ByteBuffer testPayload = ByteBuffer.allocate(0);
        Assert.assertEquals(testPayload, btNotInterested.getPayload());
        Assert.assertFalse(btNotInterested.isUrgent());

    }

}
