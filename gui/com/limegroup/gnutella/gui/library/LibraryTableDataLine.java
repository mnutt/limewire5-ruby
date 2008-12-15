package com.limegroup.gnutella.gui.library;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Icon;

import org.limewire.util.NameValue;

import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.IconManager;
import com.limegroup.gnutella.gui.dnd.FileTransfer;
import com.limegroup.gnutella.gui.tables.AbstractDataLine;
import com.limegroup.gnutella.gui.tables.ColoredCell;
import com.limegroup.gnutella.gui.tables.ColoredCellImpl;
import com.limegroup.gnutella.gui.tables.LimeTableColumn;
import com.limegroup.gnutella.gui.tables.SizeHolder;
import com.limegroup.gnutella.gui.tables.UploadCountHolder;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;
import com.limegroup.gnutella.gui.xml.XMLUtils;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * This class acts as a single line containing all
 * the necessary Library info.
 * @author Sam Berlin
 */

//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

public final class LibraryTableDataLine extends AbstractDataLine<File>
	implements ThemeObserver, FileTransfer {

    /**
     * 0 final constant to preserve memory & allocations
     */
    private static final Integer ZERO_INTEGER = new Integer(0);

    /**
     * 0 / 0 final constant UploadCountHolder to preserve memory & allocations
     */
    private static final UploadCountHolder ZERO_UPLOAD_COUNT_HOLDER
                                            = new UploadCountHolder(0, 0);
    
    /**
     * Whether or not tooltips will display XML info.
     */
    private static boolean _allowXML;

    /**
     * The schemas available
     */
    private static String[] _schemas;
    
    /**
     * Constant for the column with the icon of the file.
     */
    static final int ICON_IDX = 0;
    
	/**
	 * Constant for the column with the name of the file.
	 */
	static final int NAME_IDX = 1;
	
	/**
	 * Constant for the column storing the size of the file.
	 */
	static final int SIZE_IDX = 2;
	
	/**
	 * Constant for the column storing the file type (extension or more
	 * more general type) of the file.
	 */
	static final int TYPE_IDX = 3;
	
	/**
	 * Constant for the column storing the file's path
	 */
	static final int PATH_IDX = 4;
	
	/**
	 * Constant for the column storing the number of upload count info
	 *
	 */
	static final int UPLOADS_IDX = 5;
	
	/**
	 * Constant for the column storing the numbe of hits
	 * of the file.
	 */
	static final int HITS_IDX = 6;
	
	/**
	 * Constant for the column storing the number of alt locations
	 *
	 */
	static final int ALT_LOC_IDX = 7;
	
    /**
     * Constant for the license index.
     */
    static final int LICENSE_IDX = 8;
    
    /**
     * Constant for the column indicating the mod time of a file.
     */
    static final int MODIFICATION_TIME_IDX = 9;
    
    /**
     * Constant for the column indicating the shared state of a file.
     */
    static final int SHARED_IDX = 10;
    
    /**
     * Add the columns to static array _in the proper order_.
     * The *_IDX variables above need to match the corresponding
     * column's position in this array.
     */
    private static final LimeTableColumn[] ltColumns =
    {
        new LimeTableColumn(ICON_IDX, "LIBRARY_TABLE_ICON", I18n.tr("Icon"),
                GUIMediator.getThemeImage("question_mark"), 18, true, Icon.class),
        
        new LimeTableColumn(NAME_IDX, "LIBRARY_TABLE_NAME", I18n.tr("Name"),
                239, true, ColoredCell.class),
        
        new LimeTableColumn(SIZE_IDX, "LIBRARY_TABLE_SIZE", I18n.tr("Size"),
                62, true, ColoredCell.class),

        new LimeTableColumn(TYPE_IDX, "LIBRARY_TABLE_TYPE", I18n.tr("Type"),
                48, true, ColoredCell.class),
                                                
        new LimeTableColumn(PATH_IDX, "LIBRARY_TABLE_PATH", I18n.tr("Path"),
                108, true, ColoredCell.class),

        new LimeTableColumn(UPLOADS_IDX, "LIBRARY_TABLE_UPLOAD_COUNT", I18n.tr("Uploads"),
                62, true, UploadCountHolder.class),

        new LimeTableColumn(HITS_IDX, "LIBRARY_TABLE_HITCOUNT", I18n.tr("Hits"),
                39, true, Integer.class),
                        
        new LimeTableColumn(ALT_LOC_IDX, "LIBRARY_TABLE_NUMALTLOC", I18n.tr("Locations"),
                72, true, Integer.class),

        new LimeTableColumn(LICENSE_IDX, "LIBRARY_TABLE_LICENSE", I18n.tr("License"),
                20, true, License.class),

        new LimeTableColumn(MODIFICATION_TIME_IDX, 
                "LIBRARY_TABLE_MODIFICATION_TIME", I18n.tr("Last Modified"),
                20, false, Date.class),

        new LimeTableColumn(SHARED_IDX, "LIBRARY_TABLE_SHARED", I18n.tr("Shared"),
                20, true, Icon.class)
    };
    
	/** If the file is a directory */
	private boolean _isDirectory;

	/** Variable for the name */
	private String _name;

	/** Variable for the type */
	private String _type;

	/** Variable for the size */
	private long _size;
	
	/** Cached SizeHolder */
	private SizeHolder _sizeHolder;

	/** Variable to hold the file descriptor */
	private FileDesc _fileDesc;

	/** Variable for the path */
	private String _path;

	/**
	 * The colors for cells.
	 */
	private Color _sharedCellColor;
	private Color _unsharedCellColor;
	
	/**
	 * The model this is being displayed on
	 */
	private final LibraryTableModel _model;
	
	/**
	 * Whether or not the icon has been loaded.
	 */
	private boolean _iconLoaded = false;
	
	/**
	 * Whether or not the icon has been scheduled to load.
	 */
	private boolean _iconScheduledForLoad = false;

	public LibraryTableDataLine(LibraryTableModel ltm) {
		super();
		_model = ltm;
		updateTheme();
		ThemeMediator.addThemeObserver(this);
	}
	
	/**
	 * This must be removed from the theme observer list in
	 * order to be garbage-collected.
	 */
	@Override
    public void cleanup() {
	    ThemeMediator.removeThemeObserver(this);
	}

	// inherit doc comment
	public void updateTheme() {
		_sharedCellColor = ThemeFileHandler.WINDOW8_COLOR.getValue();
		_unsharedCellColor = ThemeFileHandler.NOT_SHARING_LABEL_COLOR.getValue();
	}

	public FileDesc getFileDesc() { return _fileDesc; }

	public int getColumnCount() { return ltColumns.length; }

	/**
	 * Initialize the object.
	 * It will fail if not given a FileDesc or a File
	 * (File is retained for compatability with the Incomplete folder)
	 */
    @Override
    public void initialize(File file) {
        super.initialize(file);
        _fileDesc = GuiCoreMediator.getFileManager().getManagedFileList().getFileDesc(file);

        String fullPath = file.getPath();
        try {
            fullPath = file.getCanonicalPath();
        } catch(IOException ioe) {}
        
		_name = initializer.getName();
		_type = "";
        if (!file.isDirectory()) {
        	_isDirectory = false;
            int index = _name.lastIndexOf(".");
            int index2 = fullPath.lastIndexOf(File.separator);
            _path = fullPath.substring(0,index2);
            if (index != -1 && index != 0) {
                _type = _name.substring(index+1);
                _name = _name.substring(0, index);
            }
        } else {
        	_path = fullPath;
        	_isDirectory = true;
        }

        // only load file sizes, do nothing for directories
        // directories implicitly set SizeHolder to null and display nothing
        if( initializer.isFile() ) {
            long oldSize = _size; 
            _size = initializer.length();
            if (oldSize != _size)
                _sizeHolder = new SizeHolder(_size);
        }
    }
    
    void setFileDesc(FileDesc fd) {
        initialize(fd.getFile());
        _fileDesc = fd;
    }
    
    /**
     * Returns the file of this data line.
     */
    public File getFile() {
        return initializer;
    }

	/**
	 * Returns the object stored in the specified cell in the table.
	 *
	 * @param idx  The column of the cell to access
	 *
	 * @return  The <code>Object</code> stored at the specified "cell" in
	 *          the list
	 */
	public Object getValueAt(int idx) {
	    switch (idx) {
	    case ICON_IDX:
	    	// the incomplete torrent icon doesn't require loading
	    	if (isIncompleteTorrent())
	    		return GUIMediator.getThemeImage("bittorrent_incomplete");
	    	boolean iconAvailable = IconManager.instance().isIconForFileAvailable(initializer);
	        if(!iconAvailable && !_iconScheduledForLoad) {
	            _iconScheduledForLoad = true;
                BackgroundExecutorService.schedule(new Runnable() {
                    public void run() {
                        GUIMediator.safeInvokeAndWait(new Runnable() {
                            public void run() {
                                IconManager.instance().getIconForFile(initializer);
                                _iconLoaded = true;
                                _model.refresh();
                            }
                        });
                    }
                });
	            return null;
            } else if(_iconLoaded || iconAvailable) {
	            return IconManager.instance().getIconForFile(initializer);
            } else {
                return null;
            }
	    case NAME_IDX:
	        String nm = _name;
	        // note: this fits better in the data line because
	        // sorting and whatnot will work correctly.
	        if (LibraryMediator.incompleteDirectoryIsSelected()) {
	            try {
                //Ideally we'd eliminate the dependency on IFM, but this seems
                //better than adding yet another method to RouterService.
                    nm = IncompleteFileManager.getCompletedName(initializer);
                } catch (IllegalArgumentException e) {
                    //Not an incomplete file?  Just return untranslated value.
                }
            }
	        return new ColoredCellImpl(nm, getColor());	                    
	    case SIZE_IDX:
	        return new ColoredCellImpl(_sizeHolder, getColor());
	    case TYPE_IDX:
	        return new ColoredCellImpl(isIncompleteTorrent() ? "torrent" : _type, 
	        		getColor());
	    case HITS_IDX:
	        if ( _fileDesc == null ) return null;
	        int hits = _fileDesc.getHitCount();
	        // don't allocate if we don't have to
	        return hits == 0 ? ZERO_INTEGER : new Integer(hits);
	        //note: we use Integer here because its compareTo is
	        //      smarter than String's, and it has a toString anyway.
	    case ALT_LOC_IDX:
	        if ( _fileDesc == null ) return null;
	        int locs = GuiCoreMediator.getAltLocManager().getNumLocs(_fileDesc.getSHA1Urn()) - 1;
	        return locs <= 0 ? ZERO_INTEGER : new Integer(locs);
	    case UPLOADS_IDX:
	        if ( _fileDesc == null ) return null;
	        int a = _fileDesc.getAttemptedUploads();
	        int c = _fileDesc.getCompletedUploads();
	        return a == 0 && c == 0 ? ZERO_UPLOAD_COUNT_HOLDER :
	                                  new UploadCountHolder(a, c);
	    case PATH_IDX:
	        return new ColoredCellImpl(_path, getColor());
        case LICENSE_IDX:
            License lc = getLicense();
            if(lc != null) {
                if(lc.isValid(_fileDesc.getSHA1Urn()))
                    return new NameValue<Integer>(lc.getLicenseName(), new Integer(License.VERIFIED));
                else
                    return new NameValue<Integer>(lc.getLicenseName(), new Integer(License.UNVERIFIED));
            } else {
                return null;
            }
        case MODIFICATION_TIME_IDX:
			// it's cheaper to use the cached value if available,
			// hope it's always uptodate
			if (_fileDesc != null) {
				return new Date(_fileDesc.lastModified());
			}
			return new Date(initializer.lastModified());
        case SHARED_IDX:
            if (GuiCoreMediator.getFileManager().getGnutellaFileList().contains(initializer))
                return GUIMediator.getThemeImage("sharing_on");
                
            return GUIMediator.getThemeImage("sharing_off");
	    }
	    return null;
	}
	
	private boolean isIncompleteTorrent() {
		return _isDirectory && LibraryMediator.incompleteDirectoryIsSelected();
	}

	public LimeTableColumn getColumn(int idx) {
	    return ltColumns[idx];
	}
	
	public boolean isClippable(int idx) {
	    switch(idx) {
        case ICON_IDX:
            return false;
        default:
            return true;
        }
    }
    
    public int getTypeAheadColumn() {
        return NAME_IDX;
    }

	public boolean isDynamic(int idx) {
	    switch(idx) {
	        case HITS_IDX:
	        case ALT_LOC_IDX:
	        case UPLOADS_IDX:
	            return true;
	    }
	    return false;
	}

	/**
	 * Initialize things we only need to do once
	 */
	static void setXMLEnabled(boolean en) {
	    _allowXML = en;
	    if ( _allowXML ) {
	        _schemas = GuiCoreMediator.getLimeXMLSchemaRepository().getAvailableSchemaURIs();
	    } else {
	        _schemas = null;
	    }
	}
	
	/**
	 * Determines if this FileDesc has a license.
	 */
	boolean isLicensed() {
	    return _fileDesc != null && _fileDesc.isLicensed();
	}
	
	/**
	 * Gets the license string for this FileDesc.
	 */
	License getLicense() {
	    return _fileDesc != null ? _fileDesc.getLicense() : null;
    }
    
    /** Gets the first XML doc associated with the FileDesc, if one exists. */
    LimeXMLDocument getXMLDocument() {
        if(_fileDesc != null) {
            List l = _fileDesc.getLimeXMLDocuments();
            if(!l.isEmpty())
                return (LimeXMLDocument)l.get(0);
        }
        
        return null;
    }

	@Override
    public String[] getToolTipArray(int col) {
        if (SHARED_IDX == col) {
            boolean shared = false;
            boolean isfile = getFile().isFile();
            
            if (GuiCoreMediator.getFileManager().getGnutellaFileList().contains(initializer))
                shared = true;
                
            if (isfile && shared)
                return new String[] { I18n.tr("This file is shared.") };
            else if (isfile && !shared)
                return new String[] { I18n.tr("This file is not shared.") };
            else if (!isfile && shared)
                return new String[] { I18n.tr("This folder is shared.") };
            else if (!isfile && !shared)
                return new String[] { I18n.tr("This folder is not shared.") };
        }
        
	    // if XML isn't finished loading, no schemas exist,
	    // or we don't have a FileDesc, get out of here.
	    if ( !_allowXML
	         || _schemas == null || _schemas.length == 0
	         || _fileDesc == null
	        ) return null;

        // Dynamically add the information.
        List<String> allData = new LinkedList<String>();        
        for(LimeXMLDocument doc : _fileDesc.getLimeXMLDocuments())
            allData.addAll(XMLUtils.getDisplayList(doc));

        
        if ( !allData.isEmpty() ) {
            // if it had meta-data, display the filename in the tooltip also.
            allData.add(0, _name);
            return allData.toArray(new String[allData.size()]);
	    } else {
	        return null;
	        //return new String[] { "No meta-data exists.", "Click 'annotate' to add some." };
	    }
	}
	
	private Color getColor() {
		if (_fileDesc != null)
			return _sharedCellColor;
//		if (GuiCoreMediator.getFileManager().isFolderShared(initializer))
//			return _sharedCellColor;
		// paint store directories as if they were shared files
//        if (GuiCoreMediator.getFileManager().isStoreDirectory(initializer))
//            return _sharedCellColor;
		return _unsharedCellColor;
	}
}
