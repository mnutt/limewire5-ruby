package com.limegroup.gnutella.auth;

import com.limegroup.gnutella.URN;

public class ValidationEvent {
    
    public enum Type { VALID, INVALID, UNKNOWN; }
    
    private final URN urn;
    private final Type type;
    
    public ValidationEvent(Type type, URN urn) {
        this.urn = urn;
        this.type = type;
    }
    
    public URN getUrn() {
        return urn;
    }
    
    public Type getType() {
        return type;
    }

}
