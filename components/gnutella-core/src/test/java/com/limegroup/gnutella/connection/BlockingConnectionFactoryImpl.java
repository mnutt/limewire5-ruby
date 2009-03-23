package com.limegroup.gnutella.connection;

import java.net.Socket;

import org.limewire.io.NetworkInstanceUtils;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;

public class BlockingConnectionFactoryImpl implements BlockingConnectionFactory {

    private final CapabilitiesVMFactory capabilitiesVMFactory;
    private final Provider<SocketsManager> socketsManager;
    private final Provider<Acceptor> acceptor;
    private final MessagesSupportedVendorMessage supportedVendorMessage;
    private final MessageFactory messageFactory;
    private final NetworkManager networkManager;
    private final NetworkInstanceUtils networkInstanceUtils;
    

    @Inject
    public BlockingConnectionFactoryImpl(CapabilitiesVMFactory capabilitiesVMFactory,
            Provider<SocketsManager> socketsManager, Provider<Acceptor> acceptor,
            MessagesSupportedVendorMessage supportedVendorMessage, MessageFactory messageFactory,
            NetworkManager networkManager, NetworkInstanceUtils networkInstanceUtils) {
        this.capabilitiesVMFactory = capabilitiesVMFactory;
        this.socketsManager = socketsManager;
        this.acceptor = acceptor;
        this.supportedVendorMessage = supportedVendorMessage;
        this.messageFactory = messageFactory;
        this.networkManager = networkManager;
        this.networkInstanceUtils = networkInstanceUtils;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.ConnectionFactory#createConnection(java.net.Socket)
     */
    public BlockingConnection createConnection(Socket socket) {
        return new BlockingConnection(socket, capabilitiesVMFactory, acceptor.get(),
                supportedVendorMessage, messageFactory, networkManager, networkInstanceUtils);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionFactory#createConnection(java.lang.String, int)
     */
    public BlockingConnection createConnection(String host, int port) {
        return createConnection(host, port, ConnectType.PLAIN);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ConnectionFactory#createConnection(java.lang.String, int, com.limegroup.gnutella.util.SocketsManager.ConnectType)
     */
    public BlockingConnection createConnection(String host, int port,
            ConnectType connectType) {
        return new BlockingConnection(host, port, connectType, capabilitiesVMFactory,
                socketsManager.get(), acceptor.get(), supportedVendorMessage,
                messageFactory, networkManager, networkInstanceUtils);
    }

}
