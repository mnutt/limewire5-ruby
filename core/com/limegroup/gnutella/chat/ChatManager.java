package com.limegroup.gnutella.chat;

import java.net.Socket;

import org.limewire.core.settings.ChatSettings;
import org.limewire.io.IOUtils;
import org.limewire.net.ConnectionAcceptor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.SpamServices;

/**
 * This class establishes a connection for a chat, either
 * incoming or outgoing, and also maintains a list of all the 
 * chats currently in progress.
 *
 * @author rsoule
 */
@Singleton
public final class ChatManager implements ConnectionAcceptor {

    private final SpamServices spamServices;
    private final InstantMessengerFactory instantMessengerFactory;
        
    @Inject
	public ChatManager(SpamServices spamServices, InstantMessengerFactory instantMessengerFactory) {
        this.spamServices = spamServices;
        this.instantMessengerFactory = instantMessengerFactory;
    }

    public boolean isBlocking() {
        return false;
    }
    
	/**
     * Invoked by the acceptor to notify this class of a new connection. Accepts
     * the given socket for a one-to-one chat connection, like an instant
     * messenger.
     */
    public void acceptConnection(String word, Socket socket) {
		boolean allowChats = ChatSettings.CHAT_ENABLED.getValue();
		if (!allowChats) {
		    IOUtils.close(socket);
			return;
		}

        if(!spamServices.isAllowed(socket.getInetAddress())) {
            IOUtils.close(socket);
            return;
        }
        
        InstantMessenger im = instantMessengerFactory.createIncomingInstantMessenger(socket);
        im.start();
	}

	/** 
	 * Request a chat connection from the host specified 
	 * returns an uninitialized chat connection.  the callback
	 * will be called when the connection is established or
	 * the connection has died.
	 */
	public InstantMessenger createConnection(String host, int port) {
        InstantMessenger im = instantMessengerFactory.createOutgoingInstantMessenger(host, port);
        return im;
	}

}
