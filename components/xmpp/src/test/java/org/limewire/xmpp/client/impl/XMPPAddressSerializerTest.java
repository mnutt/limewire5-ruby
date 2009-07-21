package org.limewire.xmpp.client.impl;

import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.friend.impl.address.FriendAddressSerializer;
import org.limewire.io.GGEP;
import org.limewire.util.BaseTestCase;

public class XMPPAddressSerializerTest extends BaseTestCase {

    private FriendAddressSerializer serializer;

    public XMPPAddressSerializerTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        serializer = new FriendAddressSerializer();
    }

    public void testDeserializeByteArray() throws Exception {
        GGEP ggep = new GGEP();
        ggep.put("JID", "me@you.com/HBEIFDVCER");
        assertEquals(new FriendAddress("me@you.com/HBEIFDVCER"), serializer.deserialize(ggep.toByteArray()));
        assertEquals("me@you.com/HBEIFDVCER", ((FriendAddress)serializer.deserialize(ggep.toByteArray())).getFullId());
    }

    public void testSerialize() throws Exception {
        GGEP ggep = new GGEP();
        ggep.put("JID", "me@you.com/helloWorld");
        assertEquals(ggep.toByteArray(), serializer.serialize(new FriendAddress("me@you.com/helloWorld")));
    }

}
