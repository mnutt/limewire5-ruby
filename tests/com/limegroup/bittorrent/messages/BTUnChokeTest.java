package com.limegroup.bittorrent.messages;

import java.nio.ByteBuffer;

import junit.framework.Assert;
import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class BTUnChokeTest extends BaseTestCase {

    public BTUnChokeTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BTUnChokeTest.class);
    }

    public void testBTUnChoke() {
        BTUnchoke btUnchoke = BTUnchoke.createMessage();
        Assert.assertEquals(BTMessage.UNCHOKE, btUnchoke.getType());
        ByteBuffer testPayload = ByteBuffer.allocate(0);
        Assert.assertEquals(testPayload, btUnchoke.getPayload());
        Assert.assertFalse(btUnchoke.isUrgent());

    }

}
