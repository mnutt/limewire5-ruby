package com.limegroup.gnutella.messages.vendor;

import java.util.HashSet;
import java.util.Set;

import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage.SupportedMessageBlock;

public class MessagesSupportedVendorMessageStubHelper {

    public static MessagesSupportedVendorMessage makeMSVMWithoutOOBProxyControl() throws Exception {
        Set<SupportedMessageBlock> supportedMessageBlocks = new HashSet<SupportedMessageBlock>();
        MessagesSupportedVendorMessage.addSupportedMessages(supportedMessageBlocks);
        supportedMessageBlocks.remove(new MessagesSupportedVendorMessage.SupportedMessageBlock(VendorMessage.F_LIME_VENDOR_ID, 
                VendorMessage.F_OOB_PROXYING_CONTROL,
                OOBProxyControlVendorMessage.VERSION));
        
        return new MessagesSupportedVendorMessage(supportedMessageBlocks);
    }
    
}
