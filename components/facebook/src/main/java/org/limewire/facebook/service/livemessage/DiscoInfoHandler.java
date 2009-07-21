package org.limewire.facebook.service.livemessage;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.facebook.service.FacebookFriend;
import org.limewire.facebook.service.FacebookFriendConnection;
import org.limewire.facebook.service.FacebookFriendPresence;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.FriendPresenceEvent;
import org.limewire.friend.api.feature.FeatureInitializer;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.friend.impl.util.PresenceUtils;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class DiscoInfoHandler implements LiveMessageHandler {

    private static final Log LOG = LogFactory.getLog(AddressHandler.class);
    
    private static final String REQUEST_TYPE = "disc-info-request";
    private static final String RESPONSE_TYPE = "disc-info-response";
    
    private final FacebookFriendConnection connection;
    private final FeatureRegistry featureRegistry;

    private ListenerSupport<FriendPresenceEvent> availableFriends;
    private EventListener<FriendPresenceEvent> friendPresenceListener;

    @Inject
    DiscoInfoHandler(@Assisted FacebookFriendConnection connection,
            FeatureRegistry featureRegistry) {
        this.connection = connection;
        this.featureRegistry = featureRegistry;
    }

    @Override
    @Inject
    public void register(LiveMessageHandlerRegistry registry) {
        registry.register(REQUEST_TYPE, this);
        registry.register(RESPONSE_TYPE, this);
    }

    @Inject
    public void register(ListenerSupport<FriendPresenceEvent> availableFriends) {
        this.availableFriends = availableFriends;
        this.friendPresenceListener = new EventListener<FriendPresenceEvent>() {
            // listen for known friends instead?
            // would result in exchanging disco-info's faster
            // but also lots of requests to offline friends
            @Override
            public void handleEvent(FriendPresenceEvent event) {
                LOG.debugf("friend presence event: {0}", event);
                if(event.getType() != FriendPresenceEvent.Type.ADDED) {
                    return;
                }
                FriendPresence friendPresence = event.getData();
                if (!(friendPresence instanceof FacebookFriendPresence)) {
                    return;
                }
                FacebookFriend facebookFriend = (FacebookFriend)friendPresence.getFriend();
                if (!facebookFriend.hasLimeWireAppInstalled()) {
                    LOG.debugf("not a limewire friend: {0}", facebookFriend);
                    return;
                }
                connection.sendLiveMessage(friendPresence, REQUEST_TYPE, new HashMap<String, Object>());
            }
        };
        this.availableFriends.addListener(friendPresenceListener);
    }

    public void unregister() {
        if (availableFriends != null) {
            availableFriends.removeListener(friendPresenceListener);
        }
    }

    private void handleDiscInfoResponse(JSONObject message) throws JSONException, URISyntaxException {
        JSONArray features = message.getJSONArray("features");     
        String from = message.getString("from");
        String friendId = PresenceUtils.parseBareAddress(from);
        FacebookFriend friend = connection.getFriend(friendId);
        if (friend == null) {
            LOG.debugf("no friend for id {0}", friendId);
            return;
        }
        FriendPresence presence = friend.getPresences().get(from);
        if(presence != null) {
            initializePresenceFeatures(presence, features);
        } else {
            LOG.debugf("presence offline: {0}", from);
        }
    }
    
    private void handleDiscInfoRequest(JSONObject message) throws JSONException {
        String from = message.getString("from");
        String friendId = PresenceUtils.parseBareAddress(from);
        FacebookFriend friend = connection.getFriend(friendId);
        if (friend == null) {
            LOG.debugf("disc info from non-friend: {0}", friendId);
            return;
        }
        FriendPresence presence = friend.getPresences().get(from);
        if(presence != null) {
            List<String> supported = new ArrayList<String>();
            for(URI feature : featureRegistry.getPublicFeatureUris()) {
                supported.add(feature.toASCIIString());
            }
            Map<String, Object> response = new HashMap<String, Object>();
            response.put("from", connection.getPresenceId());
            response.put("features", supported);
            connection.sendLiveMessage(presence, RESPONSE_TYPE, response);   
        } else {
            LOG.debugf("disc info from non-friend-presence: {0}", from);
        }
    }
    
    @Override
    public void handle(String messageType, JSONObject message) throws JSONException {
        try {
            if(messageType.equals(RESPONSE_TYPE)) {
                handleDiscInfoResponse(message);
            } else if(messageType.equals(REQUEST_TYPE)) {                
                handleDiscInfoRequest(message);
            }
        } catch (URISyntaxException e) {
            throw new JSONException(e);
        }
    }
    
    private void initializePresenceFeatures(FriendPresence presence, JSONArray features) throws JSONException, URISyntaxException {
        for(int i = 0; i < features.length(); i++) {
            String feature = features.getString(i);
            FeatureInitializer initializer = featureRegistry.get(new URI(feature));
            if (initializer != null) {
                initializer.initializeFeature(presence);
            }
        }
    }
}
