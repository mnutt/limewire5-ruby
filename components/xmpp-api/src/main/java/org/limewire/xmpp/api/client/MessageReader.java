package org.limewire.xmpp.api.client;

/**
 * Called by the xmpp service when a chat message is received
 */
public interface MessageReader {
    public void readMessage(String message);

    public void newChatState(ChatState chatState);
}
