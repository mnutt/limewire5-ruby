package com.limegroup.gnutella.stubs;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.vendor.SimppVM;
import com.limegroup.gnutella.messages.vendor.UDPCrawlerPong;
import com.limegroup.gnutella.util.DataUtils;

/**
 * Stub for the <tt>ReplyHandler</tt> interface.
 */
public class ReplyHandlerStub implements ReplyHandler {
    
    public boolean isOpen() {
        return true;
    }
	public void reply(Message m) {
	}
    public void handlePingReply(PingReply pingReply, 
                                ReplyHandler receivingConnection) {
    }
    public void handlePushRequest(PushRequest pushRequest, 
                                  ReplyHandler receivingConnection) {
    }
    public void handleQueryReply(QueryReply queryReply, 
                                 ReplyHandler receivingConnection) {
    }
    public int getNumMessagesReceived() {
        return 0;
    }
    public void countDroppedMessage() {
    }
    public boolean isPersonalSpam(Message m) {
        return false;
    }
    public boolean isOutgoing() {
        return false;
    }
    public boolean isKillable() {
        return false;
    }
    public boolean isSupernodeClientConnection() {
        return false;
    }
    public boolean isLeafConnection() {
        return false;
    }
    public boolean isHighDegreeConnection() {
        return false;
    }
    
    public boolean isUltrapeerQueryRoutingConnection() {
        return false;
    }
    
    public boolean isGoodUltrapeer() {
        return false;
    }

    public boolean isGoodLeaf() {
        return false;
    }

    public boolean supportsPongCaching() {
        return true;
    }

    public boolean allowNewPings() {
        return true;
    }

    public void updatePingTime() {
    }
    
    public InetAddress getInetAddress() {
        try {
            return InetAddress.getByName("30.24.0.5");
        } catch(UnknownHostException e) {
            throw new RuntimeException("impossible!");
        }
    }
    
    public String getAddress() {
        return "30.24.0.5";
    }
    
    public InetSocketAddress getInetSocketAddress() {
        return new InetSocketAddress(getInetAddress(), getPort());
    }
    
    public int getPort() {
        return 6346;
    }

    public void handleSimppVM(SimppVM svm) { }

    public boolean isStable() {
        return true;
    }

    public String getLocalePref() {
        return "en";
    }

    public void handleUDPCrawlerPong(UDPCrawlerPong pong){}

    public byte [] getClientGUID() {
        return DataUtils.EMPTY_GUID;
    }
}








