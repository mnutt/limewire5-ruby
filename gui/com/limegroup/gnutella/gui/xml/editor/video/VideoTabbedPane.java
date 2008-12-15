package com.limegroup.gnutella.gui.xml.editor.video;

import com.limegroup.gnutella.gui.xml.editor.MetaEditorTabbedPane;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 * Creates a tabbed pane for viewing info/editor of videos
 */
public class VideoTabbedPane extends MetaEditorTabbedPane {
    public VideoTabbedPane(FileDesc[] fds) {
        super(fds, LimeXMLNames.VIDEO_SCHEMA);
        
        add(new VideoInfo(fds,getSchema(), getDocument()));
        add(new VideoEditor(fds, getSchema(), getDocument()));
    }
}
