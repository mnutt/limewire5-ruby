package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

import junit.framework.Assert;
import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class BTChokeTest extends BaseTestCase {

    public BTChokeTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BTChokeTest.class);
    }

    public void testBTChoke() {
        BTChoke btChoke = BTChoke.createMessage();
        Assert.assertEquals(BTMessage.CHOKE, btChoke.getType());
        ByteBuffer testPayload = ByteBuffer.allocate(0);
        Assert.assertEquals(testPayload, btChoke.getPayload());
        Assert.assertFalse(btChoke.isUrgent());

    }

}
