package org.limewire.ui.swing.friends.chat;

import java.net.URI;
import java.util.Collection;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.xmpp.api.client.XMPPPresence;
import org.limewire.xmpp.api.client.XMPPFriend;

public class MockPresence implements XMPPPresence {
    private String status;
    private final XMPPFriend xmppFriend;
    private Mode mode;
    private String jid;
    private int priority;
    
    MockPresence(XMPPFriend xmppFriend, Mode mode, String status, String jid) {
        this.xmppFriend = xmppFriend;
        this.mode = mode;
        this.status = status;
        this.jid = jid;
        this.priority = 0;
    }

    public XMPPFriend getXMPPFriend() {
        return xmppFriend;
    }

    @Override
    public String getJID() {
        return jid;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public int getPriority() {
        // TODO Auto-generated method stub
        return priority;
    }

    // package private for unit tests
    void setPriority(int priority) {
        this.priority = priority;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Type getType() {
        return Type.available;
    }

    @Override
    public Friend getFriend() {
        return xmppFriend;
    }

    @Override
    public String getPresenceId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<Feature> getFeatures() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Feature getFeature(URI id) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasFeatures(URI... id) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addFeature(Feature feature) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeFeature(URI id) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
