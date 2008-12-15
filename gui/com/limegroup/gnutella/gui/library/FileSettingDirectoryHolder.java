package com.limegroup.gnutella.gui.library;

import java.io.File;

import org.limewire.setting.FileSetting;


/**
 * Implementation of the {@link DirectoryHolder} interface backed by a file 
 * setting.
 */
public class FileSettingDirectoryHolder extends AbstractDirectoryHolder {

	private String name;
	private String desc;
	private FileSetting fs;
	
	public FileSettingDirectoryHolder(FileSetting fs, String name, String description) {
		this.name = name;
		this.fs = fs;
		this.desc = description;
	}
	
	public FileSettingDirectoryHolder(FileSetting fs, String name) {
		this(fs, name, null);
	}
	
	public FileSettingDirectoryHolder(FileSetting fs) {
		this(fs, null);
	}
	
	/**
	 * Returns the name of the directory if no name is set in the constructor.
	 */
	@Override
    public String getName() {
		return name != null ? name : getDirectory().getName();
	}

	/**
	 * Returns the absolute path of directory if none is provided in the
	 * constructor.
	 */
	@Override
    public String getDescription() {
		return desc != null ? desc : getDirectory().getAbsolutePath();
	}

	public File getDirectory() {
		return fs.getValue();
	}
}
