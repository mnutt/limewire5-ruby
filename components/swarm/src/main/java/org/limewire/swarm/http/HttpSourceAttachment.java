package org.limewire.swarm.http;

import org.limewire.swarm.SwarmSource;
import org.limewire.util.Objects;

/**
 * Container class to attach SwarmSource to selection key.
 * 
 */
class HttpSourceAttachment {
    public final SwarmSource source;

    public HttpSourceAttachment(SwarmSource source) {
        this.source = Objects.nonNull(source, "source");
    }

    public SwarmSource getSource() {
        return source;
    }
}