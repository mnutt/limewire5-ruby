package org.limewire.ui.swing.friends.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.jdesktop.beans.AbstractBean;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.Presence.Mode;
import org.limewire.xmpp.api.client.User;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class ChatFriendImpl extends AbstractBean implements ChatFriend {

    private boolean chatting;
    private boolean activeConversation;
    private final User user;
    private String status;
    private Mode mode;
    private long chatStartTime;
    private boolean hasUnviewedMessages;

    ChatFriendImpl(final Presence presence) {
        this.user = presence.getUser();
        this.status = presence.getStatus();
        this.mode = presence.getMode();
    }

    @Override
    public User getUser() {
        return user;
    }
    
    @Override
    public String getID() {
        return user.getId();
    }

    @Override
    public Mode getMode() {
        return mode;
    }
    
    void setMode(Mode mode) {
        Mode oldMode = getMode();
        this.mode = mode;
        firePropertyChange("mode", oldMode, mode);
    }

    @Override
    public String getName() {
        return user.getRenderName();
    }

    @Override
    public String getStatus() {
        return status;
    }
    
    void setStatus(String status) {
        String oldStatus = getStatus();
        this.status = status;
        firePropertyChange("status", oldStatus, status);
    }

    @Override
    public boolean isChatting() {
        return chatting;
    }

    void setChatting(final boolean chatting) {
        final boolean oldChatting = isChatting();
        this.chatting = chatting;
        SwingUtils.invokeLater(new Runnable(){
            public void run() {
                firePropertyChange("chatting", oldChatting, chatting);                
            }
        });
    }

    @Override
    public MessageWriter createChat(MessageReader reader) {
        return user.createChat(reader);
    }

    @Override
    public void startChat() {
        if (isChatting() == false) {
            chatStartTime = System.currentTimeMillis();
            setChatting(true);
        }
    }

    @Override
    public void update() {
        // If there's an available presence, set to "Available"
        // If no available presence, use highest priority presence.
        Presence presence = getPresenceForModeAndStatus();
        if (presence != null) {
            setStatus(presence.getStatus());
            setMode(presence.getMode());
        }
    }

    @Override
    public void stopChat() {
        setChatting(false);
        setActiveConversation(false);
    }

    @Override
    public long getChatStartTime() {
        return chatStartTime;
    }

    @Override
    public boolean isActiveConversation() {
        return activeConversation;
    }
    
    @Override
    public void setActiveConversation(boolean active) {
        boolean oldActiveConversation = activeConversation;
        activeConversation = active;
        firePropertyChange("activeConversation", oldActiveConversation, activeConversation);
    }

    @Override
    public boolean isSignedInToLimewire() {
        for (Presence presence : user.getPresences().values()) {
            if (presence.getFeature(LimewireFeature.ID) != null) {
                return true;
            }
         }
        return false;
    }

    @Override
    public boolean isSignedIn() {
        return user.isSignedIn();
    }

    @Override
    public boolean hasReceivedUnviewedMessages() {
        return hasUnviewedMessages;
    }

    @Override
    public void setReceivedUnviewedMessages(boolean hasMessages) {
        boolean oldHasUnviewedMessages = hasUnviewedMessages;
        hasUnviewedMessages = hasMessages;
        firePropertyChange("receivingUnviewedMessages", oldHasUnviewedMessages, hasMessages);
    }

    private Presence getPresenceForModeAndStatus() {
        ArrayList<Presence> presences = new ArrayList<Presence>(user.getPresences().values());
        Collections.sort(presences, new ModeAndPriorityPresenceComparator());
        return presences.size() == 0 ? null : presences.get(presences.size()-1);
    }
    
    private static class ModeAndPriorityPresenceComparator implements Comparator<Presence> {
        @Override
        public int compare(Presence o1, Presence o2) {
            if (!o1.getMode().equals(o2.getMode())) {
                if (o1.getMode() == Presence.Mode.available) {
                    return 1;
                } else if (o2.getMode() == Presence.Mode.available) {
                    return -1;
                }
            }

            return Integer.valueOf(o1.getPriority()).compareTo(o2.getPriority());
        }
    }
}
