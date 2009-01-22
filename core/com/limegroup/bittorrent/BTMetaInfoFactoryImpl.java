package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.InvalidDataException;
import org.limewire.util.FileUtils;

import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;

public class BTMetaInfoFactoryImpl implements BTMetaInfoFactory {
        
    private static final Log LOG = LogFactory.getLog(BTMetaInfoFactory.class);
    
    /* (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfoFactory#create(com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento)
     */
    public BTMetaInfo createBTMetaInfoFromMemento (BTMetaInfoMemento memento) throws InvalidDataException{
        return new BTMetaInfoImpl(memento);
    }
    
    /*
     * (non-Javadoc)
     * @see com.limegroup.bittorrent.BTMetaInfoFactory#createBTMetaInfoFromBytes(byte[])
     */
    public BTMetaInfo createBTMetaInfoFromBytes(byte []torrent) throws IOException {
        try {
            Object metaInfo = Token.parse(torrent);
            if(!(metaInfo instanceof Map))
                throw new ValueException("metaInfo not a Map!");
            return this.createBTMetaInfoFromData(new BTDataImpl((Map)metaInfo));
        } catch (IOException bad) {
            LOG.error("read failed", bad);
            throw bad;
        }
    }
    
    /**
     * Creates an instance of BTMetaInfo from the bit torrent data passed in
     * @param data a BTData object for the torrent file
     * @return a new instance of BTMetaInfo
     * @throws IOException if data passed is incorrect
     */
    private BTMetaInfo createBTMetaInfoFromData (BTData data) throws IOException {
        return new BTMetaInfoImpl(data);
    }

    @Override
    public BTMetaInfo createMetaInfo(File torrentFile) throws IOException {
        byte[] torrentBytes = FileUtils.readFileFully(torrentFile);
        if(torrentBytes == null) {
            throw new IOException("Error reading torrent file: " + torrentFile);
        }
        return createBTMetaInfoFromBytes(torrentBytes);
    }
}
