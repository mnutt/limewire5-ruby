package com.limegroup.bittorrent.gui.options.panes;

import java.io.IOException;

import javax.swing.JCheckBox;

import org.limewire.core.settings.BittorrentSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;
import com.limegroup.gnutella.gui.options.panes.AbstractPaneItem;

public class AutoStartTorrentsPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Automatically start torrents");
    
    public final static String LABEL = I18n.tr("You can choose whether to automatically start torrents downloaded from Gnutella.");
    
	private final JCheckBox AUTO_START = new JCheckBox();
	
	public AutoStartTorrentsPaneItem() {
		super(TITLE, LABEL);
		
		LabeledComponent comp = new LabeledComponent(
				I18nMarker.marktr("Start automatically"), AUTO_START, 
				LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);
		add(comp.getComponent());
	}
	
	@Override
	public boolean applyOptions() throws IOException {
		BittorrentSettings.TORRENT_AUTO_START.setValue(AUTO_START.isSelected());
		return false;
	}

	@Override
	public void initOptions() {
		AUTO_START.setSelected(BittorrentSettings.TORRENT_AUTO_START.getValue());
	}

	public boolean isDirty() {
		return BittorrentSettings.TORRENT_AUTO_START.getValue() != 
			AUTO_START.isSelected();
	}

}
