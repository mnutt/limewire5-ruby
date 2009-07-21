package org.limewire.friend.api;

public interface Network {
    public enum Type {XMPP, FACEBOOK}
    public String getCanonicalizedLocalID();    
    public String getNetworkName();
    public Type getType();
}
