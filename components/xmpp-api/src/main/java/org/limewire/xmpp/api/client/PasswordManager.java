package org.limewire.xmpp.api.client;

import java.io.IOException;

/**
 * Defines an interface for saving and loading passwords.
 */
public interface PasswordManager {
    public String loadPassword(String username) throws IOException;
    public void storePassword(String username, String password) throws IOException;
    public void removePassword(String username);
}
