package com.limegroup.gnutella;

import java.net.InetAddress;

public class StubSpamServices implements SpamServices {

    public void adjustSpamFilters() {
    }

    public void reloadIPFilter() {        
    }
    
    public void reloadURNFilter() {
    }

    public void blockHost(String host) {
    }

    public boolean isAllowed(InetAddress host) {
        return true;
    }

    public void unblockHost(String host) {
    }

}
