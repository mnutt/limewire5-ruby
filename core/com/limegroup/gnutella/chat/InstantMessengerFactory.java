package com.limegroup.gnutella.chat;

import java.net.Socket;

public interface InstantMessengerFactory {

    public abstract InstantMessenger createIncomingInstantMessenger(Socket socket);

    public abstract InstantMessenger createOutgoingInstantMessenger(String host, int port);

}