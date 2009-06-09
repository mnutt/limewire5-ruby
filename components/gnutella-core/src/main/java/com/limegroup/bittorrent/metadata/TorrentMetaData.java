package com.limegroup.bittorrent.metadata;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.BTData.BTFileData;
import org.limewire.util.Base32;
import org.limewire.util.NameValue;
import org.limewire.util.StringUtils;
import org.limewire.util.URIUtils;

import com.limegroup.gnutella.metadata.MetaData;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * Allows accessing metadata about the torrent through the MetaData interface. 
 */
public class TorrentMetaData implements MetaData {

    public static final String TORRENT_SCHEMA = "http://www.limewire.com/schemas/torrent.xsd";
    
    public static final String INFO_HASH = "torrents__torrent__infohash__";
    
    public static final String ANNOUNCE = "torrents__torrent__announce__";
    
    public static final String LENGTH = "torrents__torrent__length__";
    
    public static final String NAME = "torrents__torrent__name__";
    
    public static final String PRIVATE = "torrents__torrent__private__";
    
    public static final String PIECE_LENGTH = "torrents__torrent__piecelength__";
    
    public static final String WEBSEEDS = "torrents__torrent__webseeds__";
    
    public static final String FILE_PATHS = "torrents__torrent__filepaths__";
    
    public static final String FILE_LENGTHS = "torrents__torrent__filelengths__";

    private List<NameValue<String>> nameValues;

    public TorrentMetaData(BTData data) throws IOException {
        nameValues = Collections.unmodifiableList(buildNameValueList(data));
    }

    @Override
    public String getSchemaURI() {
        return TORRENT_SCHEMA;
    }

    @Override
    public void populate(LimeXMLDocument doc) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    private List<NameValue<String>> buildNameValueList(BTData data) throws IOException {
        NameValueListBuilder builder = new NameValueListBuilder();
        builder.add(INFO_HASH, Base32.encode(data.getInfoHash()));
        try {
            builder.add(ANNOUNCE, URIUtils.toURI(data.getAnnounce()).toASCIIString());
        } catch (URISyntaxException ie) {
            throw new IOException(ie);
        }
        Long length = data.getLength();
        if (length != null) {
            builder.add(LENGTH, length);
        }
        builder.add(NAME, data.getName());
        // unimportant information not parsed and sent for now
//        builder.add(PIECE_LENGTH, data.getPieceLength());
        boolean isPrivate = data.isPrivate();
        if (isPrivate) {
            builder.add(PRIVATE, Boolean.TRUE.toString());
        }
        String uris = StringUtils.explode(data.getWebSeeds(), "\t");
        if (uris.length() > 0) {
            builder.add(WEBSEEDS, uris);
        }
        List<BTFileData> files = data.getFiles();
        if (files != null) {
            List<String> filePaths = new ArrayList<String>(files.size());
            List<Long> fileLengths = new ArrayList<Long>(files.size());
            for (BTFileData file : files) {
                try {
                    filePaths.add(URIUtils.toURI(file.getPath()).toASCIIString());
                } catch (URISyntaxException e) {
                    throw new IOException(e);
                }
                fileLengths.add(file.getLength());
            }
            builder.add(FILE_PATHS, StringUtils.explode(filePaths, "\t"));
            builder.add(FILE_LENGTHS, StringUtils.explode(fileLengths, "\t"));
        }
        return builder.toList();
    }
    
    @Override
    public List<NameValue<String>> toNameValueList() {
        return nameValues;
    }

    private static class NameValueListBuilder {

        private List<NameValue<String>> values = new ArrayList<NameValue<String>>();

        public void add(String name, String value) {
            values.add(new NameValue<String>(name, value));
        }

        public void add(String name, URI uri) {
            values.add(new NameValue<String>(name, uri.toASCIIString()));
        }

        public void add(String name, long value) {
            values.add(new NameValue<String>(name, Long.toString(value)));
        }

        List<NameValue<String>> toList() {
            return values;
        }
    }

}
