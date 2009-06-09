package org.limewire.xmpp.client.impl;

import org.limewire.core.api.friend.client.IncomingChatListener;
import org.limewire.core.api.friend.client.MessageReader;
import org.limewire.core.api.friend.client.MessageWriter;

public class IncomingChatListenerMock implements IncomingChatListener {
    MessageWriter writer;
    MessageReaderMock reader;
    
    public MessageReader incomingChat(MessageWriter writer) {
        System.out.println("new chat");
        this.writer = writer;
        this.reader = new MessageReaderMock();
        return reader;
    }
}
