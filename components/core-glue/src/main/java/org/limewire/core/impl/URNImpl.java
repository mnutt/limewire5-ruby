package org.limewire.core.impl;

import org.limewire.core.api.URN;
import org.limewire.util.Objects;

public class URNImpl implements URN {
    private final com.limegroup.gnutella.URN urn;

    public URNImpl(com.limegroup.gnutella.URN urn) {
        this.urn = Objects.nonNull(urn, "urn");
    }

    public com.limegroup.gnutella.URN getUrn() {
        return urn;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof URNImpl) {
            return urn.equals(((URNImpl) obj).getUrn());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return urn.hashCode();
    }

    @Override
    public String toString() {
        return urn.toString();
    }

    @Override
    public int compareTo(URN o) {
        return toString().compareTo(o.toString());
    }

}
