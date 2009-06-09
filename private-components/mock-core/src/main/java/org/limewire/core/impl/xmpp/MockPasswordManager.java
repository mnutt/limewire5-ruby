package org.limewire.core.impl.xmpp;

import org.limewire.core.api.friend.client.PasswordManager;

class MockPasswordManager implements PasswordManager {
    
    public String loadPassword(String username) {
        return null;
    }
    
    public void storePassword(String username, String password) {
        // Your secret's safe with me
    }
    
    public void removePassword(String username) {
        // Remove what password?
    }
}