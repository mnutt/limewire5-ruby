package org.limewire.http.auth;

import org.limewire.util.BaseTestCase;
import org.apache.http.auth.Credentials;
import junit.framework.Test;

/**
 * Test for AuthenticatorRegistryImpl
 */
public class AuthenticatorRegistryImplTest extends BaseTestCase {

    private AuthenticatorRegistryImpl authenticatorRegistry;
    private Credentials credentials;
    
    public AuthenticatorRegistryImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AuthenticatorRegistryImplTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        authenticatorRegistry = new AuthenticatorRegistryImpl();
        credentials = null;
    }

    public void testAddOneTrueAuthenticator() throws Exception {
        authenticatorRegistry.register(createAuthenticator(true));
        assertTrue(authenticatorRegistry.authenticate(credentials));
    }

    public void testAddOneFalseAuthenticator() throws Exception {
        authenticatorRegistry.register(createAuthenticator(false));
        assertFalse(authenticatorRegistry.authenticate(credentials));
    }

    public void testAddNoAuthenticators() throws Exception {
        assertFalse(authenticatorRegistry.authenticate(credentials));
    }

    public void testAddMultipleAuthenticatorsOneTrue() throws Exception {
        authenticatorRegistry.register(createAuthenticator(false));
        authenticatorRegistry.register(createAuthenticator(false));
        authenticatorRegistry.register(createAuthenticator(false));
        authenticatorRegistry.register(createAuthenticator(true));
        assertTrue(authenticatorRegistry.authenticate(credentials));
    }

    public void testAddMultipleAuthenticatorsAllFalse() throws Exception {
        authenticatorRegistry.register(createAuthenticator(false));
        authenticatorRegistry.register(createAuthenticator(false));
        authenticatorRegistry.register(createAuthenticator(false));
        assertFalse(authenticatorRegistry.authenticate(credentials));
    }

    public void testAddNullAuthenticator() throws Exception {
        Authenticator authen = null;
        try {
            authenticatorRegistry.register(authen);
            fail("Did not get expected NullPointerException when " +
                 "attempting to register null Authenticator");
        } catch (NullPointerException npe) {
            // got expected exception
        }
    }


    private Authenticator createAuthenticator(final boolean authenticateReturnsTrue) {
        return new Authenticator() {

            public void register(AuthenticatorRegistry registry) {
                registry.register(this);
            }

            public boolean authenticate(Credentials credentials) {
                return authenticateReturnsTrue;
            }
        };

    }
}
