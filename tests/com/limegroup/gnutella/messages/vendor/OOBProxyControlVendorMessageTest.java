package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.limewire.util.BaseTestCase;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.OOBProxyControlVendorMessage.Control;

public class OOBProxyControlVendorMessageTest extends BaseTestCase {

    private MessageFactory messageFactory;

    public OOBProxyControlVendorMessageTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
		Injector injector = LimeTestUtils.createInjector();
		messageFactory = injector.getInstance(MessageFactory.class);
    }
    
    public void testDoNotProxyAtAllMessage() throws Exception {
        OOBProxyControlVendorMessage msg = OOBProxyControlVendorMessage.createDoNotProxyMessage();
        assertEquals(Integer.MAX_VALUE, msg.getMaximumDisabledVersion());
        writeAndReadMessage(msg);
        
        msg = new OOBProxyControlVendorMessage(Control.DISABLE_FOR_ALL_VERSIONS);
        assertEquals(Integer.MAX_VALUE, msg.getMaximumDisabledVersion());
        writeAndReadMessage(msg);
    }
    
    public void testAllowProxyingAllMessage() throws Exception {
        OOBProxyControlVendorMessage msg = OOBProxyControlVendorMessage.createDoProxyMessage();
        assertEquals(0, msg.getMaximumDisabledVersion());
        writeAndReadMessage(msg);
        
        msg = new OOBProxyControlVendorMessage(Control.ENABLE_FOR_ALL_VERSIONS);
        assertEquals(0, msg.getMaximumDisabledVersion());
        writeAndReadMessage(msg);
    }
    
    public void testConcreteMaximumVersionNotAllowed() throws Exception {
        for (Control control : new Control[] { Control.DISABLE_VERSION_1, Control.DISABLE_VERSION_2, Control.DISABLE_VERSION_3, Control.DISABLE_VERSION_255}) {
            OOBProxyControlVendorMessage msg = new OOBProxyControlVendorMessage(control);
            assertEquals(control.getVersion(), msg.getMaximumDisabledVersion());
            writeAndReadMessage(msg);
        }
    }
    
    private OOBProxyControlVendorMessage writeAndReadMessage(OOBProxyControlVendorMessage msg) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        msg.write(out);
        
        Message m = messageFactory.read(new ByteArrayInputStream(out.toByteArray()), Network.TCP);
        assertTrue(m instanceof OOBProxyControlVendorMessage);
        OOBProxyControlVendorMessage read = (OOBProxyControlVendorMessage)m;
        assertEquals(msg.getMaximumDisabledVersion(), read.getMaximumDisabledVersion());
        return read;
    }

}
