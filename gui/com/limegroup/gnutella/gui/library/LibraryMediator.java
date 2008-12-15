package com.limegroup.gnutella.gui.library;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import org.limewire.core.settings.UISettings;

import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.FileChooserHandler;
import com.limegroup.gnutella.gui.GUIConstants;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MessageService;
import com.limegroup.gnutella.gui.library.RecursiveSharingDialog.State;
import com.limegroup.gnutella.gui.options.ConfigureOptionsAction;
import com.limegroup.gnutella.gui.options.OptionsConstructor;
import com.limegroup.gnutella.gui.sharing.ShareManager;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;
import com.limegroup.gnutella.gui.util.DividerLocationSettingUpdater;
import com.limegroup.gnutella.library.ManagedListStatusEvent;


/**
 * This class functions as an initializer for all of the elements
 * of the library and as a mediator between library objects.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class LibraryMediator implements ThemeObserver {

	/**
	 * The primary panel that contains all of the library elements.
	 */
	private static final JPanel MAIN_PANEL = new JPanel(new GridBagLayout());
	private static final CardLayout viewLayout = new CardLayout();
	private static final JPanel viewPanel = new JPanel(viewLayout);

	/**
     * Constant handle to the <tt>LibraryTree</tt> library controller.
     */
    private static final LibraryTree LIBRARY_TREE = LibraryTree.instance();
	static {
		LIBRARY_TREE.setBorder(BorderFactory.createEmptyBorder(2,0,0,0));
	}
	private static final JScrollPane TREE_SCROLL_PANE = new JScrollPane(LIBRARY_TREE);
    
    /**
     * Constant handle to the <tt>LibraryTable</tt> that displays the files
     * in a given directory.
     */
    private static final LibraryTableMediator LIBRARY_TABLE =
        LibraryTableMediator.instance();

    private static final String TABLE_KEY = "LIBRARY_TABLE";
    private static final String SHARED_KEY = "SHARED";
    private static final String STORE_KEY = "STORE";

    /**
     * Constant handle to the file update handler.
     */
    private final HandleFileUpdate FILE_UPDATER = new HandleFileUpdate();
   
	/** Panel for the Shared Files node. */
	private static JPanel jpShared = null;

    /** Panel for the LimeWire Store Files node.  */
    private static JPanel storeShared = null;

	
	///////////////////////////////////////////////////////////////////////////
	//  Singleton Pattern
	///////////////////////////////////////////////////////////////////////////
	
	/**
	 * Singleton instance of this class.
	 */
	private static final LibraryMediator INSTANCE = new LibraryMediator();
    
	/**
	 * @return the <tt>LibraryMediator</tt> instance
	 */
	public static LibraryMediator instance() { return INSTANCE; }

    /** 
	 * Constructs a new <tt>LibraryMediator</tt> instance to manage calls
	 * between library components.
	 */
    private LibraryMediator() {		
		GUIMediator.setSplashScreenString(
		    I18n.tr("Loading Library Window..."));
		ThemeMediator.addThemeObserver(this);

		addView(LIBRARY_TABLE.getScrolledTablePane(), TABLE_KEY);
		
		//  Create split pane
		JSplitPane splitPane = 
		    new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, TREE_SCROLL_PANE, viewPanel);
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(true);
		DividerLocationSettingUpdater.install(splitPane,
				UISettings.UI_LIBRARY_TREE_DIVIDER_LOCATION);

		JPanel buttonPanel = new JPanel(new BorderLayout());
		buttonPanel.add(LIBRARY_TREE.getButtonRow(), BorderLayout.WEST);
		buttonPanel.add(LIBRARY_TABLE.getButtonRow(), BorderLayout.CENTER);
		
		//  Layout main panel
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(GUIConstants.SEPARATOR, GUIConstants.SEPARATOR,
								GUIConstants.SEPARATOR, GUIConstants.SEPARATOR);
		MAIN_PANEL.add(new LibrarySearchPanel(GuiCoreMediator.getQueryRequestFactory()), gbc);
		gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(GUIConstants.SEPARATOR, GUIConstants.SEPARATOR, GUIConstants.SEPARATOR, GUIConstants.SEPARATOR);
		MAIN_PANEL.add(splitPane, gbc);
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, GUIConstants.SEPARATOR, GUIConstants.SEPARATOR, GUIConstants.SEPARATOR);
		MAIN_PANEL.add(buttonPanel, gbc);
		updateTheme();		
		
		//  Set the initial selection in the LibraryTree
		LIBRARY_TREE.setInitialSelection();
	}

	// inherit doc comment
	public void updateTheme() {
		LIBRARY_TREE.updateTheme();
		Color tableColor = ThemeFileHandler.TABLE_BACKGROUND_COLOR.getValue();
		TREE_SCROLL_PANE.getViewport().setBackground(tableColor);
	}

	/**
	 * Returns the <tt>JComponent</tt> that contains all of the elements of
	 * the library.
	 *
	 * @return the <tt>JComponent</tt> that contains all of the elements of
	 * the library.
	 */
	public JComponent getComponent() {
		return MAIN_PANEL;
	}
	
    /**
	 * Tells the library to launch the application associated with the 
	 * selected row in the library. 
	 */
    public void launchLibraryFile() {
		LIBRARY_TABLE.launch();
    }
    
    /**
	 * Deletes the currently selected rows in the table. 
	 */
    public void deleteLibraryFile() {
        LIBRARY_TABLE.removeSelection();
    }
        
	/**
	 * Removes the gui elements of the library tree and table.
	 */
