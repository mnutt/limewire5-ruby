package org.limewire.facebook.service;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;

import org.limewire.friend.api.ChatState;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;

/**
 * Manages message readers ({@link org.limewire.friend.api.MessageReader}) that ChatListener
 * uses to handle incoming instant messages and changing chat states.
 *
 * MessageReaders get added to ChatManager either by directly being
 * added via {@link #addMessageReader}, or by being created via
 * {@link IncomingChatListener#incomingChat(MessageWriter)}.
 *
 */
class ChatManager {

    private final Map<String, MessageReader> readers =
            new HashMap<String, MessageReader>();

    private final FacebookFriendConnection facebookFriendConnection;

    ChatManager(FacebookFriendConnection facebookFriendConnection) {
        this.facebookFriendConnection = facebookFriendConnection;
    }

    /**
     * Associates a friend ID with a MessageReader.  Happens when preparing to
     * initiate a new chat with a friend by sending a chat message.
     *
     * @param friendId ID of friend.
     * @param reader {link @MessageReader} to associate it with.
     * @return MessageWriter interface to use for sending chat messages.
     */
    MessageWriter addMessageReader(String friendId, MessageReader reader) {
        addMessageReaderAndProcess(friendId, reader);       
        return new MessageWriterImpl(friendId);
    }

    /**
     * Gets from existing collection of {@link MessageReader} objects
     * or creates one via the friend's {@link IncomingChatListener}.
     *
     * @param friendId ID of friend
     * @return MessageReader message reader associated with friend.
     */
    MessageReader getMessageReader(String friendId) {
        if (this.facebookFriendConnection.isLoggedIn()) {
            synchronized (readers) {
                MessageReader messageReader = readers.get(friendId);
                if (messageReader == null) {
                    messageReader = new MessageReaderQueued();
                    readers.put(friendId, messageReader);
                }
                return messageReader;
            }
        }
        return null;
    }

    /**
     * Sets an incoming chat listener for the friend whose ID is friendId
     * Creates a {@link MessageReader} object wrapping the incoming chat listener.
     *
     * @param friendId ID of the friend.
     * @param listener {@link IncomingChatListener} used to create message readers.
     */
    void setIncomingChatListener(String friendId, IncomingChatListener listener) {
        MessageReaderIncomingListenerWrapper wrapperReader = 
                new MessageReaderIncomingListenerWrapper(friendId, listener);
        addMessageReaderAndProcess(friendId, wrapperReader);               
    }

    /**
     * Stops managing chats for friendId.
     *
     * @param friendId ID of friend
     */
    void removeChat(String friendId) {
        synchronized (readers) {
            readers.remove(friendId);
        }
    }

    /**
     * Add a MessageReader to readers map.  If a previous MessageReader was in the map,
     * read in the queued up messages (if it is a {@link MessageReaderQueued}).
     * 
     * @param friendId friend id
     * @param reader MessageReader
     */
    private void addMessageReaderAndProcess(String friendId, MessageReader reader) {        
        synchronized (readers) {
            MessageReader prevReader = readers.put(friendId, reader);

            if (prevReader instanceof MessageReaderQueued) {
                MessageReaderQueued msgQ = (MessageReaderQueued) prevReader;
                msgQ.processQueuedMessages(reader);
            }
        } 
    }

    /**
     * {@link MessageReader} wrapper object backed by a MessageReader created
     * via incoming chat (using {@link IncomingChatListener#incomingChat(MessageWriter)}.
     *
     */
    private class MessageReaderIncomingListenerWrapper implements MessageReader {

        private final MessageWriter messageWriter;
        private final IncomingChatListener listener;
        private MessageReader messagereader;
        
        public MessageReaderIncomingListenerWrapper(String friendId, IncomingChatListener listener) {
            this.messageWriter = new MessageWriterImpl(friendId);
            this.listener = listener;
            this.messagereader = null;
        }

        @Override
        public void readMessage(String message) {
            getMessageReader().readMessage(message);
        }

        @Override
        public void newChatState(ChatState chatState) {
            getMessageReader().newChatState(chatState);
        }
        
        @Override
        public void error(String errorMessage) {
            getMessageReader().error(errorMessage);
        }

        private MessageReader getMessageReader() {
            if (messagereader == null) {
                messagereader = listener.incomingChat(messageWriter);
            }
            return messagereader;
        }
    }

    /**
     * A {@link MessageReader} impl that keeps a record of 
     * what is read in, chat states, etc.  The messages will be
     * read in (for example, into the chat window) at a later point in time.
     */
    private class MessageReaderQueued implements MessageReader {
        private final List<ChatActivity> queuedActivities = 
                Collections.synchronizedList(new ArrayList<ChatActivity>());
        
        @Override
        public void readMessage(String message) {
            queuedActivities.add(new MessageActivity(message));
        }

        @Override
        public void newChatState(ChatState chatState) {
            queuedActivities.add(new ChatStateActivity(chatState.toString()));
        }

        @Override
        public void error(String errorMessage) {
            queuedActivities.add(new ErrorActivity(errorMessage));
        }

        public void processQueuedMessages(MessageReader reader) {
            synchronized (queuedActivities) {
                for (ChatActivity msg : queuedActivities) {
                    msg.processActivity(reader);
                }
            }
        }
    }
    
    private abstract class ChatActivity {
        ChatActivity(String text) {
            this.text = text;
        }
        final String text;
        
        abstract void processActivity(MessageReader reader);
    }
    
    private class MessageActivity extends ChatActivity {
        MessageActivity(String text) {
            super(text);
        }
        @Override
        void processActivity(MessageReader reader) {
            reader.readMessage(text);
        }
    }
    private class ChatStateActivity extends ChatActivity {
        ChatStateActivity(String text) {
            super(text);
        }
        @Override
        void processActivity(MessageReader reader) {
            reader.newChatState(ChatState.valueOf(text));
        }
    }
    private class ErrorActivity extends ChatActivity {
        ErrorActivity(String text) {
            super(text);
        }
        @Override
        void processActivity(MessageReader reader) {
            reader.error(text);
        }
    }  

    /**
     * {@link MessageWriter} impl that delegates to the facebook connection
     * to write messages or set chat states.
     */
    private class MessageWriterImpl implements MessageWriter {

        private final String friendId;

        MessageWriterImpl(String friendId) {
            this.friendId = friendId;
        }

        @Override
        public void writeMessage(String message) throws FriendException {
            facebookFriendConnection.sendChatMessage(friendId, message);
        }

        @Override
        public void setChatState(ChatState chatState) throws FriendException {
            facebookFriendConnection.sendChatStateUpdate(friendId, chatState);
        }
    }
}
