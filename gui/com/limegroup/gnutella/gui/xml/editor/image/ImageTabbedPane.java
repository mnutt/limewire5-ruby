package com.limegroup.gnutella.gui.xml.editor.image;

import com.limegroup.gnutella.gui.xml.editor.MetaEditorTabbedPane;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 * Creates a tabbed pane for viewing info/editor of images
 */
public class ImageTabbedPane extends MetaEditorTabbedPane {
    public ImageTabbedPane(FileDesc[] fds) {
        super(fds, LimeXMLNames.IMAGE_SCHEMA);
        
        add(new ImageInfo(fds, getSchema(), getDocument()));
        add(new ImageEditor(fds, getSchema(), getDocument()));
    }
}
