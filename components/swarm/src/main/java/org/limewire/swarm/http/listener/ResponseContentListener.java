package org.limewire.swarm.http.listener;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.nio.entity.ContentListener;

/**
 * Extends {@link ContentListener} providing a way to be notified
 * of the {@link HttpResponse} preceding the content.
 */
public interface ResponseContentListener extends ContentListener {

    /**
     * Called with the response preceding the content.
     * @param response the response preceding the content
     * @throws IOException if the listener wants to stop the content
     * from being processed
     */
    void initialize(HttpResponse response) throws IOException;
    
}
