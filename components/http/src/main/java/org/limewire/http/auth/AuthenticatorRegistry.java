package org.limewire.http.auth;

/**
 * Registry for {@link Authenticator authenticators} that
 * lets them register with it.
 */
public interface AuthenticatorRegistry {
    void register(Authenticator authenticator);
}
