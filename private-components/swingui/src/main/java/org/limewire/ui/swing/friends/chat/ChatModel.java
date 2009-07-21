package org.limewire.ui.swing.friends.chat;

import java.util.HashMap;
import java.util.Map;

import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.api.FileOffer;
import org.limewire.friend.api.FileOfferEvent;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.FriendPresenceEvent;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.friends.chat.Message.Type;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

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
    
    @Inject
    public ChatModel(ChatFramePanel chatFramePanel) {
        this.chatFramePanel = chatFramePanel;
        this.chatFriends = new BasicEventList<ChatFriend>();
        this.idToFriendMap = new HashMap<String, ChatFriend>();
    }
    
    /**
     * Returns an EventList of chatFriends.
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
    
    @Inject 
    void register(ListenerSupport<FriendPresenceEvent> presenceSupport,
            ListenerSupport<FileOfferEvent> fileOfferEventListenerSupport) {
        
        
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
            Map<String, FriendPresence> presences = chatFriend.getFriend().getPresences();
            FriendPresence fileOfferPresence = presences.get(fromJID);
            if (fileOfferPresence != null) {
                new MessageReceivedEvent(new MessageFileOfferImpl(fromFriendId, fromFriendId,
                        Type.RECEIVED, metadata, fileOfferPresence)).publish();
            }
        }
    }
    
    /**
     * Updates the list of ChatFriends as presences sign on and off.
     */
    private void handlePresenceEvent(FriendPresenceEvent event) {
        final FriendPresence presence = event.getData();
        final Friend friend = presence.getFriend();
        ChatFriend chatFriend = idToFriendMap.get(friend.getId());
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
                    chatFriends.remove(idToFriendMap.remove(friend.getId()));
                }
                chatFriend.update();
            }
            break;
        }
    }
    
    /**
     * Remove from the friends list only when:
     * <pre>
     * 1. The user (buddy) associated with the chatfriend is no longer signed in, AND
     * 2. The chat has been closed (by clicking on the "x" on the friend in the friend's list)
     * </pre>
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
     * <p>
     * This listener ensures that the ChatPanel has been created prior to 
     * firing a ConversationEvent.
     */
    private void addFriend(ChatFriend chatFriend, final FriendPresence presence) {
        if(chatFriend == null) {
            chatFriend = new ChatFriendImpl(presence);
            chatFriends.add(chatFriend);
            idToFriendMap.put(presence.getFriend().getId(), chatFriend);
        }

        final ChatFriend chatFriendForIncomingChat = chatFriend;
        IncomingChatListener incomingChatListener = new IncomingChatListener() {
            public MessageReader incomingChat(MessageWriter writer) {
                chatFramePanel.createChatPanel();
                MessageWriter writerWrapper = new MessageWriterImpl(chatFriendForIncomingChat, writer);
                ConversationSelectedEvent event =
                        new ConversationSelectedEvent(chatFriendForIncomingChat, writerWrapper, false);
                event.publish();
                //Hang out until a responder has processed this event
                event.await();
                return new MessageReaderImpl(chatFriendForIncomingChat);
            }
        };
        presence.getFriend().setChatListenerIfNecessary(incomingChatListener);
        chatFriend.update();
    }
}
