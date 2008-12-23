package org.limewire.core.api.friend.feature;

import org.limewire.core.api.friend.FriendPresence;

import com.google.inject.Inject;

public interface FeatureInitializer {
    @Inject
    void register(FeatureRegistry registry);
    void initializeFeature(FriendPresence friendPresence);
    void removeFeature(FriendPresence friendPresence);
}
