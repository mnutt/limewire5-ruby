package com.limegroup.gnutella.gui.library;

import java.io.File;

import javax.swing.Icon;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.ImageManipulator;

/**
 * Class that holds the search results of the last library search of shared 
 * files. 
 */
public class LibrarySearchResultsHolder implements DirectoryHolder {

	private File[] results = new File[0];
	private final Icon icon;
	
	public LibrarySearchResultsHolder() {
		icon = ImageManipulator.resize(GUIMediator.getThemeImage("search_tab"), 16, 16);
	}
	
	public void setResults(File[] results) {
		this.results = results;
	}
	
	public String getName() {
		return I18n.tr("Search Results");
	}
	
	public String getDescription() {
		return I18n.tr("Holds the Results of the Last Search");
	}
	
	public File getDirectory() {
		return null;
	}
	
	public File[] getFiles() {
		return results;
	}
	
	public int size() {
		return results.length;
	}
	
	public Icon getIcon() {
		return icon;
	}
	
	public boolean isEmpty() {
		return results.length == 0;
	}
	
	public boolean accept(File pathname) {
		return false;
	}
	
    public boolean isStoreNode(){
        return false;
    }
}
