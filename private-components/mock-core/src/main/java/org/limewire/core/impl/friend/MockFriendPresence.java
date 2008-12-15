package org.limewire.core.impl.friend;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.listener.EventListenerList;

public class MockFriendPresence implements FriendPresence{
    
    private MockFriend friend;
    private Map<URI, Feature> features;
    private EventListenerList<FeatureEvent> featureListeners;
    
    public MockFriendPresence() {
        this(new MockFriend());
    }
    
    public MockFriendPresence(MockFriend friend, Feature...features) {
        this.features = new ConcurrentHashMap<URI, Feature>();
        this.featureListeners = new EventListenerList<FeatureEvent>();
        this.friend = friend;
        for(Feature feature : features) {
            addFeature(feature);
        }
    }
    
    @Override
    public Friend getFriend() {
        return friend;
    }

    @Override
    public String getPresenceId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<Feature> getFeatures() {
        return features.values();
    }

    @Override
    public Feature getFeature(URI id) {
        return features.get(id);
    }

    @Override
    public boolean hasFeatures(URI... id) {
        for(URI uri : id) {
            if(getFeature(uri) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void addFeature(Feature feature) {
        features.put(feature.getID(), feature);
        featureListeners.broadcast(new FeatureEvent(this, FeatureEvent.Type.ADDED, feature));
    }

    @Override
    public void removeFeature(URI id) {
        Feature feature = features.remove(id);
        if(feature != null) {
            featureListeners.broadcast(new FeatureEvent(this, FeatureEvent.Type.REMOVED, feature));
        }
    }
}
