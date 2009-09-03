package com.limegroup.gnutella;

import java.net.InetAddress;

import com.limegroup.gnutella.messages.Message;

public class StubSpamServices implements SpamServices {

    public void adjustSpamFilters() {
    }

    public void reloadIPFilter() {        
    }

    public void reloadSpamFilters() {
    }

    public void blockHost(String host) {
    }

    public boolean isAllowed(InetAddress host) {
        return true;
    }

    public void unblockHost(String host) {
    }

    public boolean isPersonalSpam(Message m) {
        return false;
    }

    public boolean isRouteSpam(Message m) {
        return false;
    }
}
