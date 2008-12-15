package org.limewire.xmpp.api.client;

public class FileOffer {
    private final FileMetaData file;
    private final String fromJID;

    public enum EventType {OFFER}
    
    public FileOffer(FileMetaData file, String fromJID) {
        this.file = file;
        this.fromJID = fromJID;
    }

    public FileMetaData getFile() {
        return file;
    }

    public String getFromJID() {
        return fromJID;
    }
}
