package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultEvent;

public class FileOfferEvent extends DefaultEvent<FileOffer, FileOffer.EventType> {

    public FileOfferEvent(FileOffer source, FileOffer.EventType event) {
        super(source, event);
    }
}
