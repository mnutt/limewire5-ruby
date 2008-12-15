package org.limewire.lws.server;

import org.limewire.net.ProxySettings;

public class ProxySettingsStub implements ProxySettings {
    
    private ProxyType proxyType = ProxyType.NONE;
    private String proxyHost;
    private String proxyPass;
    private String proxyUser;
    private boolean proxyAuthRequired;
    private boolean proxyForPrivate;
    private int proxyPort;

    public ProxyType getCurrentProxyType() {
        return proxyType;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public String getProxyPassword() {
        return proxyPass;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyUsername() {
        return proxyUser;
    }

    public boolean isProxyAuthenticationRequired() {
        return proxyAuthRequired;
    }

    public boolean isProxyForPrivateEnabled() {
        return proxyForPrivate;
    }

    public void setProxyType(ProxyType proxyType) {
        this.proxyType = proxyType;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPass(String proxyPass) {
        this.proxyPass = proxyPass;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public void setProxyAuthRequired(boolean proxyAuthRequired) {
        this.proxyAuthRequired = proxyAuthRequired;
    }

    public void setProxyForPrivate(boolean proxyForPrivate) {
        this.proxyForPrivate = proxyForPrivate;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

}