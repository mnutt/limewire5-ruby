package org.limewire.facebook.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.friend.impl.AbstractFriend;
import org.limewire.util.StringUtils;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.Inject;

public class FacebookFriend extends AbstractFriend {
    
    private final String id;
    private final JSONObject friend;
    private final Network network;
    private final Map<String, FacebookFriendPresence> presenceMap = new ConcurrentHashMap<String, FacebookFriendPresence>();
    
    private final boolean hasLimeWireAppInstalled;
    private final FeatureRegistry featureRegistry;
    private final FacebookFriendConnection connection;

    
    @Inject
    public FacebookFriend(@Assisted String id, @Assisted JSONObject friend,
                          @Assisted Network network, @Assisted boolean hasLimeWireAppInstalled,
                          @Assisted FacebookFriendConnection connection,
                          FeatureRegistry featureRegistry) {
        this.id = id;
        this.friend = friend;
        this.network = network;
        this.hasLimeWireAppInstalled = hasLimeWireAppInstalled;
        this.featureRegistry = featureRegistry;
        this.connection = connection;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }
    
    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return friend.optString("name", "");
    }

    @Override
    public String getRenderName() {
        return getName();
    }

    @Override
    public String getFirstName() {
        return friend.optString("first_name", "");
    }

    @Override
    public MessageWriter createChat(MessageReader reader) {
        return connection.createChat(id, reader);
    }

    @Override
    public FriendPresence getActivePresence() {
        // ok to return null. fb only allows 1 presence at a time to be logged in
        return null;
    }

    @Override
    public Map<String, FriendPresence> getPresences() {
        return Collections.unmodifiableMap(new HashMap<String, FriendPresence>(presenceMap));
    }

    @Override
    public Network getNetwork() {
        return network;
    }

    @Override
    public boolean hasActivePresence() {
        return false;
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }

    @Override
    public boolean isSignedIn() {
        return !presenceMap.isEmpty();
    }

    @Override
    public boolean isSubscribed() {
        return true;
    }

    @Override
    public void removeChatListener() {
    }

    @Override
    public void setChatListenerIfNecessary(IncomingChatListener listener) {
        connection.setIncomingChatListener(id, listener);
    }

    @Override
    public void setName(String name) {
    }
    
    void addPresence(FacebookFriendPresence presence) {
        presenceMap.put(presence.getPresenceId(), presence);    
    }
    
    void removePresence(FacebookFriendPresence presence) {
        Collection<Feature> features = presence.getFeatures();
        for(Feature feature : features) {
            featureRegistry.get(feature.getID()).removeFeature(presence);
        }
        presenceMap.remove(presence.getPresenceId());
        
    }

    public boolean hasLimeWireAppInstalled() {
        return hasLimeWireAppInstalled;
    }
}
