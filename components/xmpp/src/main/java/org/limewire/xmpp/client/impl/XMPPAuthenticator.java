package org.limewire.xmpp.client.impl;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.auth.Credentials;
import org.limewire.http.auth.Authenticator;
import org.limewire.http.auth.AuthenticatorRegistry;
import org.limewire.security.SHA1;
import org.limewire.security.SecurityUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Handles authentication and creation of user passwords for the xmpp component.
 * 
 * The class is inherently stateless except for a per session random seed.
 */
@Singleton
public class XMPPAuthenticator implements Authenticator {

//    private final static Log LOG = LogFactory.getLog(XMPPAuthenticator.class);
    
    /**
     * Per session random seed.
     */
    private final byte[] seed = new byte[SHA1.HASH_LENGTH];
    
    { 
        SecurityUtils.createSecureRandomNoBlock().nextBytes(seed);
    }
    
    @Override
    @Inject
    public void register(AuthenticatorRegistry registry) {
        registry.register(this);
    }

    /**
     * Returns an base 64 encoded auth token for <code>userId</code>.
     * 
     * @return the ascii-encoded auth token
     */
    public String getAuthToken(String userId) {
        SHA1 sha1 = new SHA1();
        byte[] hash = sha1.digest(StringUtils.toUTF8Bytes(userId));
        for (int i = 0; i < hash.length; i++) {
            hash[i] = (byte)(hash[i] ^ seed[i]);
        }
        sha1.reset();
        // digest again, to make the seed irreconstructible
        return StringUtils.getASCIIString(Base64.encodeBase64(sha1.digest(hash)));
    }

    @Override
    public boolean authenticate(Credentials credentials) {
        // password can be null
        String password = credentials.getPassword();
        if (password == null) {
            return false;
        }
        return password.equals(getAuthToken(credentials.getUserPrincipal().getName()));
    }
}
