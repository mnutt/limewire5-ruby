package com.limegroup.gnutella.connection;

import java.io.IOException;

import org.limewire.io.NetworkInstanceUtils;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;

public class CountingConnection extends BlockingConnection {
    
    public int incomingCount;
    public int incomingFailed;
    public int outgoingCount;
    public int outgoingFailed;

    public boolean countEnabled;


    public CountingConnection(String host, int port, ConnectType connectType,
            CapabilitiesVMFactory capabilitiesVMFactory, SocketsManager socketsManager,
            Acceptor acceptor, MessagesSupportedVendorMessage supportedVendorMessage,
            MessageFactory messageFactory, NetworkManager networkManager,
            NetworkInstanceUtils networkInstanceUtils) {
        super(host, port, ConnectType.PLAIN, capabilitiesVMFactory, socketsManager, acceptor,
                supportedVendorMessage, messageFactory, networkManager, networkInstanceUtils);
        countEnabled = true;
    }

    @Override
    public Message receive() throws IOException, BadPacketException {
        Message m = null;
        try {
            m = super.receive();
        } catch (IOException iox) {
            //System.out.println("IOX");
            if(countEnabled)
                incomingFailed++;
            throw iox;
        } catch (BadPacketException bpe) {
            //System.out.println("BPE");
            if(countEnabled)
                incomingFailed++;
            throw bpe;
        }
        if(countEnabled)
            incomingCount++;
        //System.out.println(""+incomingCount+":"+_socket.getLocalPort()+":"+m);
        return m;
    }
    
    @Override
    public Message receive(int timeout) throws IOException, BadPacketException {
        Message m = null;
        try {
            m = super.receive(timeout);
        } catch (IOException iox) {
            //System.out.println("iox");
            if(countEnabled)
                incomingFailed++;
            throw iox;
        } catch (BadPacketException bpe) {
            //System.out.println("bpe");
            if(countEnabled)
                incomingFailed++;
            throw bpe;
        }  
        if(countEnabled)
            incomingCount++;
        //System.out.println(""+incomingCount+":"+_socket.getLocalPort()+":"+m);
        return m;
    }

    @Override
    public void send(Message m) throws IOException {
        super.send(m);
        if(countEnabled)
            outgoingCount++;
        //System.out.println(""+outgoingCount+": "+m);
    }

}
