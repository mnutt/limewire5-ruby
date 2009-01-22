package org.limewire.ui.swing.friends.chat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

import org.jdesktop.beans.AbstractBean;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.feature.features.LimewireFeature;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.Presence.Mode;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.IncomingChatListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class ChatFriendImpl extends AbstractBean implements ChatFriend {
    private static final Log LOG = LogFactory.getLog(ChatFriendImpl.class);

    private boolean chatting;
    private boolean activeConversation;
    private final User user;
    private String status;
    private Mode mode;
    private long chatStartTime;
    private boolean hasUnviewedMessages;

    ChatFriendImpl(final Presence presence, final String localId) {
        this.user = presence.getUser();
        this.status = presence.getStatus();
        this.mode = presence.getMode();
        this.user.setIncomingChatListener(new IncomingChatListener() {
            public MessageReader incomingChat(MessageWriter writer) {
                LOG.debugf("{0} is typing a message", presence.getJID());
                MessageWriter writerWrapper = new MessageWriterImpl(localId, ChatFriendImpl.this, writer);
                ConversationSelectedEvent event = new ConversationSelectedEvent(ChatFriendImpl.this, writerWrapper, false);
                event.publish();
                //Hang out until a responder has processed this event
                event.await();
                return new MessageReaderImpl(ChatFriendImpl.this);
            }
        });
    }

    @Override
    public Friend getFriend() {
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
    
    @Override
    public void setMode(Mode mode) {
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
    
    @Override
    public void setStatus(String status) {
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
        Presence presence = getBestPresence();
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
    public boolean isReceivingUnviewedMessages() {
        return hasUnviewedMessages;
    }

    @Override
    public void setReceivingUnviewedMessages(boolean hasMessages) {
        boolean oldHasUnviewedMessages = hasUnviewedMessages;
        hasUnviewedMessages = hasMessages;
        firePropertyChange("receivingUnviewedMessages", oldHasUnviewedMessages, hasMessages);
    }

    @Override
    public Presence getBestPresence() {
        if (user.hasActivePresence()) {
            return user.getActivePresence();
        }
        return getHighestPriorityPresence();
    }

    private Presence getHighestPriorityPresence() {
        Collection<Presence> values = this.user.getPresences().values();
        ArrayList<Presence> presences = new ArrayList<Presence>(values);
        Collections.sort(presences, new PresenceSorter());
        return presences.size() == 0 ? null : presences.get(0);
    }
    
    private static class PresenceSorter implements Comparator<Presence> {
        @Override
        public int compare(Presence o1, Presence o2) {
            return new Integer(o1.getPriority()).compareTo(new Integer(o2.getPriority()));
        }
    }
}
