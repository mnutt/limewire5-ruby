package org.limewire.core.api.friend.feature;

import java.net.URI;

public interface FeatureRegistry {
    void add(URI uri, FeatureInitializer featureInitializer);    
}
