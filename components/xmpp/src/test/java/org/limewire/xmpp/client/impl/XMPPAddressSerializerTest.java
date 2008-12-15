package org.limewire.xmpp.client.impl;

import org.limewire.io.GGEP;
import org.limewire.util.BaseTestCase;
import org.limewire.xmpp.api.client.XMPPAddress;

public class XMPPAddressSerializerTest extends BaseTestCase {

    private XMPPAddressSerializer serializer;

    public XMPPAddressSerializerTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        serializer = new XMPPAddressSerializer();
    }

    public void testDeserializeByteArray() throws Exception {
        GGEP ggep = new GGEP();
        ggep.put("JID", "me@you.com/HBEIFDVCER");
        assertEquals(new XMPPAddress("me@you.com/HBEIFDVCER"), serializer.deserialize(ggep.toByteArray()));
        assertEquals("me@you.com/HBEIFDVCER", ((XMPPAddress)serializer.deserialize(ggep.toByteArray())).getFullId());
    }

    public void testSerialize() throws Exception {
        GGEP ggep = new GGEP();
        ggep.put("JID", "me@you.com/helloWorld");
        assertEquals(ggep.toByteArray(), serializer.serialize(new XMPPAddress("me@you.com/helloWorld")));
    }

}
