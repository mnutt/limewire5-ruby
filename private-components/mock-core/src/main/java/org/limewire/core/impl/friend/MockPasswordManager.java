package org.limewire.core.impl.friend;

import org.limewire.friend.api.PasswordManager;

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