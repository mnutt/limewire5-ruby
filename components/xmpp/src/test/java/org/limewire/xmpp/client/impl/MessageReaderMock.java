package org.limewire.xmpp.client.impl;

import java.util.ArrayList;

import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.ChatState;

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
}
