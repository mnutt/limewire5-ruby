package org.limewire.core.api.friend.client;

/**
 * Called when a chat message or new chat state is received
 */
public interface MessageReader {

    /**
     * called when new instant message is received.
     *
     * @param message String representing chat message
     */
    public void readMessage(String message);

    /**
     * called when the chat state is updated.
     *
     * @param chatState {@link ChatState} chat activity
     */
    public void newChatState(ChatState chatState);
}
