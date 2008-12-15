package com.limegroup.gnutella.messages;

import org.limewire.util.ByteUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.library.FileManager;

@Singleton
public class LocalPongInfoImpl implements LocalPongInfo {
    
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<FileManager> fileManager;

    @Inject
    public LocalPongInfoImpl(Provider<ConnectionManager> connectionManager,
            Provider<FileManager> fileManager) {
        this.connectionManager = connectionManager;
        this.fileManager = fileManager;
    }


    /**
     * @return the number of free non-leaf slots available for limewires.
     */
    public byte getNumFreeLimeWireNonLeafSlots() {
        return (byte)connectionManager.get().getNumFreeLimeWireNonLeafSlots();
    }

    /**
     * @return the number of free leaf slots available for limewires.
     */
    public byte getNumFreeLimeWireLeafSlots() {
        return (byte)connectionManager.get().getNumFreeLimeWireLeafSlots();
    }

    public long getNumSharedFiles() {
        return fileManager.get().getGnutellaFileList().size();
    }

    public int getSharedFileSize() {
        return ByteUtils.long2int(fileManager.get().getGnutellaFileList().getNumBytes());
    }

    public boolean isSupernode() {
        return connectionManager.get().isSupernode();
    }
}
