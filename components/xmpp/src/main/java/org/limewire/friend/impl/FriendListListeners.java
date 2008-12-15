package org.limewire.friend.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
class FriendListListeners {
    
    private final EventBroadcaster<FriendEvent> knownBroadcaster;
    private final EventBroadcaster<FriendEvent> availableBroadcaster;
    private final EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster;
    private final Map<String, Friend> knownFriends = new ConcurrentHashMap<String, Friend>();
    
    @Inject
    FriendListListeners(@Named("known") EventBroadcaster<FriendEvent> knownBroadcaster,
                   @Named("available") EventBroadcaster<FriendEvent> availableBroadcaster,
                   EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster) {
        this.knownBroadcaster = knownBroadcaster;
        this.availableBroadcaster = availableBroadcaster;
        this.friendPresenceBroadcaster = friendPresenceBroadcaster; 
    }
    
    @Inject void register(ListenerSupport<RosterEvent> rosterListeners,
                          ListenerSupport<XMPPConnectionEvent> connectionListeners) {
        rosterListeners.addListener(new EventListener<RosterEvent>() {
            @Override
            public void handleEvent(RosterEvent event) {
                switch(event.getType()) {
                case USER_ADDED:
                    addKnownFriend(event.getSource());
                    break;
                case USER_DELETED:
                    removeKnownFriend(event.getSource(), true);
                    break;
                }
            }
        });
        
        connectionListeners.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            public void handleEvent(XMPPConnectionEvent event) {
                switch(event.getType()) {
                case DISCONNECTED:
                    for(User user : event.getSource().getUsers()) {
                        removeKnownFriend(user, false);
                    }
                    break;
                }
            }
        });
    }
    
    private void addKnownFriend(User user) {        
        user.addPresenceListener(new EventListener<PresenceEvent>() {
            @Override
            public void handleEvent(PresenceEvent event) {
                switch(event.getSource().getType()) {
                case available:
                    switch(event.getType()) {
                    case PRESENCE_NEW:
                        addPresence(event.getSource());
                        break;
                    case PRESENCE_UPDATE:
                        updatePresence(event.getSource());
                        break;
                    }
                    break;
                case unavailable:
                    removePresence(event.getSource());
                    break;
                }
            }
        });
        
        knownFriends.put(user.getId(), user);        
        knownBroadcaster.broadcast(new FriendEvent(user, FriendEvent.Type.ADDED));
    }
    
    private void removeKnownFriend(User user, boolean delete) {
        knownFriends.remove(user.getId());
        
        if(delete) {
            knownBroadcaster.broadcast(new FriendEvent(user, FriendEvent.Type.DELETE));
        }
        knownBroadcaster.broadcast(new FriendEvent(user, FriendEvent.Type.REMOVED));
    }
    
    private void updatePresence(Presence presence) {
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.UPDATE));
    }
    
    private void addPresence(Presence presence) {
        if(presence.getUser().getPresences().size() == 1) {
            availableBroadcaster.broadcast(new FriendEvent(presence.getFriend(), FriendEvent.Type.ADDED));
        }
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.ADDED));
    }
    
    private void removePresence(Presence presence) {
        if(!presence.getUser().isSignedIn()) {
            availableBroadcaster.broadcast(new FriendEvent(presence.getFriend(), FriendEvent.Type.REMOVED));
        }
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.REMOVED));
    }

    Collection<Friend> getKnownFriends() {
        return Collections.unmodifiableCollection(knownFriends.values());        
    }

}
