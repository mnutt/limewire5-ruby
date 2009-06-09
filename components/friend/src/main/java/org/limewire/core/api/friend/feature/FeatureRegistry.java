package org.limewire.core.api.friend.feature;

import java.net.URI;

public interface FeatureRegistry {
    /**
     * Add to the feature registry.
     *
     * @param uri identifies the feature being added.
     * @param featureInitializer  the means to initialize the feature when needed.
     * @param external true if this feature is to be broadcast to other presences.
     * (i.e. other presences know that the current login's presence has this feature)
     * False otherwise.
     */
    void add(URI uri, FeatureInitializer featureInitializer, boolean external);

    /**
     * Retrieve the {@link FeatureInitializer} based on the identifying URI.
     *
     * @param uri identifies the feature being retrieved
     * @return FeatureInitializer desired
     */
    FeatureInitializer get(URI uri);
}
