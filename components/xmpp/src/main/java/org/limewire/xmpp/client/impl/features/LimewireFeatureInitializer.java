package org.limewire.xmpp.client.impl.features;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.FeatureInitializer;
import org.limewire.core.api.friend.feature.FeatureRegistry;
import org.limewire.core.api.friend.feature.features.LimewireFeature;

public class LimewireFeatureInitializer implements FeatureInitializer{
    @Override
    public void register(FeatureRegistry registry) {
        registry.add(LimewireFeature.ID, this);
    }

    @Override
    public void initializeFeature(FriendPresence friendPresence) {
        friendPresence.addFeature(new LimewireFeature());
    }

    @Override
    public void removeFeature(FriendPresence friendPresence) {
        friendPresence.removeFeature(LimewireFeature.ID);
    }
}
