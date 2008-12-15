package com.limegroup.gnutella.gui.library;

import java.io.File;

import javax.swing.Icon;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;

public class SpeciallySharedFilesDirectoryHolder extends AbstractDirectoryHolder {

	@Override
    public String getName() {
		return I18n.tr
			("Individually Shared Files");
	}

	@Override
    public String getDescription() {
		return I18n.tr
			("List of All Individually Shared Files");
	}

	public File getDirectory() {
		return null;
	}
	
	@Override
    public boolean isEmpty() {
	    return false;
	}
	
	@Override
    public File[] getFiles() {
	    return new File[0];
	}
	
	@Override
    public boolean accept(File file) {
	    return false; 
	}
	
	@Override
    public Icon getIcon() {
		return GUIMediator.getThemeImage("multifile_small");
	}
}
