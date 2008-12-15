package org.limewire.xmpp.client.impl;

import org.limewire.xmpp.api.client.IncomingChatListener;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;

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
