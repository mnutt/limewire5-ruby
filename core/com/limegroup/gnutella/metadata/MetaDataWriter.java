package com.limegroup.gnutella.metadata;

import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection.MetaDataState;

/**
 *  A utility class that writes modified LimeXMLDocuments as Meta-data to 
 *  an audio or video file
 */
public class MetaDataWriter {

    /**
     * File we're writing to
     */
    private final String fileName;
    
    /**
     * The editor that we're using
     */
    private final MetaWriter editor;
    
    /**
     * LimeXMLDocument that populated the MetaData
     */
    protected LimeXMLDocument correctDocument= null;
    
    public MetaDataWriter(String fileName, MetaDataFactory metaDataFactory) {
        this.fileName = fileName;
        editor = metaDataFactory.getEditorForFile(fileName);
    }
    
    public boolean needsToUpdate(MetaData data) {
        if( editor.getMetaData() == null)
            return false;
        else if ( data == null )
            return true;
        return !editor.getMetaData().equals(data);
    }
    
    /**
     * performs the actual write of the metadata to disk
     * @param filename the file that should be annotated
     * @return status code as defined in LimeWireXMLReplyCollection
     */
    public MetaDataState commitMetaData(){
        return editor.commitMetaData(fileName);
    }
    
    /**
     * Populates the editor with the values from xmldocument
     */
    public void populate(LimeXMLDocument doc) {
        if( editor == null )
            throw new NullPointerException("Editor not created");
        correctDocument = doc;
        editor.populate(doc);
    }

    public LimeXMLDocument getCorrectDocument() {
        return correctDocument;
    }
    
    public MetaWriter getEditor(){
        return editor;
    }
}
