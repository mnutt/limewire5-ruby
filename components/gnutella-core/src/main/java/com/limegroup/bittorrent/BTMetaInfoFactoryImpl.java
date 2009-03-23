package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.io.InvalidDataException;

import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;

public class BTMetaInfoFactoryImpl implements BTMetaInfoFactory {

    private static final Log LOG = LogFactory.getLog(BTMetaInfoFactory.class);

    @Override
    public BTMetaInfo createBTMetaInfoFromMemento(BTMetaInfoMemento memento)
            throws InvalidDataException {
        return new BTMetaInfoImpl(memento);
    }

    @Override
    public BTMetaInfo createBTMetaInfoFromBytes(ReadableByteChannel torrentByteChannel)
            throws IOException {
        try {
            Object metaInfo = Token.parse(torrentByteChannel);
            if (!(metaInfo instanceof Map))
                throw new ValueException("metaInfo not a Map!");
            return this.createBTMetaInfoFromData(new BTDataImpl((Map) metaInfo));
        } catch (IOException bad) {
            LOG.error("read failed", bad);
            throw bad;
        }
    }

    /**
     * Creates an instance of BTMetaInfo from the bit torrent data passed in
     * 
     * @param data a BTData object for the torrent file
     * @return a new instance of BTMetaInfo
     * @throws IOException if data passed is incorrect
     */
    private BTMetaInfo createBTMetaInfoFromData(BTData data) throws IOException {
        return new BTMetaInfoImpl(data);
    }

    @Override
    public BTMetaInfo createMetaInfo(File torrentFile) throws IOException {
        FileInputStream torrentInputStream = null;
        try {
            torrentInputStream = new FileInputStream(torrentFile);
        return createBTMetaInfoFromBytes(torrentInputStream.getChannel());
        } finally{
            IOUtils.close(torrentInputStream);
        }
    }
}
