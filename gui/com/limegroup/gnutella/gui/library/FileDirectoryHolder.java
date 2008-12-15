package com.limegroup.gnutella.gui.library;

import java.io.File;

import com.limegroup.gnutella.library.LibraryUtils;

/**
 * DirectoryHandler implementation backed by a simple directory.
 */
public class FileDirectoryHolder extends AbstractDirectoryHolder {

	private File dir;
	
	public FileDirectoryHolder(File dir) {
		this.dir = dir;
	}
	
	@Override
    public String getName() {
		return dir.getName();
	}

	@Override
    public String getDescription() {
		return dir.getAbsolutePath();
	}

	public File getDirectory() {
		return dir;
	}
	
	@Override
    public boolean accept(File f) {
	    return super.accept(f) && (LibraryUtils.isFileManagable(f) || f.isDirectory());
    }
}
