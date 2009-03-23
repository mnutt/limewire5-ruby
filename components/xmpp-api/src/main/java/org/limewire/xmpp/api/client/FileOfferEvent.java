package org.limewire.xmpp.api.client;

import org.limewire.listener.DefaultDataTypeEvent;

public class FileOfferEvent extends DefaultDataTypeEvent<FileOffer, FileOfferEvent.Type> {

    public FileOfferEvent(FileOffer data, Type event) {
        super(data, event);
    }

    public enum Type {OFFER}
}
