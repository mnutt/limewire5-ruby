package com.limegroup.gnutella.gui.notify;

import org.limewire.service.ErrorService;
import org.limewire.util.OSUtils;

public class Growl {
    
    static {
        if (OSUtils.isMacOSX105()) {
            try {
                System.loadLibrary("GrowlLeopard");
            } catch (UnsatisfiedLinkError err) {
                ErrorService.error(err);
            }
        }
        else if (OSUtils.isAnyMac()) {
            try {
                System.loadLibrary("GrowlTiger");
            } catch (UnsatisfiedLinkError err) {
                ErrorService.error(err);
            }
        }
    }
    
    public Growl() {
        RegisterGrowl();
    }
    
    public void showMessage (String message) {
        SendNotification(message);
    }
    
    private static final native void RegisterGrowl();
    private static final native void SendNotification(String message);
    
}
