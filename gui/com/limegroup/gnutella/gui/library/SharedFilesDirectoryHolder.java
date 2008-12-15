package com.limegroup.gnutella.gui.library;

import java.io.File;

public class SharedFilesDirectoryHolder extends FileDirectoryHolder {

    private final boolean isStore;
    
    public SharedFilesDirectoryHolder(File dir){
    	this(dir, false);
    }
    
	public SharedFilesDirectoryHolder(File dir, boolean isStore) {
		super(dir);
        
        this.isStore = isStore;
	}
    
    @Override
    public boolean isStoreNode(){
        return isStore;
	}
}
