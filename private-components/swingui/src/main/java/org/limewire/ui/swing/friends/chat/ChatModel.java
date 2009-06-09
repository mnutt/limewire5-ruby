package org.limewire.ui.swing.friends.chat;

import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.client.FileMetaData;
import org.limewire.core.api.friend.client.FileOffer;
import org.limewire.core.api.friend.client.FileOfferEvent;
import org.limewire.core.api.friend.client.IncomingChatListener;
import org.limewire.core.api.friend.client.MessageReader;
import org.limewire.core.api.friend.client.MessageWriter;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.friends.chat.Message.Type;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPFriend;
import org.limewire.xmpp.api.client.XMPPPresence;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * General purpose model for the chat window. Keeps track of presences, userId, etc..
 * Most EventBus chat events are now fired from within this class. Prior to firing
 * these events we ensure that the ChatPanel has been constructed. 
 */
@Singleton
public class ChatModel {
	/** Reference to the heavy weight panel */
    private final ChatFramePanel chatFramePanel;
    /** List of friends to chat with */
    private final EventList<ChatFriend> chatFriends;
    /** Mapping of friendId to ChatFriend */
    private final Map<String, ChatFriend> idToFriendMap;
    /** ID user is logged in with */
    private String myId;
    
    @Inject
    public ChatModel(ChatFramePanel chatFramePanel) {
        this.chatFramePanel = chatFramePanel;
        this.chatFriends = new BasicEventList<ChatFriend>();
        this.idToFriendMap = new HashMap<String, ChatFriend>();
    }
    
    /**
	 * Returns an EventList of chatFriends
	 */
    public EventList<ChatFriend> getChatFriendList() {
        return chatFriends;
    }
    
    /** 
	 * Returns the ChatFriend associated with this friendId. 
     * Returns null if no ChatFriend exists for this friendId.
	 */
    public ChatFriend getChatFriend(String friendId) {
        return idToFriendMap.get(friendId);
    }
    
    /**
	 * Removes this chatFriendId.
	 */
    public ChatFriend removeChatFriend(String friendId) {
        return idToFriendMap.remove(friendId);
    }
    
    /**
	 * Returns the id of the user currently signed into chat.
	 */
    public String getLoggedInId() {
        return myId;
    }
    
    @Inject 
    void register(ListenerSupport<XMPPConnectionEvent> connectionSupport,
            ListenerSupport<FriendPresenceEvent> presenceSupport,
            ListenerSupport<FileOfferEvent> fileOfferEventListenerSupport) {
        
        // listen for user login changes
        connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch(event.getType()) {
                case CONNECTED:
                    myId = formatLoggedInName(event.getSource().getConfiguration().getCanonicalizedLocalID());
                    break;
                case DISCONNECTED:
                    myId = null;
                    break;
                }
            }
        });
        
        // listen for presence sign on/off changes
        presenceSupport.addListener(new EventListener<FriendPresenceEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendPresenceEvent event) {
                handlePresenceEvent(event);
            }
        });
        
        // listens for file offers
        fileOfferEventListenerSupport.addListener(new EventListener<FileOfferEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FileOfferEvent event) {
                chatFramePanel.createChatPanel();
                FileOffer fileOfferReceived = event.getData();
                handleIncomingFileOffer(fileOfferReceived.getFile(), fileOfferReceived.getFromJID());
            }
        });
    }
    
    private String formatLoggedInName(String fullLoggedInId) {
        int index = fullLoggedInId.lastIndexOf("@");
        return (index == -1) ? fullLoggedInId : fullLoggedInId.substring(0, index);
    }
    
    /**
	 * Handles an file offer from a presence. Ensures that ChatPanel has been created
	 * prior to firing the MessageRecieved event.
	 */
    private void handleIncomingFileOffer(FileMetaData metadata, String fromJID) {
        int slashIndex = fromJID.indexOf("/");
        String fromFriendId = (slashIndex < 0) ? fromJID : fromJID.substring(0, slashIndex);

        // look up and get chatFriend, and get presence which sent the file offer
        ChatFriend chatFriend = idToFriendMap.get(fromFriendId);

        if (chatFriend != null) {
            Map<String, FriendPresence> presences = chatFriend.getFriend().getFriendPresences();
            FriendPresence fileOfferPresence = presences.get(fromJID);
            if (fileOfferPresence != null) {
                new MessageReceivedEvent(new MessageFileOfferImpl(fromFriendId, fromFriendId,
                        Type.Received, metadata, fileOfferPresence)).publish();
            }
        }
    }
    
    /**
	 * Updates the list of ChatFriends as presences sign on and off.
	 */
    private void handlePresenceEvent(FriendPresenceEvent event) {
        final XMPPPresence presence = (XMPPPresence)event.getData();
        final XMPPFriend user = presence.getXMPPFriend();
        ChatFriend chatFriend = idToFriendMap.get(user.getId());
        switch(event.getType()) {
        case ADDED:
            addFriend(chatFriend, presence);
            break;
        case UPDATE:
            if (chatFriend != null) {
                chatFriend.update();
            }
            break;
        case REMOVED:
            if (chatFriend != null) {
                if (shouldRemoveFromFriendsList(chatFriend)) {
                    chatFriends.remove(idToFriendMap.remove(user.getId()));
                }
                chatFriend.update();
            }
            break;
        }
    }
    
    /**
     * Remove from the friends list only when:
     *
     * 1. The user (buddy) associated with the chatfriend is no longer signed in, AND
     * 2. The chat has been closed (by clicking on the "x" on the friend in the friend's list)
     *
     * @param chatFriend the ChatFriend to decide whether to remove (no null check)
     * @return true if chatFriend should be removed.
     */
    private boolean shouldRemoveFromFriendsList(ChatFriend chatFriend) {
        return (!chatFriend.isChatting()) && (!chatFriend.isSignedIn());
    }
    
    /**
     * Adds a friend to the list of friends that can be chatted with. Also
	 * adds a listener to this friend presence that listens for incoming messages
     * from this presence. 
	 *
	 * This listener ensures that the ChatPanel has been created prior to 
	 * firing a ConversationEvent.
     */
    private void addFriend(ChatFriend chatFriend, final XMPPPresence presence) {
        if(chatFriend == null) {
            chatFriend = new ChatFriendImpl(presence);
            chatFriends.add(chatFriend);
            idToFriendMap.put(presence.getXMPPFriend().getId(), chatFriend);
        }

        final ChatFriend chatFriendForIncomingChat = chatFriend;
        IncomingChatListener incomingChatListener = new IncomingChatListener() {
            public MessageReader incomingChat(MessageWriter writer) {
                chatFramePanel.createChatPanel();
                MessageWriter writerWrapper = new MessageWriterImpl(getLoggedInId(), chatFriendForIncomingChat, writer);
                ConversationSelectedEvent event =
                        new ConversationSelectedEvent(chatFriendForIncomingChat, writerWrapper, false);
                event.publish();
                //Hang out until a responder has processed this event
                event.await();
                return new MessageReaderImpl(chatFriendForIncomingChat);
            }
        };
        presence.getXMPPFriend().setChatListenerIfNecessary(incomingChatListener);
        chatFriend.update();
    }
}