//	private void clearLibrary() {
//        LIBRARY_TABLE.clearTable();
//        LIBRARY_TREE.clear();
//        quickRefresh();
//	}
    
    /**
     * @return Returns the directory that's currently visible from the table.
     *
     */
    public File getVisibleDirectory() {
        return LIBRARY_TREE.getSelectedDirectory();
    }
    
    /**
     * Quickly refreshes the library.
     *
     * This is only done if a saved or incomplete folder is selected,
     * in case an incomplete file was deleted or a new file (not shared)
     * was added to a save directory.
     */
    public void quickRefresh() {
	    DirectoryHolder dh = LIBRARY_TREE.getSelectedDirectoryHolder();
		if(dh instanceof SavedFilesDirectoryHolder || dh instanceof IncompleteDirectoryHolder ||
                dh instanceof LWSSpecialFilesHolder )
            updateTableFiles(dh);
    }
    
    /**
     * Forces a refresh of the currently selected folder.
     */
    public void forceRefresh() {
        updateTableFiles(LIBRARY_TREE.getSelectedDirectoryHolder());
    }

	/**
	 * Handles events created by the FileManager.  Passes these events on to
	 * the LibraryTableMediator or LibraryTree as necessary.
     * @param evt event created by the FileManager
     */
//    private void handleFileManagerEvent(final FileManagerEvent evt) {
//		LIBRARY_TREE.handleFileManagerEvent(evt);
//		LIBRARY_TABLE.handleFileManagerEvent(evt, LIBRARY_TREE.getSelectedDirectoryHolder());		
//    }
		
    /** 
	 * Displays a file chooser for selecting a new folder to share and 
	 * adds that new folder to the settings and FileManager.
	 */
    public void addSharedLibraryFolder() {
		File dir = FileChooserHandler.getInputDirectory();
		if (dir == null)
			return;
		addSharedLibraryFolder(dir);
    }
	
	public void addSharedLibraryFolder(final File dir) {
		if(ShareManager.checkAndWarnNewSharedFolder(dir)) {    		
    		final RecursiveSharingDialog dialog = new RecursiveSharingDialog(GUIMediator.getAppFrame(), dir);
    		if (dialog.showChooseDialog(MessageService.getParentComponent()) == State.OK) {
    			BackgroundExecutorService.schedule(new Runnable() {
    			    public void run() {
//    			        GuiCoreMediator.getFileManager().addSharedFolders(dialog.getRootsToShare(), dialog.getFoldersToExclude());
    	            }
    	        });	
    		}
		}
	}
	
	/**
	 * Update the file's statistic
	 */
	public void updateSharedFile(final File file) {
	    // if the library table is visible, and
	    // if the selected directory is null
	    // or if we the file exists in a directory
	    // other than the one we selected, then there
	    // is no need to update.
	    // the user will see the newest stats when he/she 
	    // selects the directory.
	    DirectoryHolder dh = LIBRARY_TREE.getSelectedDirectoryHolder();
		if(LIBRARY_TABLE.getTable().isShowing() && dh != null && dh.accept(file)) {
		    // pass the update off to the file updater
		    // this way, only one Runnable is ever created,
		    // instead of allocating a new one every single time
		    // a query is hit.
		    // Very useful for large libraries and generic searches (ala: mp3)
		    FILE_UPDATER.addFileUpdate(file);
	    }
	}
	
	public void setAnnotateEnabled(boolean enabled) {
	    LIBRARY_TABLE.setAnnotateEnabled(enabled);
	}

    /** 
	 * Removes the selected folder from the shared folder group.. 
	 */
    public void unshareLibraryFolder() {
        LIBRARY_TREE.unshareLibraryFolder();
    }

    /**
     * Adds a file to the playlist.
     * @param toAdd File to add
     */
    void addFileToPlayList(File toAdd) {
        GUIMediator.getPlayList().addFileToPlaylist(toAdd);
    }
    
    /**
     * Adds a list of files to add to the playlist
     * @param toAdd list of files
     */
    void addFilesToPlayList(List<File> toAdd) {
        GUIMediator.getPlayList().addFilesToPlaylist(toAdd.toArray( new File[toAdd.size()]));
    }

    /** 
	 * Obtains the shared files for the given directory and updates the 
	 * table accordingly.
	 *
	 * @param dirHolder the currently selected directory in
	 *        the library
	 */
    static void updateTableFiles(DirectoryHolder dirHolder) {
		LIBRARY_TABLE.updateTableFiles(dirHolder);
		showView(TABLE_KEY);
    }
    
	/** 
     * @return Returns true if this is showing the
     * special incomplete directory, false if showing
     * normal files
     */
    public static boolean incompleteDirectoryIsSelected() {
        return LIBRARY_TREE.incompleteDirectoryIsSelected();        
    }
    
    /**
	 * Whether or not the files in the table can currently be renamed.
	 * 
	 * TODO another hack to disallow renames for the search results holder
	 * clean this up
	 */
    static boolean isRenameEnabled() { 
    	return !LIBRARY_TREE.searchResultDirectoryIsSelected()
    		&& !LIBRARY_TREE.incompleteDirectoryIsSelected();
    }
    
    /**
     *  Class to handle updates to shared file stats
     *  without creating tons of runnables.
     *  Idea taken from HandleQueryString in VisualConnectionCallback
     */
    private static final class HandleFileUpdate implements Runnable {
        private Vector<File>  list;
        private boolean active;
    
        public HandleFileUpdate( ) {
            list   = new Vector<File>();
            active = false;
        }
    
        public void addFileUpdate(File f) {
            list.addElement(f);
            if(active == false) {
                active = true;
                SwingUtilities.invokeLater(this);
            }
        }
    
        public void run() {
            try {
                File f;
                while (list.size() > 0) {
                    f = list.firstElement();
                    list.removeElementAt(0);
    			    LIBRARY_TABLE.update(f);
                }
			} catch (IndexOutOfBoundsException e) {
        	    //this really should never happen, but
        	    //who really cares if we're not sharing it?
			} finally {
			    active = false;
            }
        }
    }    
	
	/**
	 * Shows the Shared Files view in response to selection of the Shared Files
	 * in the LibraryTree.
	 */
	public static void showSharedFiles() {
		if (jpShared == null) {
			jpShared = new JPanel(new BorderLayout());
			JPanel jpInternal = new JPanel(new GridBagLayout());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets = new Insets(0, 0, ButtonRow.BUTTON_SEP, 0);
			jpInternal.add(new JLabel(I18n.tr("You can configure the folders you share in LimeWire\'s Options.")), gbc);
			gbc.gridy = 1;
			jpInternal.add(new JButton(new ConfigureOptionsAction(
					OptionsConstructor.SHARED_KEY,
					I18n.tr("Configure..."),
					I18n.tr("You can configure the folders you share in LimeWire\'s Options."))), gbc);
			jpShared.add(jpInternal, BorderLayout.CENTER);
			jpShared.setBorder(BorderFactory.createEtchedBorder());
            addView(jpShared, SHARED_KEY);
        }
        showView(SHARED_KEY);
    }

    /**
	 *	Constructs a panel that displays the LimeWire Store (LWS) logo in the 
	 *	library table when the root in the tree of the LWS was chosen
	 */
    public static void showStoreFiles() {
        if( storeShared == null) {
            storeShared = new JPanel(new BorderLayout());
            storeShared.add( new JLabel( I18n.tr("Songs purchased from the LimeWire Store"), GUIMediator.getThemeImage("lws"), JLabel.CENTER));
            storeShared.setBorder(BorderFactory.createEtchedBorder());
            addView(storeShared, STORE_KEY);
        }
        showView(STORE_KEY);
    }

	public static void showView(String key) {
		viewLayout.show(viewPanel, key);
	}
	
	public static void addView(Component c, String key) {
		viewPanel.add(c, key);
	}

	/**
	 * Sets the selected directory in the LibraryTree.
	 * 
	 * @return true if the directory exists in the tree and could be selected
	 */
	public static boolean setSelectedDirectory(File dir) {
		return LIBRARY_TREE.setSelectedDirectory(dir);		
	}

	/**
	 * Selects the file in the library tab.
	 *
	 * @return true if the directory exists in the tree and could be selected
	 */
	public static boolean setSelectedFile(File file) {
	    boolean selected = LIBRARY_TREE.setSelectedDirectory(file.getParentFile());
	    if (selected) {
	        return LIBRARY_TABLE.setFileSelected(file);
	    }
	    return false;
	}

	/**
	 * Updates the Library GUI based on whether the player is enabled.
     * @param value setter value (true for player enabled)
     */
	public void setPlayerEnabled(boolean value) {
		LIBRARY_TABLE.setPlayerEnabled(value);
		LIBRARY_TREE.setPlayerEnabled(value);
	}

	/**
	 * Listen to events from the FileManager
	 */
    public void handleEvent(final ManagedListStatusEvent evt) {
//        SwingUtilities.invokeLater(new Runnable(){
//            public void run(){
//                switch (evt.getType()) {
//                    case LOAD_STARTED:
//                        clearLibrary();
//                        setAnnotateEnabled(false);
//                        break;
//                    case LOAD_COMPLETE:
//                        setAnnotateEnabled(true);
//                        break;
//                    default:
//                        //handleFileManagerEvent(evt);
//                }
//            }
//        });
    }
}
