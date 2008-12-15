package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.JCheckBox;

import org.limewire.core.settings.SharingSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;

public class ShareTorrentMetaPaneItem extends AbstractPaneItem {
    
    public final static String TITLE = I18n.tr("Torrent Sharing");
    
    public final static String LABEL = I18n.tr("You can automatically share .torrent files.  Sharing torrent files allows non-private torrents you are uploading or downloading to be found from searches in LimeWire.");

    /**
     * Constant for the key of the locale-specific <tt>String</tt> for the 
     * check box that allows the user to share .torrent files or not.
     */
    private final String CHECK_BOX_LABEL = 
        I18nMarker.marktr("Share .torrent files");
    
    /**
     * Constant for the check box that determines whether or not 
     * to share .torrent meta data files.
     */
    private final JCheckBox CHECK_BOX = new JCheckBox();
    
    public ShareTorrentMetaPaneItem() {
        super(TITLE, LABEL);
        
        LabeledComponent comp = new LabeledComponent(CHECK_BOX_LABEL,
                CHECK_BOX, LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);
        add(comp.getComponent());
    }


    @Override
    public void initOptions() {
        CHECK_BOX.setSelected
            (SharingSettings.SHARE_TORRENT_META_FILES.getValue());
    }

    @Override
    public boolean applyOptions() throws IOException {
        SharingSettings.SHARE_TORRENT_META_FILES.
            setValue(CHECK_BOX.isSelected());
        return false;
    }

    public boolean isDirty() {
        return SharingSettings.SHARE_TORRENT_META_FILES.getValue() 
            != CHECK_BOX.isSelected();
    }

}
