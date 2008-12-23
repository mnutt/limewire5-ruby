package org.limewire.core.impl.xmpp;

import java.util.Collection;

import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.Presence.Mode;

class MockXMPPConnection2 implements XMPPConnection {

    @Override
    public void addUser(String id, String name) throws XMPPException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public XMPPConnectionConfiguration getConfiguration() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public User getUser(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<User> getUsers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isLoggedIn() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLoggingIn() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void login() throws XMPPException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void logout() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeUser(String id) throws XMPPException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setMode(Mode mode) {
        // TODO Auto-generated method stub
        
    }
}
