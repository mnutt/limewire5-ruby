package org.limewire.xmpp.client.impl;

import java.util.ArrayList;

import org.limewire.friend.api.ChatState;
import org.limewire.friend.api.MessageReader;

class MessageReaderMock implements MessageReader {
    ArrayList<String> messages = new ArrayList<String>();

    @Override
    public void readMessage(String message) {
        messages.add(message);
    }

    @Override
    public void newChatState(ChatState chatState) {
        // TODO update the UI
    }
    
    @Override
    public void error(String errorMessage) {
        // todo: indicate error message
    }
}
