package org.limewire.friend.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendManager;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPFriend;
import org.limewire.xmpp.api.client.XMPPPresence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
class FriendListListeners implements FriendManager {
    
    private final EventBroadcaster<FriendEvent> knownBroadcaster;
    private final EventBroadcaster<FriendEvent> availableBroadcaster;
    private final EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster;
    private final ConcurrentMap<String, Friend> knownFriends = new ConcurrentHashMap<String, Friend>();
    private final ConcurrentMap<String, Friend> availFriends = new ConcurrentHashMap<String, Friend>();
    private final PresenceListener presenceListener = new PresenceListener();
    
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
                Collection<XMPPFriend> xmppFriends = event.getData();

                switch(event.getType()) {
                case FRIENDS_ADDED:
                    addFriends(xmppFriends);
                    break;
                case FRIENDS_UPDATED:
                    updateFriends(xmppFriends);
                    break;
                case FRIENDS_DELETED:
                    removeFriends(xmppFriends);
                    break;
                }
            }
        });
        
        connectionListeners.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            public void handleEvent(XMPPConnectionEvent event) {
                switch(event.getType()) {
                case DISCONNECTED:
                    for(XMPPFriend friend : event.getSource().getFriends()) {
                        removeKnownFriend(friend, false);
                    }
                    break;
                }
            }
        });
    }

    @Override
    public FriendPresence getMostRelevantFriendPresence(String id) {
        Friend friend = availFriends.get(id);
        if(friend == null) {
            return null;
        } else {
            Collection<FriendPresence> presences = friend.getFriendPresences().values();
            //TODO: this is not guarenteed to return the correct FriendPresence
            // if the friend is logged in through two LWs with the same ID
            // Not really able to fix this without modifying the Browse/File Request
            for(FriendPresence nextPresence : presences) {
                if(nextPresence.hasFeatures(LimewireFeature.ID)) {
                    return nextPresence;
                }
            }
            return null;
        }
    }
    
    @Override
    public boolean containsAvailableFriend(String id) {
        return availFriends.containsKey(id);
    }

    private void addFriends(Collection<XMPPFriend> friends) {
        for (XMPPFriend friend : friends) {
            if (friend.isSubscribed()) {
                addKnownFriend(friend);
            }
        }
    }

    private void updateFriends(Collection<XMPPFriend> friends) {
        for (XMPPFriend friend : friends) {
            if (friend.isSubscribed()) {
                addKnownFriend(friend);
            } else {
                removeKnownFriend(friend, true);
            }
        }
    }

    private void removeFriends(Collection<XMPPFriend> friends) {
        for (XMPPFriend friend : friends) {
            removeKnownFriend(friend, true);
        }
    }

    private void addKnownFriend(XMPPFriend friend) {
        if (knownFriends.putIfAbsent(friend.getId(), friend) == null) {
            friend.addPresenceListener(presenceListener);
            knownBroadcaster.broadcast(new FriendEvent(friend, FriendEvent.Type.ADDED));
        }
    }
    
    private void removeKnownFriend(XMPPFriend friend, boolean delete) {
        if (knownFriends.remove(friend.getId()) != null) {
            if(delete) {
                knownBroadcaster.broadcast(new FriendEvent(friend, FriendEvent.Type.DELETE));
            }
            knownBroadcaster.broadcast(new FriendEvent(friend, FriendEvent.Type.REMOVED));
        }
    }
    
    private void updatePresence(XMPPPresence presence) {
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.UPDATE));
    }
    
    private void addPresence(XMPPPresence presence) {
        XMPPFriend xmppFriend = presence.getXMPPFriend();
        if(xmppFriend.getPresences().size() == 1) {
            availFriends.put(xmppFriend.getId(), presence.getFriend());
            availableBroadcaster.broadcast(new FriendEvent(presence.getFriend(), FriendEvent.Type.ADDED));
        }
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.ADDED));
    }
    
    private void removePresence(XMPPPresence presence) {
        XMPPFriend xmppFriend = presence.getXMPPFriend();
        if(!xmppFriend.isSignedIn()) {
            availFriends.remove(xmppFriend.getId());
            availableBroadcaster.broadcast(new FriendEvent(presence.getFriend(), FriendEvent.Type.REMOVED));
        }
        friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.REMOVED));
    }

    public Collection<Friend> getKnownFriends() {
        return Collections.unmodifiableCollection(knownFriends.values());        
    }

    Collection<Friend> getAvailableFriends() {
        return Collections.unmodifiableCollection(availFriends.values());
    }
    
    Set<String> getAvailableFriendIds() {
        return Collections.unmodifiableSet(availFriends.keySet());
    }

    private class PresenceListener implements EventListener<PresenceEvent> {
        @Override
        public void handleEvent(PresenceEvent event) {
            switch (event.getData().getType()) {
                case available:
                    switch (event.getType()) {
                        case PRESENCE_NEW:
                            addPresence(event.getData());
                            break;
                        case PRESENCE_UPDATE:
                            updatePresence(event.getData());
                            break;
                    }
                    break;
                case unavailable:
                    removePresence(event.getData());
                    break;
            }
        }
    }
}
