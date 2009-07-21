package org.limewire.xmpp.client.impl;

import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;

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
