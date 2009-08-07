package org.limewire.xmpp.client.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.PresenceEvent;
import org.limewire.friend.api.RosterEvent;
import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.friend.impl.util.PresenceUtils;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

public class RosterListenerMock implements EventListener<RosterEvent> {
    
    private static final Log LOG = LogFactory.getLog(RosterListenerMock.class);
    
    private HashMap<String, Friend> users = new HashMap<String, Friend>();
    private HashMap<String, ArrayList<FriendPresence>> roster = new HashMap<String, ArrayList<FriendPresence>>();
    IncomingChatListenerMock listener = new IncomingChatListenerMock();

    @Override
    public void handleEvent(RosterEvent event) {
        for (Friend friend : event.getData()) {
            if(event.getType().equals(RosterEvent.Type.FRIENDS_ADDED)) {
                friendAdded(friend);
            } else if(event.getType().equals(RosterEvent.Type.FRIENDS_DELETED)) {
                friendDeleted(friend.getId());
            } else if(event.getType().equals(RosterEvent.Type.FRIENDS_UPDATED)) {
                friendUpdated(friend);
            }
        }
    }
    
    public synchronized int getRosterSize() {
        return roster.size();
    }
    
    public synchronized String getFirstRosterEntry() {
        return roster.keySet().iterator().next();
    }
    
    public synchronized int countPresences(String username) {
        ArrayList<FriendPresence> presences = roster.get(username);
        return presences == null ? 0 : presences.size();
    }
    
    public synchronized FriendPresence getFirstPresence(String username) {
        ArrayList<FriendPresence> presences = roster.get(username);
        return (presences == null || presences.isEmpty()) ? null : presences.get(0);
    }
    
    public synchronized Friend getUser(String username) {
        return users.get(username);
    }
    
    private synchronized void friendAdded(Friend friend) {
        LOG.debugf("friend added: {0}", friend);
        users.put(friend.getId(), friend);
        if(roster.get(friend.getId()) == null) {
            roster.put(friend.getId(), new ArrayList<FriendPresence>());
        }
        friend.addPresenceListener(new EventListener<PresenceEvent>() {
            public void handleEvent(PresenceEvent event) {
                synchronized (RosterListenerMock.this) {
                    FriendPresence presence = event.getData();
                    String id = PresenceUtils.parseBareAddress(presence.getPresenceId());
                    if(presence.getType().equals(FriendPresence.Type.available)) {
                        if(roster.get(id) == null) {
                            roster.put(id, new ArrayList<FriendPresence>());
                        }
                        if(!contains(roster.get(id), presence.getPresenceId())) {
                            roster.get(id).add(presence);
                            presence.getFriend().setChatListenerIfNecessary(listener);
                            LOG.debugf("presence {0}", presence);
                        } else {
                            replace(roster.get(id), presence);
                        }
                    } else if(presence.getType().equals(FriendPresence.Type.unavailable)) {
                        if(roster.get(id) == null) {
                            roster.put(id, new ArrayList<FriendPresence>());
                        }
                        remove(id, presence);
                    } else {
                        LOG.debugf("user presence changed: {0}", presence.getType());
                    }
                }
            }
        });
    }

    private synchronized void replace(ArrayList<FriendPresence> presences, FriendPresence presence) {
        for(FriendPresence p : presences) {
            if(p.getPresenceId().equals(presence.getPresenceId())) {
                presences.remove(p);
                presences.add(presence);
            }
        }
    }

    private synchronized boolean contains(ArrayList<FriendPresence> presences, String jid) {
        for(FriendPresence presence : presences) {
            if(presence.getPresenceId().equals(jid)) {
                return true;
            }
        }
        return false;
    }

    private synchronized void remove(String id, FriendPresence p) {
        for(FriendPresence presence : roster.get(id)) {
            if(presence.getPresenceId().equals(p.getPresenceId())) {
                roster.get(id).remove(presence);
                if(roster.get(id).size() == 0) {
                    roster.remove(id);
                }
                break;
            }
        }
    }

    private void friendUpdated(Friend friend) {
        LOG.debugf("friend updated: {0}", friend);
    }

    private void friendDeleted(String id) {
        LOG.debugf("friend deleted: {0}", id);
    }

    class FeatureEventListener implements EventListener<FeatureEvent> {
        List<FeatureEvent> featureEvents = new ArrayList<FeatureEvent>();
        public void handleEvent(FeatureEvent event) {
            featureEvents.add(event);
        }
    }
}
