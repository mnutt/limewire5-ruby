package com.limegroup.gnutella.library;

import java.util.List;

import org.limewire.listener.DefaultEvent;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.xml.LimeXMLDocument;

public class FileDescChangeEvent extends DefaultEvent<FileDesc, FileDescChangeEvent.Type> {
    
    public static enum Type { URNS_CHANGED, LOAD }
    
    private final List<? extends LimeXMLDocument> xmlDocs;
    
    public FileDescChangeEvent(FileDesc fileDesc, Type type) {
        super(fileDesc, type);
        this.xmlDocs = null;
    }
    
    public FileDescChangeEvent(FileDesc fileDesc, Type type, List<? extends LimeXMLDocument> xmlDocs) {
        super(fileDesc, type);
        this.xmlDocs = xmlDocs;
    }
    
    public List<? extends LimeXMLDocument> getXmlDocs() {
        return xmlDocs;
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this) + ", super: " + super.toString();
    }

}
