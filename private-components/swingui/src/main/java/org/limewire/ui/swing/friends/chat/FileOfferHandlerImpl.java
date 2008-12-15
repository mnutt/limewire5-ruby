package org.limewire.ui.swing.friends.chat;

import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.ui.swing.friends.chat.Message.Type;
import org.limewire.xmpp.api.client.FileOfferEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Event handler for file offers that are RECEIVED
 */
@Singleton
class FileOfferHandlerImpl implements RegisteringEventListener<FileOfferEvent> {
    
    @Inject
    public void register(ListenerSupport<FileOfferEvent> fileOfferEventListenerSupport) {
        fileOfferEventListenerSupport.addListener(this);
    }

    public void handleEvent(FileOfferEvent event) {
        String fromFriendID = event.getSource().getFromJID();
        new MessageReceivedEvent(new MessageFileOfferImpl(fromFriendID, null, fromFriendID,
                Type.Received, event.getSource().getFile())).publish();
    }
}
