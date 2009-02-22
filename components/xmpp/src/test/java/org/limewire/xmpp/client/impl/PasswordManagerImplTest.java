package org.limewire.xmpp.client.impl;

import java.io.IOException;
import java.util.prefs.Preferences;

import junit.framework.Test;

import org.limewire.security.certificate.CipherProviderImpl;
import org.limewire.util.BaseTestCase;

public class PasswordManagerImplTest extends BaseTestCase {

    private PasswordManagerImpl passwordManagerImpl;

    public PasswordManagerImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PasswordManagerImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        passwordManagerImpl = new PasswordManagerImpl(new CipherProviderImpl());
    }

    public void testLoadPasswordNonExistingPasswordThrows() throws Exception {
        try {
            passwordManagerImpl.loadPassword("mimi");
            fail("io exception expected");
        } catch (IOException ie) {
        }
    }

    public void testPasswordIsStoredInPreferences() throws Exception {
        Preferences preferences = Preferences.userRoot().node(PasswordManagerImpl.PREFERENCES_NODE);
        try {
            passwordManagerImpl.storePassword("mimi", "imim");
            String encrpytedPassword = preferences.get("mimi", null); 
            assertNotNull(encrpytedPassword);
            assertNotEquals("imim", encrpytedPassword);
        } finally {
            // clean up in case of failure
            preferences.remove("mimi");
        }
    }

    public void testRemovePassword() throws IOException {
        Preferences preferences = Preferences.userRoot().node(PasswordManagerImpl.PREFERENCES_NODE);
        try {
            passwordManagerImpl.storePassword("mimi", "imim");
            String encrpytedPassword = preferences.get("mimi", null); 
            assertNotNull(encrpytedPassword);
            passwordManagerImpl.removePassword("mimi");
            assertNull(preferences.get("mimi", null));
        } finally {
            // clean up in case of failure
            preferences.remove("mimi");
        }
    }
    
    public void loadStoredPassword() throws IOException {
        Preferences preferences = Preferences.userRoot().node(PasswordManagerImpl.PREFERENCES_NODE);
        try {
            passwordManagerImpl.storePassword("mimi", "imim");
            assertEquals("imim", passwordManagerImpl.loadPassword("mimi"));
        } finally {
         // clean up in case of failure
            preferences.remove("mimi");
        }
    }
    
    public void testStoreEmptyPasswordThrowsIllegalArgument() throws IOException {
        Preferences preferences = Preferences.userRoot().node(PasswordManagerImpl.PREFERENCES_NODE);
        try {
            passwordManagerImpl.storePassword("mimi", "");
            fail("illegal argument expected");
        } catch (IllegalArgumentException iae) {
        } finally {
            // clean up in case of failure
            preferences.remove("mimi");
        }
    }
    
    public void testStoreEmptyUsernameThrowsIllegalArgument() throws IOException {
        Preferences preferences = Preferences.userRoot().node(PasswordManagerImpl.PREFERENCES_NODE);
        try {
            passwordManagerImpl.storePassword("", "imim");
            fail("illegal argument expected");
        } catch (IllegalArgumentException iae) {
        } finally {
            // clean up in case of failure
            preferences.remove("");
        }
    }

    public void testRemovePasswordWithInvalidUserNameThrowsIllegalArgument() {
        try {
            passwordManagerImpl.removePassword(null);
            fail("IAE expected");
        } catch (IllegalArgumentException iae) {
        }
        try {
            passwordManagerImpl.removePassword("");
            fail("IAE expected");
        } catch (IllegalArgumentException iae) {
        }
    }
}
