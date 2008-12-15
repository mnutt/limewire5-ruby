

package com.limegroup.gnutella.gui.xml.editor.audio;

import com.limegroup.gnutella.gui.xml.editor.MetaEditorTabbedPane;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 * Creates a tabbed pane for viewing info/editor of audio files
 */
public class AudioTabbedPane extends MetaEditorTabbedPane {
    public AudioTabbedPane(FileDesc[] fds) {
        super(fds, LimeXMLNames.AUDIO_SCHEMA);
        
            add(new AudioInfo(fds, getSchema(), getDocument()));
        add(new AudioEditor(fds, getSchema(), getDocument()));
    }
}
