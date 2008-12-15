package com.limegroup.gnutella.chat;

import java.net.Socket;

import org.limewire.net.SocketsManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;

@Singleton
public class InstantMessengerFactoryImpl implements InstantMessengerFactory {

    private final Provider<SocketsManager> socketsManager;
    private final Provider<ActivityCallback> activityCallback;

    @Inject
    public InstantMessengerFactoryImpl(Provider<ActivityCallback> activityCallback, Provider<SocketsManager> socketsManager) {
        this.activityCallback = activityCallback;
        this.socketsManager = socketsManager;
    }
    
    public InstantMessenger createIncomingInstantMessenger(Socket socket) {
        return new InstantMessengerImpl(socket, activityCallback.get());
    }

    public InstantMessenger createOutgoingInstantMessenger(String host, int port) {
        return new InstantMessengerImpl(host, port, activityCallback.get(), socketsManager.get());
    }

}
