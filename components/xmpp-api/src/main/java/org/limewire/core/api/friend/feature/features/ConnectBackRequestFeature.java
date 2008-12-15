package org.limewire.core.api.friend.feature.features;

import java.net.URI;
import java.net.URISyntaxException;

import org.limewire.core.api.friend.feature.Feature;

public class ConnectBackRequestFeature extends Feature<String> {

    public static final URI ID;

    static {
        try {
            ID = new URI("http://www.limewire.org/connect-back-request/2008-12-01");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public ConnectBackRequestFeature() {
        super(ID.toASCIIString(), ID);
    }
}
