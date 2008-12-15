package com.limegroup.gnutella.gui.library;

import javax.swing.Icon;

import org.limewire.setting.FileSetting;

import com.limegroup.gnutella.gui.search.NamedMediaType;

public class SavedFilesDirectoryHolder extends FileSettingDirectoryHolder {

	public SavedFilesDirectoryHolder(FileSetting saveDir, String name) {
		super(saveDir, name);
	}
	
	@Override
    public Icon getIcon() {
		NamedMediaType nmt = NamedMediaType.getFromDescription("*");
		return nmt.getIcon();
	}
}
