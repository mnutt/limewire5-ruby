package com.limegroup.gnutella.connection;

import java.net.Socket;

import org.limewire.io.NetworkInstanceUtils;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;

@Singleton
public class CountingConnectionFactory implements BlockingConnectionFactory {

    private final CapabilitiesVMFactory capabilitiesVMFactory;
    private final Provider<SocketsManager> socketsManager;
    private final Provider<Acceptor> acceptor;
    private final MessagesSupportedVendorMessage supportedVendorMessage;
    private final MessageFactory messageFactory;
    private final NetworkManager networkManager;
    private final NetworkInstanceUtils networkInstanceUtils;
    

    @Inject
    public CountingConnectionFactory(CapabilitiesVMFactory capabilitiesVMFactory,
            Provider<SocketsManager> socketsManager,
            Provider<Acceptor> acceptor,
            MessagesSupportedVendorMessage supportedVendorMessage,
            MessageFactory messageFactory, NetworkManager networkManager,
            NetworkInstanceUtils networkInstanceUtils) {
        this.capabilitiesVMFactory = capabilitiesVMFactory;
        this.socketsManager = socketsManager;
        this.acceptor = acceptor;
        this.supportedVendorMessage = supportedVendorMessage;
        this.messageFactory = messageFactory;
        this.networkManager = networkManager;
        this.networkInstanceUtils = networkInstanceUtils;
    }
    
    public CountingConnection createConnection(Socket socket) {
        throw new UnsupportedOperationException("not implemented");
    }
    
    public CountingConnection createConnection(String host, int port) {
        return createConnection(host, port, ConnectType.PLAIN);
    }

    public CountingConnection createConnection(String host, int port,
            ConnectType connectType) {
        return new CountingConnection(host, port, connectType, capabilitiesVMFactory,
                socketsManager.get(), acceptor.get(), supportedVendorMessage,
                messageFactory, networkManager, networkInstanceUtils);
    }
    
}