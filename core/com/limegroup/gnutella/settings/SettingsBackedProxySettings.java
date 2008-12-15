package com.limegroup.gnutella.settings;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.net.ProxySettings;

import com.google.inject.Singleton;

/**
 * An implementation of {@link ProxySettings} that is based on LimeWire's
 * settings from {@link ConnectionSettings}.
 */
@Singleton
public class SettingsBackedProxySettings implements ProxySettings {

    public ProxyType getCurrentProxyType() {
        switch (ConnectionSettings.CONNECTION_METHOD.getValue()) {
        case ConnectionSettings.C_HTTP_PROXY:
            return ProxyType.HTTP;
        case ConnectionSettings.C_SOCKS4_PROXY:
            return ProxyType.SOCKS4;
        case ConnectionSettings.C_SOCKS5_PROXY:
            return ProxyType.SOCKS5;
        default:
            return ProxyType.NONE;
        }
    }

    public String getProxyPassword() {
        return ConnectionSettings.PROXY_PASS.getValue();
    }

    public String getProxyUsername() {
        return ConnectionSettings.PROXY_USERNAME.getValue();
    }

    public boolean isProxyAuthenticationRequired() {
        return ConnectionSettings.PROXY_AUTHENTICATE.getValue();
    }

    public boolean isProxyForPrivateEnabled() {
        return ConnectionSettings.USE_PROXY_FOR_PRIVATE.getValue();
    }

    public String getProxyHost() {
        return ConnectionSettings.PROXY_HOST.getValue();
    }

    public int getProxyPort() {
        return ConnectionSettings.PROXY_PORT.getValue();
    }

}
