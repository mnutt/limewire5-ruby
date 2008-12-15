package org.limewire.xmpp.client.impl;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.User;

class PresenceImpl implements Presence {

    private final User user;
    private final Map<URI, Feature> features;
    private final EventBroadcaster<FeatureEvent> featureBroadcaster;
    private final String jid;
    
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    private Type type;
    private String status;
    private int priority;
    private Mode mode;
    

    PresenceImpl(org.jivesoftware.smack.packet.Presence presence,
                 User user, EventBroadcaster<FeatureEvent> featureSupport) {
        this.user = user;
        this.features = new ConcurrentHashMap<URI, Feature>();
        this.featureBroadcaster = featureSupport;
        this.jid = presence.getFrom();
        update(presence);
    }

    public void update(org.jivesoftware.smack.packet.Presence presence) {
        rwLock.writeLock().lock();
        try {
            this.type = Type.valueOf(presence.getType().toString());
            this.status = presence.getStatus();
            this.priority = presence.getPriority();
            this.mode = presence.getMode() != null ? Mode.valueOf(presence.getMode().toString()) : Mode.available;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public String getJID() {
        return jid;
    }

    @Override
    public Type getType() {
        rwLock.readLock().lock();
        try {
            return type;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public String getStatus() {
        rwLock.readLock().lock();
        try {
            return status;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public int getPriority() {
        rwLock.readLock().lock();
        try {
            return priority;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Mode getMode() {
        rwLock.readLock().lock();
        try {
            return mode;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public String toString() {
        return getJID() + " for " + user.toString();
    }

    @Override
    public User getUser() {
        return user;
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
    public Friend getFriend() {
        return user;
    }

    @Override
    public String getPresenceId() {
        return getJID();
    }

    @Override
    public void addFeature(Feature feature) {
        features.put(feature.getID(), feature);
        featureBroadcaster.broadcast(new FeatureEvent(this, FeatureEvent.Type.ADDED, feature));
    }

    @Override
    public void removeFeature(URI id) {
        Feature feature = features.remove(id);
        if(feature != null) {
            featureBroadcaster.broadcast(new FeatureEvent(this, FeatureEvent.Type.REMOVED, feature));
        }
    }
}
