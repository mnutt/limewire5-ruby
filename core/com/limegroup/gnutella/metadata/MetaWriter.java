package com.limegroup.gnutella.metadata;

import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection.MetaDataState;

/**
 * Writes metadata to disk, each type of media editor must implement
 * this interface
 */
public interface MetaWriter {

    public MetaDataState commitMetaData(String filename);
    
    public void populate(LimeXMLDocument doc);
    
    public MetaData getMetaData();
}
