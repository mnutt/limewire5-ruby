package com.limegroup.gnutella.gui.options.panes;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.limewire.core.settings.SharingSettings;
import org.limewire.setting.FileSetting;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.SaveDirectoryHandler;
import com.limegroup.gnutella.gui.SaveDirectoryHandler.ValidationResult;
import com.limegroup.gnutella.gui.search.NamedMediaType;
import com.limegroup.gnutella.gui.tables.AbstractDataLine;
import com.limegroup.gnutella.gui.tables.IconAndNameHolder;
import com.limegroup.gnutella.gui.tables.LimeTableColumn;

/**
 * Displays the named mediatype in the first column and its download directory
 * in the second column.
 */
public class MediaTypeDownloadDirDataLine extends AbstractDataLine<NamedMediaType> {

	/**
	 * Holds the new value of the download directory for this mediatype if it
	 * was set in this session.
	 */
	private String dir;

	/**
	 * Holds the corresponding file setting for the mediatype.
	 */
	private FileSetting setting;

	/**
	 * Is true when the reset action has been called on this data line.
	 */
	private boolean isReset;

	/**
	 * Handle to the current default download directory.
	 */
	private String defaultDir;

	private static final LimeTableColumn[] columns = new LimeTableColumn[] {
			new LimeTableColumn(0, "OPTIONS_SAVE_MEDIATYPE", I18n.tr("Media Type"), 60, true, IconAndNameHolder.class),
			new LimeTableColumn(1, "OPTIONS_SAVE_DIRECTORY", I18n.tr("Save Folder"), 100, true, String.class), };

	public int getColumnCount() {
		return columns.length;
	}

	public void setDefaultDir(String text) {
		defaultDir = text;
	}

	/**
	 * Reset the download directory for this mediatype. Henceforth files of this
	 * mediatype will be saved to the default download directory again.
	 */
	public void reset() {
		dir = null;
		isReset = true;
	}

	/**
	 * Saves the new download directory for this mediatype if it was set during
	 * this session or reverts the default value if it was reset.
	 */
	public void saveDirectory(Set<? super File> newDirs) {
	    boolean dirty = isDirty();
	    
		if (isReset)
			setting.revertToDefault();
		else if (dir != null && !setting.getValue().equals(new File(dir)))
			setting.setValue(new File(dir));

        if(dirty)
            newDirs.add(setting.getValue());
	}

	public LimeTableColumn getColumn(int col) {
		return columns[col];
	}

	public boolean isDynamic(int col) {
		return false;
	}

	public boolean isClippable(int col) {
		return true;
	}

	@Override
    public void initialize(NamedMediaType obj) {
		super.initialize(obj);
		setting = SharingSettings.getFileSettingForMediaType(initializer.getMediaType());
		isReset = false;
	}

	public Object getValueAt(int col) {
		switch (col) {
		case 0:
			return initializer;
		case 1:
			return getVisibleDirectoryString();
		}
		return null;
	}
	
	/**
	 * Determines if this has data different than the setting's data.
	 */
	boolean isDirty() {
        return dir != null &&  !setting.getValue().equals(new File(dir));
	}

	/**
	 * Sets the new download directory for this mediatype.
	 */
	public void setDirectory(File dir) {
	    // Otherwise, make sure they selected a valid directory that
        // they can really write to.
        ValidationResult result = SaveDirectoryHandler.isFolderValidForSaveDirectory(dir);
        switch(result) {
        case VALID:
            break;
        case BAD_BANNED:
        case BAD_VISTA:
        case BAD_SENSITIVE:
            return; // These already show a warning.
        case BAD_PERMS:
        default:
            // These need another message.
            GUIMediator.showError(I18n.tr("The selected save folder is invalid. You may not have permissions to write to the selected folder. LimeWire will revert to your previously selected folder."));
            return;
        }
        
        try {
            String newDir = FileUtils.getCanonicalPath(dir);
            this.dir = newDir;
            isReset = false;
        } catch(IOException ignored) {}
	}
    
	
	/**
	 * Returns the currently visible directory string.
	 * @return
	 */
	String getVisibleDirectoryString()	{
		if (dir != null)	
			return dir;	
		else if (isReset || setting.isDefault())
			return defaultDir;
		else		
			return setting.getValue().getAbsolutePath();
	}
	
	/**
	 * Gets the current value.
	 */
	String getDirectory() {
	    return dir;
	}

	/*
	 * @see com.limegroup.gnutella.gui.tables.DataLine#getTypeAheadColumn()
	 */
	public int getTypeAheadColumn() {
		return 0;
	}
}
