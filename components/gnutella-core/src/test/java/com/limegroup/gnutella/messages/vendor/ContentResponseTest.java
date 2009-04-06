package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;

public class ContentResponseTest extends BaseTestCase {

    private MessageFactory messageFactory;

    public ContentResponseTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        messageFactory = LimeTestUtils.createInjector().getInstance(MessageFactory.class);
    }
    
    public static Test suite() {
        return buildTestSuite(ContentResponseTest.class);
    }
    
    public void testUrnIsEncodedAndDecodedProperly() throws Exception {
        URN urn = URN.createSHA1Urn("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        ContentResponse response = new ContentResponse(urn, false);
        response = writeAndRead(response);
        assertFalse(response.getOK());
        assertEquals(urn, response.getURN());
    }
    
    @SuppressWarnings("unchecked")
    private <T extends Message> T writeAndRead(T message) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        message.write(out);
        return (T)messageFactory.read(new ByteArrayInputStream(out.toByteArray()), Network.UDP);
    }

}
