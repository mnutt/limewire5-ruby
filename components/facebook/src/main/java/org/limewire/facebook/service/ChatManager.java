package org.limewire.facebook.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
            new ConcurrentHashMap<String, MessageReader>();

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
        readers.put(friendId, reader);
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
        return readers.get(friendId);
    }

    /**
     * Sets an incoming chat listener for the friend whose ID is friendId
     * Creates a {@link MessageReader} object wrapping the incoming chat listener.
     *
     * @param friendId ID of the friend.
     * @param listener {@link IncomingChatListener} used to create message readers.
     */
    void setIncomingChatListener(String friendId, IncomingChatListener listener) {
        readers.put(friendId, new MessageReaderIncomingListenerWrapper(friendId, listener));
    }

    /**
     * Stops managing chats for friendId.
     *
     * @param friendId ID of friend
     */
    void removeChat(String friendId) {
        readers.remove(friendId);
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
