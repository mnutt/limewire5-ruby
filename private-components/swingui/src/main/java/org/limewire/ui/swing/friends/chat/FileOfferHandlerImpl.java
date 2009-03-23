package org.limewire.ui.swing.friends.chat;

import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.xmpp.api.client.FileOfferEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Event handler for file offers that are RECEIVED
 * TODO: Get rid of this class.  logic already moved inside ChatFriendListPane.
 */
@Singleton
class FileOfferHandlerImpl implements RegisteringEventListener<FileOfferEvent> {
    
    @Inject
    public void register(ListenerSupport<FileOfferEvent> fileOfferEventListenerSupport) {
        fileOfferEventListenerSupport.addListener(this);
    }

    public void handleEvent(FileOfferEvent event) {

    }
}
