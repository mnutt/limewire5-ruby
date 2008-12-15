package com.limegroup.gnutella.auth;

import java.util.LinkedList;
import java.util.List;

import com.limegroup.gnutella.messages.Message;

@SuppressWarnings("unchecked")
public class StubContentAuthority implements ContentAuthority {

    private List sent = new LinkedList();

    public boolean initialize() {
        return false;
    }

    public void send(Message m) {
        sent.add(m);
    }
    
    public List getSent() {
        return sent;
    }

}
