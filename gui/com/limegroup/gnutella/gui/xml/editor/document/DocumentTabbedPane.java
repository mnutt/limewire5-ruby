package com.limegroup.gnutella.gui.xml.editor.document;

import com.limegroup.gnutella.gui.xml.editor.MetaEditorTabbedPane;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 * Creates a tabbed pane for viewing info/editor of documents
 */
public class DocumentTabbedPane extends MetaEditorTabbedPane {   
    public DocumentTabbedPane(FileDesc[] fds) {
        super(fds, LimeXMLNames.DOCUMENT_SCHEMA);
        
        add(new DocumentInfo(fds, getSchema(), getDocument()));
        add(new DocumentEditor(fds, getSchema(), getDocument()));
    }
}
