/**
 * 
 */
package org.limewire.xmpp.client.impl;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.api.friend.client.FriendException;
import org.limewire.listener.EventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.DebugRunnable;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

/**
 * Different implementation from {@link org.jivesoftware.smack.ReconnectionManager}
 * to ensure thread safe behavior.
 */
class ReconnectionManager implements EventListener<XMPPConnectionEvent> {

    private static final int MAX_RECONNECTION_ATTEMPTS = 10;
    
    private static final Log LOG = LogFactory.getLog(ReconnectionManager.class);
    
    private final XMPPServiceImpl serviceImpl;

    /**
     * @param serviceImpl
     */
    ReconnectionManager(XMPPServiceImpl serviceImpl) {
        this.serviceImpl = serviceImpl;
    }

    private volatile boolean connected;
    
    @Override
    public void handleEvent(XMPPConnectionEvent event) {
        if(event.getType() == XMPPConnectionEvent.Type.CONNECTED) {
            connected = true;   
        } else if(event.getType() == XMPPConnectionEvent.Type.DISCONNECTED) {
            if(event.getException() != null && connected) {
                XMPPConnection connection = event.getSource();
                final XMPPConnectionConfiguration configuration = connection.getConfiguration();
                synchronized (this.serviceImpl) {
                    this.serviceImpl.connections.remove(connection);
                }
                Thread t = ThreadExecutor.newManagedThread(new DebugRunnable(new Runnable() {
                    @Override
                    public void run() {
                        long sleepTime = 10000;
                        XMPPConnection newConnection = null;
                        for(int i = 0; i < MAX_RECONNECTION_ATTEMPTS &&
                                newConnection == null; i++) {
                            try {
                                LOG.debugf("attempting to reconnect to {0} ..." + configuration.getServiceName());
                                newConnection = serviceImpl.loginImpl(configuration, true);
                            } catch (FriendException e) {
                                // Ignored
                            }
                            try {
                                Thread.sleep(sleepTime);
                            } catch (InterruptedException e) {
                                // Ignored
                            }
                        }
                        LOG.debugf("giving up trying to connect to {0}" + configuration.getServiceName());
                    }
                }), "xmpp-reconnection-manager");
                t.start();
            }
            connected = false;
        }
    }
}