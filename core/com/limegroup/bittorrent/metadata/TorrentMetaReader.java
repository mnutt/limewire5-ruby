package com.limegroup.bittorrent.metadata;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.bittorrent.BTDataImpl;
import com.limegroup.bittorrent.ValueException;
import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.metadata.MetaDataFactory;
import com.limegroup.gnutella.metadata.MetaReader;

@Singleton
public class TorrentMetaReader implements MetaReader {

    @Override
    public MetaData parse(File file) throws IOException {
        byte[] contents = FileUtils.readFileFully(file);
        Object obj = Token.parse(contents);
        if (!(obj instanceof Map)) {
            throw new ValueException("expected map");
        }
        return new TorrentMetaData(new BTDataImpl((Map)obj));
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
