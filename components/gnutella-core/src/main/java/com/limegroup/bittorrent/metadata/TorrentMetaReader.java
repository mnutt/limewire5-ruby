package com.limegroup.bittorrent.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import org.limewire.io.IOUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.BTData;
import com.limegroup.bittorrent.BTDataImpl;
import com.limegroup.bittorrent.ValueException;
import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.metadata.MetaDataFactory;
import com.limegroup.gnutella.metadata.MetaReader;

@Singleton
public class TorrentMetaReader implements MetaReader {

    @Override
    public MetaData parse(File torrentFile) throws IOException {
        FileInputStream torrentInputStream = null;
        try {
            torrentInputStream = new FileInputStream(torrentFile);
            Object obj = Token.parse(torrentInputStream.getChannel());
            if (!(obj instanceof Map)) {
                throw new ValueException("expected map");
            }
            
            BTData btData = new BTDataImpl((Map)obj);
            btData.clearPieces(); // save memory
            return new TorrentMetaData(btData);
        } finally {
            IOUtils.close(torrentInputStream);
        }
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "torrent" };
    }
    
    @Inject
    public void register(MetaDataFactory metaDataFactory) {
        metaDataFactory.registerReader(this);
    }

}
