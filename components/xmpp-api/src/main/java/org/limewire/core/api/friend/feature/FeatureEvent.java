package org.limewire.core.api.friend.feature;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.listener.DefaultDataEvent;

public class FeatureEvent extends DefaultDataEvent<FriendPresence, FeatureEvent.Type, Feature> {

    public static enum Type {
        ADDED, 
        REMOVED 
    }
    
    public FeatureEvent(FriendPresence source, Type type, Feature feature) {
        super(source, type, feature);
    }
}


