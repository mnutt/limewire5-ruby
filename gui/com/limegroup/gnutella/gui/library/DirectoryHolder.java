package com.limegroup.gnutella.gui.library;

import java.io.File;
import java.io.FileFilter;

import javax.swing.Icon;

/**
 * Interface for the directory data model behind a node in the library tree.
 * 
 */
public interface DirectoryHolder extends FileFilter {

	/**
	 * Returns the name of the directory.
	 * @return
	 */
	String getName();
	/**
	 * Returns an additional description which is displayed as a tooltip.
	 * @return
	 */
	String getDescription();
	/**
	 * Returns the physical directory behind this virtual directory holder.
	 * @return
	 */
	File getDirectory();
	/**
	 * Returns the files that should be displayed when this directory holder
	 * is selected.
	 * @return
	 */
	File[] getFiles();
	/**
	 * Returns the number of files that this directory holder contains.
	 */
	int size();
	/**
	 * Returns a display item for the folder.
	 * @return
	 */
	Icon getIcon();
	
	/**
	 * Determines if this is empty.
	 */
	boolean isEmpty();
    
    /**
     * @return true if this is an instance that contains a file from the LWS,
     * false otherwise
     */
    boolean isStoreNode();
}
