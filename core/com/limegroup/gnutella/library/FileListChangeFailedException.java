package com.limegroup.gnutella.library;

public class FileListChangeFailedException extends Exception {
    
    private final FileListChangedEvent event;
    private final String reason;

    public FileListChangeFailedException(FileListChangedEvent event, String reason) {
        super("Event: " + event + ", Reason: " + reason);
        this.event = event;
        this.reason = reason;
    }
    
    public FileListChangedEvent getEvent() {
        return event;
    }
    
    public String getReason() {
        return reason;
    }
    
}
