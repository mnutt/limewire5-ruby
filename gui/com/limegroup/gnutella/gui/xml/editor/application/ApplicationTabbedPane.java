package com.limegroup.gnutella.gui.xml.editor.application;

import com.limegroup.gnutella.gui.xml.editor.MetaEditorTabbedPane;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 * Creates a tabbed pane for viewing info/editor of applications
 */
public class ApplicationTabbedPane extends MetaEditorTabbedPane{
    public ApplicationTabbedPane(FileDesc[] fds) {
        super(fds, LimeXMLNames.APPLICATION_SCHEMA);
        
        add(new ApplicationInfo(fds,getSchema(),getDocument()));
        add(new ApplicationEditor(fds, getSchema(), getDocument()));
    }
}
