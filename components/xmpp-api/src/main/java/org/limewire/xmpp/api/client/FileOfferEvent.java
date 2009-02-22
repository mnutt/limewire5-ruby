package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultSourceTypeEvent;

public class FileOfferEvent extends DefaultSourceTypeEvent<FileOffer, FileOffer.EventType> {

    public FileOfferEvent(FileOffer source, FileOffer.EventType event) {
        super(source, event);
    }
}
