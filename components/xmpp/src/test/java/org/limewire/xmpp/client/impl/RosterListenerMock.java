package org.limewire.xmpp.client.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collection;

import org.jivesoftware.smack.util.StringUtils;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.listener.EventListener;
import org.limewire.xmpp.api.client.XMPPPresence;
import org.limewire.xmpp.api.client.PresenceEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.XMPPFriend;

public class RosterListenerMock implements EventListener<RosterEvent> {
    private HashMap<String, XMPPFriend> users = new HashMap<String, XMPPFriend>();
    private HashMap<String, ArrayList<XMPPPresence>> roster = new HashMap<String, ArrayList<XMPPPresence>>();
    IncomingChatListenerMock listener = new IncomingChatListenerMock();

    @Override
    public void handleEvent(RosterEvent event) {
        Collection<XMPPFriend> users = event.getData();
        for (XMPPFriend user : users) {
            if(event.getType().equals(RosterEvent.Type.FRIENDS_ADDED)) {
                userAdded(user);
            } else if(event.getType().equals(RosterEvent.Type.FRIENDS_DELETED)) {
                userDeleted(user.getId());
            } else if(event.getType().equals(RosterEvent.Type.FRIENDS_UPDATED)) {
                userUpdated(user);
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
        ArrayList<XMPPPresence> presences = roster.get(username);
        return presences == null ? 0 : presences.size();
    }
    
    public synchronized XMPPPresence getFirstPresence(String username) {
        ArrayList<XMPPPresence> presences = roster.get(username);
        return (presences == null || presences.isEmpty()) ? null : presences.get(0);
    }
    
    public synchronized XMPPFriend getUser(String username) {
        return users.get(username);
    }
    
    private synchronized void userAdded(XMPPFriend user) {
        System.out.println("user added: " + user.getId()); 
        users.put(user.getId(), user);
        if(roster.get(user.getId()) == null) {
            roster.put(user.getId(), new ArrayList<XMPPPresence>());
        }
        final String name = user.getName();
        user.addPresenceListener(new EventListener<PresenceEvent>() {
            public void handleEvent(PresenceEvent event) {
                synchronized (RosterListenerMock.this) {
                    XMPPPresence presence = event.getData();
                    String id = StringUtils.parseBareAddress(presence.getJID());
                    if(presence.getType().equals(XMPPPresence.Type.available)) {
                        if(roster.get(id) == null) {
                            roster.put(id, new ArrayList<XMPPPresence>());
                        }
                        if(!contains(roster.get(id), presence.getJID())) {
                            roster.get(id).add(presence);
                            presence.getXMPPFriend().setChatListenerIfNecessary(listener);
                            System.out.println("user " + presence.getJID() + " (" + name + ") available");
                        } else {
                            replace(roster.get(id), presence);
                        }
                    } else if(presence.getType().equals(XMPPPresence.Type.unavailable)) {
                        if(roster.get(id) == null) {
                            roster.put(id, new ArrayList<XMPPPresence>());
                        }
                        remove(id, presence);
                    } else {
                        System.out.println("user presence changed: " + presence.getType());
                    }
                }
            }
        });
    }

    private synchronized void replace(ArrayList<XMPPPresence> presences, XMPPPresence presence) {
        for(XMPPPresence p : presences) {
            if(p.getJID().equals(presence.getJID())) {
                presences.remove(p);
                presences.add(presence);
            }
        }
    }

    private synchronized boolean contains(ArrayList<XMPPPresence> presences, String jid) {
        for(XMPPPresence presence : presences) {
            if(presence.getJID().equals(jid)) {
                return true;
            }
        }
        return false;
    }

    private synchronized void remove(String id, XMPPPresence p) {
        for(XMPPPresence presence : roster.get(id)) {
            if(presence.getJID().equals(p.getJID())) {
                roster.get(id).remove(presence);
                if(roster.get(id).size() == 0) {
                    roster.remove(id);
                }
                break;
            }
        }
    }

    private void userUpdated(XMPPFriend user) {
        System.out.println("user updated: " + user.getId());
    }

    private void userDeleted(String id) {
        System.out.println("user deleted: " +id);
    }

    class FeatureEventListener implements EventListener<FeatureEvent> {
        List<FeatureEvent> featureEvents = new ArrayList<FeatureEvent>();
        public void handleEvent(FeatureEvent event) {
            featureEvents.add(event);
        }
    }
}
