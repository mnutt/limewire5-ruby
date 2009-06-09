package org.limewire.core.api.friend.client;

public class FileOffer {
    private final FileMetaData file;
    private final String fromJID;

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
