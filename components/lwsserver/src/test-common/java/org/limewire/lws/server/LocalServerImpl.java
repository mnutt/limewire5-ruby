package org.limewire.lws.server;

import java.util.Map;

import org.limewire.lws.server.AbstractReceivesCommandsFromDispatcher;
import org.limewire.lws.server.LWSDispatcherImpl;
import org.limewire.lws.server.LWSSenderOfMessagesToServer;
import org.limewire.lws.server.StringCallback;
import org.limewire.net.SocketsManager;

/**
 * Base class for local servers.
 */
public final class LocalServerImpl extends AbstractServer implements LocalServer {
    
    /** The port on which we'll connect this server. */
    public final static int PORT = 45100;
    
    private final LocalServerDelegate del;

    public LocalServerImpl(SocketsManager socketsManager, String host, int otherPort) {
        super(PORT, "Local Server");
        LWSDispatcherImpl ssd = new LWSDispatcherImpl(new LWSSenderOfMessagesToServer() {
            public void sendMessageToServer(String msg, Map<String, String> args, StringCallback cb) {
                del.sendMessageToServer(msg, args, cb, LocalServerDelegate.WicketStyleURLConstructor.INSTANCE);
            }

        });
        setDispatcher(ssd);
        
        ssd.setCommandReceiver(new AbstractReceivesCommandsFromDispatcher() {
            public String receiveCommand(String cmd, Map<String, String> args) {
                return null;
            }
            
        });
        this.del = new LocalServerDelegate(socketsManager, host, otherPort);
    }
    
    /**
     * We do NOT want the IP of the incoming request to go to our handlers.
     */
    @Override
    protected final boolean sendIPToHandlers() {
        return false;
    }
}
