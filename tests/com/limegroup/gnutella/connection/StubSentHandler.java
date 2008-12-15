package com.limegroup.gnutella.connection;

import java.util.LinkedList;
import java.util.List;

import com.limegroup.gnutella.messages.Message;

class StubSentHandler implements SentMessageHandler {
    
    private List<Message> SENT = new LinkedList<Message>();
    
    public void processSentMessage(Message m) { SENT.add(m); }
    
    public List<Message> list() { return SENT; }
    
    public Message next() { return SENT.remove(0); }
    
    public int size() { return SENT.size(); }
    
    public void clear() { SENT.clear(); }
    
}