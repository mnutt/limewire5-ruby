package com.limegroup.gnutella.gui.download;


import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.core.settings.QuestionsHandler;
import org.limewire.core.settings.SearchSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.inspection.InspectionPoint;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;
import org.limewire.util.OSUtils;

import com.limegroup.bittorrent.gui.TorrentDownloadFactory;
import com.limegroup.bittorrent.gui.TorrentFileFetcher;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.gui.DialogOption;
import com.limegroup.gnutella.gui.FileChooserHandler;
import com.limegroup.gnutella.gui.FileDetailsProvider;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MessageService;
import com.limegroup.gnutella.gui.PaddedPanel;
import com.limegroup.gnutella.gui.actions.BitziLookupAction;
import com.limegroup.gnutella.gui.actions.CopyMagnetLinkToClipboardAction;
import com.limegroup.gnutella.gui.actions.LimeAction;
import com.limegroup.gnutella.gui.actions.SearchAction;
import com.limegroup.gnutella.gui.dnd.FileTransfer;
import com.limegroup.gnutella.gui.dock.DockIcon;
import com.limegroup.gnutella.gui.dock.DockIconFactoryImpl;
import com.limegroup.gnutella.gui.options.OptionsMediator;
import com.limegroup.gnutella.gui.search.SearchInformation;
import com.limegroup.gnutella.gui.search.SearchMediator;
import com.limegroup.gnutella.gui.tables.AbstractTableMediator;
import com.limegroup.gnutella.gui.tables.ColumnPreferenceHandler;
import com.limegroup.gnutella.gui.tables.LimeJTable;
import com.limegroup.gnutella.gui.tables.LimeTableColumn;
import com.limegroup.gnutella.gui.tables.SimpleColumnListener;
import com.limegroup.gnutella.gui.tables.TableSettings;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeSettings;
import com.limegroup.gnutella.gui.util.CoreExceptionHandler;
import com.limegroup.gnutella.gui.util.GUILauncher;
import com.limegroup.gnutella.gui.util.GUILauncher.LaunchableProvider;
import com.limegroup.gnutella.util.QueryUtils;

/**
 * This class acts as a mediator between all of the components of the
 * download window.  It also constructs all of the download window
 * components.
 */
public final class DownloadMediator extends AbstractTableMediator<DownloadModel, DownloadDataLine, Downloader>
	implements FileDetailsProvider {

//	private static final Log LOG = LogFactory.getLog(DownloadMediator.class);
	
	/**
	 * Count the number of resume clicks
	 */
	@InspectablePrimitive("resume button clicks")
    private static volatile int resumeClicks;
	
	/** Count the number of downloads removed complete. */
    @InspectablePrimitive("downloads complete")
	private static volatile int downloadsComplete;

    /** Count the number of downloads removed not complete. */
    @InspectablePrimitive("downloads not complete")
	private static volatile int downloadsNotComplete;
	
    /**
     * Variable for the total number of downloads that have been added in this
     * session.
     */
    private static int _totalDownloads = 0;

    private int maxConcurrentActiveDownloads = 0;
    
    private int maxConcurrentDownloadingDownloads = 0;
    
    /**
     * instance, for singleton acces
     */
    private static DownloadMediator _instance = new DownloadMediator();


    public static DownloadMediator instance() { return _instance; }

    /**
     * Variables so only one ActionListener needs to be created for both
     * the buttons & popup menu.
     */
	private Action removeAction;
    private Action chatAction;
    private Action clearAction;
    private Action browseAction;
    private Action launchAction;
    private Action resumeAction;
    private Action pauseAction;
    private Action priorityUpAction;
    private Action priorityDownAction;
	private Action editLocationAction;
	private Action magnetAction;
	private Action bitziAction;
	private Action exploreAction; 

    /** The actual download buttons instance.
     */
    private DownloadButtons _downloadButtons;
    
    private final DockIcon dockIcon;

    private static final HashMap<String, Object> inspectionData = new HashMap<String, Object>();
    
    @SuppressWarnings("unused")
    @InspectionPoint("downloadCounts")
    private final static Inspectable downloadCountInspectable = new Inspectable() {
        public Object inspect() {
            synchronized (inspectionData) {
                return inspectionData.clone();
            }
        }
    };
    
    /**
     * Overriden to have different default values for tooltips.
     */
    @Override
    protected void buildSettings() {
        SETTINGS = new TableSettings(ID) {
            @Override
            public boolean getDefaultTooltips() {
                return false;
            }
        };
    }

    /**
     * Sets up drag & drop for the table.
     */
    @Override
    protected void setupDragAndDrop() {
    	TABLE.setDragEnabled(true);
    	TABLE.setTransferHandler(new DownloadTransferHandler());
    }

    /**
     * Build some extra listeners
     */
    @Override
    protected void buildListeners() {
        super.buildListeners();

		removeAction = new RemoveAction();
		chatAction = new ChatAction();
		clearAction = new ClearAction();
		browseAction = new BrowseAction();
		launchAction = new LaunchAction();
		resumeAction = new ResumeAction();
		pauseAction = new PauseAction();
		priorityUpAction = new PriorityUpAction();
		priorityDownAction = new PriorityDownAction();
		editLocationAction = new EditLocationAction();
		magnetAction = new CopyMagnetLinkToClipboardAction(this);
		exploreAction = new ExploreAction(); 
		bitziAction = new BitziLookupAction(this);       
    }

	/**
	 * Returns the most prominent actions that operate on the download table.
	 * @return
	 */
	public Action[] getActions() {
		Action[] actions;
		if(OSUtils.isWindows()||OSUtils.isMacOSX())
			actions = new Action[] { priorityUpAction, priorityDownAction,
				removeAction, resumeAction, pauseAction, launchAction,
				exploreAction,clearAction};
		else 
			actions = new Action[] { priorityUpAction, priorityDownAction,
			removeAction, resumeAction, pauseAction, launchAction,clearAction 
		};
		return actions;
	}
	
    /**
     * Set up the necessary constants.
     */
    @Override
    protected void setupConstants() {
        MAIN_PANEL =
            new PaddedPanel(I18n.tr("Downloads"));
        DATA_MODEL = new DownloadModel();
        TABLE = new LimeJTable(DATA_MODEL);
        _downloadButtons = new DownloadButtons(this);
        BUTTON_ROW = _downloadButtons.getComponent();
    }
    
    /**
     * Sets up the table headers.
     */
    @Override
    protected void setupTableHeaders() {
        super.setupTableHeaders();
        
        // set the queue panel to be visible depending on whether or 
        // not the priority column is visible.
        Object pId = DATA_MODEL.getColumnId(DownloadDataLine.PRIORITY_INDEX);
        _downloadButtons.setQueuePanelVisible(TABLE.isColumnVisible(pId));
        
        // add a listener to keep the queue panel in synch with the priority column
        ColumnPreferenceHandler cph = TABLE.getColumnPreferenceHandler();
        cph.setSimpleColumnListener(new SimpleColumnListener() {
            public void columnAdded(LimeTableColumn ltc, LimeJTable table) {
                assert(table == TABLE);
                if(ltc.getModelIndex() == DownloadDataLine.PRIORITY_INDEX)
                    _downloadButtons.setQueuePanelVisible(true);
            }
            
            public void columnRemoved(LimeTableColumn ltc, LimeJTable table) {
                assert(table == TABLE);
                if(ltc.getModelIndex() == DownloadDataLine.PRIORITY_INDEX)
                    _downloadButtons.setQueuePanelVisible(false);
            }
        });
    }

    /**
     * Update the splash screen.
     */
    @Override
    protected void updateSplashScreen() {
        GUIMediator.setSplashScreenString(
            I18n.tr("Loading Download Window..."));
    }

    /**
     * Constructs all of the elements of the download window, including
     * the table, the buttons, etc.
     */
    private DownloadMediator() {
        super("DOWNLOAD_TABLE");
        GUIMediator.addRefreshListener(this);
        ThemeMediator.addThemeObserver(this);
        
        if(SETTINGS.REAL_TIME_SORT.getValue())
            DATA_MODEL.sort(DownloadDataLine.PRIORITY_INDEX); // ascending
        
        this.dockIcon = new DockIconFactoryImpl().createDockIcon();
    }

    /**
     * Override the default refreshing so that we can
     * set the clear button appropriately.
     */
    @Override
    public void doRefresh() {
        boolean inactivePresent =
            ((Boolean)DATA_MODEL.refresh()).booleanValue();
        
		clearAction.setEnabled(inactivePresent);
      
		int[] selRows = TABLE.getSelectedRows();
        
		if (selRows.length > 0) {
            DownloadDataLine dataLine = DATA_MODEL.get(selRows[0]);
            
			if (dataLine.getState() == DownloadStatus.WAITING_FOR_USER) {
				resumeAction.putValue(Action.NAME,
						I18n.tr("Find More Sources for Download"));
				resumeAction.putValue(LimeAction.SHORT_NAME,
						I18n.tr("Find Sources"));
				resumeAction.putValue(Action.SHORT_DESCRIPTION,
						I18n.tr("Try to Find Additional Sources for Downloads"));
			}
            else {
				resumeAction.putValue(Action.NAME,
						I18n.tr("Resume Download"));
				resumeAction.putValue(LimeAction.SHORT_NAME, 
						 I18n.tr("Resume"));
				resumeAction.putValue(Action.SHORT_DESCRIPTION,
						 I18n.tr("Reattempt Selected Downloads"));
            }
			
            Downloader dl = dataLine.getDownloader();
            boolean inactive = dataLine.isDownloaderInactive();
            boolean resumable = dl.isResumable();
            boolean pausable = dl.isPausable(); 
            boolean completed = dl.isCompleted();
            
			resumeAction.setEnabled(resumable);
			pauseAction.setEnabled(pausable);
			priorityUpAction.setEnabled(inactive && pausable);
			priorityDownAction.setEnabled(inactive && pausable);
			exploreAction.setEnabled(completed || inactive);
		}
		
        dockIcon.draw(getCompleteDownloads());
        
        updateInspectionData();
	}

    /**
     * Returns the number of completed Downloads.
     * 
     * @return The number of completed Downloads
     */
    public int getCompleteDownloads() {
        int complete = 0;
        for (int row = 0; row < DATA_MODEL.getRowCount(); row++) {
            DownloadDataLine dataLine = DATA_MODEL.get(row);
            if (dataLine.getState() == DownloadStatus.COMPLETE) {
                complete++;
            }
        }
        return complete;
    }
    
    /**
     * Returns the total number of Downloads that have occurred in this session.
     *
     * @return the total number of Downloads that have occurred in this session
     */
    public int getTotalDownloads() {
        return _totalDownloads;
    }

    /**
     * Returns the total number of current Downloads.
     *
     * @return the total number of current Downloads
     */
    public int getCurrentDownloads() {
        return DATA_MODEL.getCurrentDownloads();
    }

    /**
     * Returns the total number of active Downloads.
     * This includes anything that is still viewable in the Downloads view.
     *
     * @return the total number of active Downloads
     */
    public int getActiveDownloads() {
        return DATA_MODEL.getRowCount();
    }
    
    /**
     * Returns the set of filenames of all downloads
     * This includes anything that is still viewable in the Downloads view.
     *
     * @return Set of filenames (String) of all downloads
     */
    
    public Set<String> getFileNames() {
    	Set<String> names = new HashSet<String>();
    	for(int c = 0;c < DATA_MODEL.getRowCount(); c++) {
    	    names.add(DATA_MODEL.get(c).getFileName());
        }
    	return names;
    }
    
    /**
     * Returns the aggregate amount of bandwidth being consumed by active downloads.
     *  
     * @return the total amount of bandwidth being consumed by active downloads.
     */
    public double getActiveDownloadsBandwidth() {
        return DATA_MODEL.getActiveDownloadsBandwidth();
    }

    /**
     * Overrides the default add.
     *
     * Adds a new Downloads to the list of Downloads, obtaining the necessary
     * information from the supplied <tt>Downloader</tt>.
     *
     * If the download is not already in the list, then it is added.
     *  <p>
     */
    @Override
    public void add(Downloader downloader) {
        if ( !DATA_MODEL.contains(downloader) ) {
            _totalDownloads++;
            super.add(downloader);
        }
    }

    private void updateInspectionData() {
        maxConcurrentActiveDownloads = Math.max(maxConcurrentActiveDownloads, getActiveDownloads());
        maxConcurrentDownloadingDownloads = Math.max(maxConcurrentDownloadingDownloads, getCurrentDownloads());
        synchronized (inspectionData) {
            inspectionData.put("total", getTotalDownloads());
            inspectionData.put("downloading", getCurrentDownloads());
            inspectionData.put("active", getActiveDownloads());
            inspectionData.put("maxActive", maxConcurrentActiveDownloads);
            inspectionData.put("maxDActive", maxConcurrentDownloadingDownloads);
        }
    }

    /**
     * Overrides the default remove.
     *
     * Takes action upon downloaded theme files, asking if the user wants to
     * apply the theme.
     *
     * Removes a download from the list if the user has configured their system
     * to automatically clear completed download and if the download is
     * complete.
     *
     * @param downloader the <tt>Downloader</tt> to remove from the list if it is
     *  complete.
     */
    @Override
    public void remove(Downloader dloader) {
        DownloadStatus state = dloader.getState();

        // Record if this download completed, or was removed by the user not yet completed
        if (state == DownloadStatus.COMPLETE)
            downloadsComplete++;
        else
            downloadsNotComplete++;
        
        if (state == DownloadStatus.COMPLETE 
        		&& isThemeFile(dloader.getSaveFile().getName())) {
        	File themeFile = dloader.getDownloadFragment();
        	themeFile = copyToThemeDir(themeFile);
        	// don't allow changing of theme while options are visible,
        	// but notify the user how to change the theme
        	if (OptionsMediator.instance().isOptionsVisible()) {
        		GUIMediator.showMessage(I18n.tr("You have downloaded a skin titled \"{0}\", you can activate the new skin by clicking \"{1}\" in the \"{2}\"->\"{3}\" menu and then selecting it from the list of available skins.",
        		        ThemeSettings.formatName(dloader.getSaveFile().getName()),
        				I18n.tr("&Refresh Skins"),
        				I18n.tr("&View"),
        				I18n.tr("&Apply Skins")));
        	}
        	else {
        	    DialogOption response = GUIMediator.showYesNoMessage(
        				I18n.tr("You have downloaded a new skin titled {0}. Would you like to use this new skin?",
        				        ThemeSettings.formatName(dloader.getSaveFile().getName())),
        				QuestionsHandler.THEME_DOWNLOADED, DialogOption.YES
        				);
        		if( response == DialogOption.YES ) {
        			ThemeMediator.changeTheme(themeFile);
        		}
        	}
        }
        
        if (state == DownloadStatus.COMPLETE &&
        		BittorrentSettings.TORRENT_AUTO_START.getValue() &&
        		isTorrentFile(dloader.getSaveFile().getName())) 
        	GUIMediator.instance().openTorrent(dloader.getSaveFile());
        
        if(SharingSettings.CLEAR_DOWNLOAD.getValue()
           && ( state == DownloadStatus.COMPLETE ||
                state == DownloadStatus.ABORTED ) ) {
            super.remove(dloader);
        } else {
            DownloadDataLine ddl = DATA_MODEL.get(dloader);
            if (ddl != null) ddl.setEndTime(System.currentTimeMillis());
        }
    }

    public void openTorrent(File file) {
    	try {
            TorrentDownloadFactory factory = new TorrentDownloadFactory(file);
    		DownloaderUtils.createDownloader(factory);
            if(SharingSettings.SHARE_TORRENT_META_FILES.getValue()) {
                final File tFile = GuiCoreMediator.getTorrentManager()
                                .getSharedTorrentMetaDataFile(factory.getBTMetaInfo());
                
                File backup = null;
                if(tFile.exists()) {
                    //could be same file if we are re-launching 
                    //an existing torrent from library
                    if(tFile.equals(file)) {
                        return;
                    }
                    
                    GuiCoreMediator.getFileManager().getManagedFileList().remove(tFile);

                    backup = new File(tFile.getParent(), tFile.getName().concat(".bak"));
                    FileUtils.forceRename(tFile, backup);
                }
                if(!FileUtils.copy(file, tFile) && (backup != null)) {
                    //try restoring backup
                    if(FileUtils.forceRename(backup, tFile)) {
                        GuiCoreMediator.getFileManager().getGnutellaFileList().add(tFile);
                    }
                } 
            }
    	} catch (IOException ioe) {
        	// could not read torrent file or bad torrent file.
        	GUIMediator.showError(I18n.tr("LimeWire was unable to load the torrent file \"{0}\", - it may be malformed or LimeWire does not have permission to access this file.", 
        			file.getName()),
        			QuestionsHandler.TORRENT_OPEN_FAILURE);
    	}
    }
    
    public void openTorrentURI(URI uri) {
    	TorrentFileFetcher fetcher = new TorrentFileFetcher(uri, GuiCoreMediator.getDownloadManager());
    	add(fetcher);
    	fetcher.fetch();
    }
    
    private File copyToThemeDir(File themeFile) {
        File themeDir = ThemeSettings.THEME_DIR_FILE;
        File realLoc = new File(themeDir, themeFile.getName());
        // if they're the same, just use it.
        if( realLoc.equals(themeFile) )
            return themeFile;

        // otherwise, if the file already exists in the theme dir, remove it.
        realLoc.delete();
        
        // copy from shared to theme dir.
        FileUtils.copy(themeFile, realLoc);
        return realLoc;
    }
    
    private boolean isThemeFile(String name) {
        return name.toLowerCase(Locale.US).endsWith(ThemeSettings.EXTENSION);
    }
    
    private boolean isTorrentFile(String name) {
    	return name.toLowerCase(Locale.US).endsWith(".torrent");
    }
    

    /**
     * Launches the selected files in the <tt>Launcher</tt> or in the built-in
     * media player.
     */
    void launchSelectedDownloads() {
        int[] sel = TABLE.getSelectedRows();
        if (sel.length == 0) {
        	return;
        }
        LaunchableProvider[] providers = new LaunchableProvider[sel.length];
        for (int i = 0; i < sel.length; i++) {
        	providers[i] = new DownloaderProvider(DATA_MODEL.get(sel[i]).getDownloader());
        }
        GUILauncher.launch(providers);
    }
    
    /**
     * Pauses all selected downloads.
     */
    void pauseSelectedDownloads() {
        int[] sel = TABLE.getSelectedRows();
        for(int i = 0; i < sel.length; i++)
            DATA_MODEL.get(sel[i]).getInitializeObject().pause();
    }
    
    /**  
     * Launches explorer
     */ 
    void launchExplorer() { 
        int[] sel = TABLE.getSelectedRows();
        Downloader dl = DATA_MODEL.get(sel[sel.length-1]).getInitializeObject(); 
        File toExplore = dl.getFile(); 
        
        if (toExplore == null) {
            return;
        }
        
        GUIMediator.launchExplorer(toExplore);
    } 

    /**
     * Changes the priority of the selected downloads by amt.
     */
    void bumpPriority(final boolean up, int amt) {
        int[] sel = TABLE.getSelectedRows();
        DownloadDataLine[] lines = new DownloadDataLine[sel.length];
        for(int i = 0; i < sel.length; i++)
            lines[i] = DATA_MODEL.get(sel[i]);

        // sort the lines by priority.
        // this is necessary so that they move in the correct order
        Arrays.sort(lines, new Comparator<DownloadDataLine>() {
            public int compare(DownloadDataLine a, DownloadDataLine b) {
                int pa = a.getInitializeObject().getInactivePriority();
                int pb = b.getInitializeObject().getInactivePriority();
                return (pa < pb ? -1 : pa > pb ? 1 : 0) * ( up ? 1 : -1 );
            }
        });

        for(int i = 0; i < lines.length; i++) {
            Downloader dl = lines[i].getInitializeObject();
            GuiCoreMediator.getDownloadManager().bumpPriority(dl, up, amt);
        }
    }
    
    FileTransfer[] getSelectedFileTransfers() {
    	int[] sel = TABLE.getSelectedRows();
    	ArrayList<FileTransfer> transfers = new ArrayList<FileTransfer>(sel.length);
    	for (int i = 0; i < sel.length; i++) {
    		DownloadDataLine line = DATA_MODEL.get(sel[i]);
    		Downloader downloader = line.getDownloader();
    		// ignore if save file of complete downloader has already been moved
    		if (downloader.getState() == DownloadStatus.COMPLETE
    				&& !downloader.getSaveFile().exists()) {
    			continue;
    		}
        	if (downloader.isLaunchable()) {
        		transfers.add(line.getFileTransfer());
        	}
    	}
    	return transfers.toArray(new FileTransfer[transfers.size()]);
    }

    /**
     * Forces the selected downloads in the download window to resume.
     */
    void resumeSelectedDownloads() {
        int[] sel = TABLE.getSelectedRows();
        for(int i = 0; i < sel.length; i++) {
            DownloadDataLine dd = DATA_MODEL.get(sel[i]);
            Downloader downloader = dd.getDownloader();
                if(!dd.isCleaned())
                    downloader.resume();
        }
        
        resumeClicks++;
    }

    /**
     * Opens up a chat session with the selected hosts in the download
     * window.
     */
    void chatWithSelectedDownloads() {
        int[] sel = TABLE.getSelectedRows();
        for(int i = 0; i < sel.length; i++) {
            DownloadDataLine dd = DATA_MODEL.get(sel[i]);
            Downloader downloader= dd.getInitializeObject();
            Endpoint end = downloader.getChatEnabledHost();
            if (end != null) {
//                GUIMediator.createChat(end.getAddress(), end.getPort());
            }
        }
    }

	/**
	 * Shows file chooser dialog for first selected download.
	 *
	 */
	void editSelectedDownload() {
        int[] sel = TABLE.getSelectedRows();
		Downloader dl = DATA_MODEL.get(sel[0]).getInitializeObject();
		File saveLocation = dl.getSaveFile();
		File saveFile = FileChooserHandler.getSaveAsFile(MessageService.getParentComponent(),
				I18nMarker.marktr("Choose Save Location"), saveLocation);
		if (saveFile == null)
			return;

		try {
			// note: if the user did not change the file location
			// and you try setting the same location as is set,
			// you get an exception because the
			// filename is already taken by the same downloader
			if (!saveFile.equals(dl.getSaveFile())) {
				dl.setSaveFile(saveFile.getParentFile(), saveFile.getName(), false);
			}
		} catch (SaveLocationException sle) {
			CoreExceptionHandler.handleSaveLocationError(sle);
		}
	}

    /**
     * Opens up a browse session with the selected hosts in the download
     * window.
     */
    void browseSelectedDownloads() {
        int[] sel = TABLE.getSelectedRows();
        for(int i = 0; i < sel.length; i++) {
            DownloadDataLine dd = DATA_MODEL.get(sel[i]);
            Downloader downloader = dd.getInitializeObject();
            RemoteFileDesc end = downloader.getBrowseEnabledHost();
            if (end != null)
                SearchMediator.doBrowseHost(end);
        }
    }

    /**
     * Handles a double-click event in the table.
     */
    public void handleActionKey() {
    	if (launchAction.isEnabled())
    		launchSelectedDownloads();
    } 

    /**
     * Clears the downloads in the download window that have completed.
     */
    void clearCompletedDownloads() {
        DATA_MODEL.clearCompleted();
        clearSelection();
        clearAction.setEnabled(false);
        
        dockIcon.draw(0);
    }

	/**
	 * Returns the selected {@link FileDetails}.
	 */
	public FileDetails[] getFileDetails() {
        int[] sel = TABLE.getSelectedRows();
//		FileManager fmanager = GuiCoreMediator.getFileManager();
		List<FileDetails> list = new ArrayList<FileDetails>(sel.length);
//        for(int i = 0; i < sel.length; i++) {
//            URN urn = DATA_MODEL.get(sel[i]).getDownloader().getSha1Urn();
//			if (urn != null) {
//				FileDesc fd = fmanager.getManagedFileList().getFileDesc(urn);
//				if (fd != null) {
//				    // DPINJ:  Use passed in LocalFileDetailsFactory
//					list.add(GuiCoreMediator.getLocalFileDetailsFactory().create(fd));
//				}
//				else if (LOG.isDebugEnabled()) {
//					LOG.debug("not filedesc for urn " + urn);
//				}
//			}
//			else if (LOG.isDebugEnabled()) {
//				LOG.debug("no urn");
//			}
//		}
		return list.toArray(new FileDetails[0]);
	}

    // inherit doc comment
    @Override
    protected JPopupMenu createPopupMenu() {
		
		JPopupMenu menu = new JPopupMenu();
		menu.add(new JMenuItem(removeAction));
		menu.add(new JMenuItem(resumeAction));
		menu.add(new JMenuItem(pauseAction));
		menu.add(new JMenuItem(launchAction));
		if(OSUtils.isWindows()||OSUtils.isMacOSX())
			menu.add(new JMenuItem(exploreAction)); 
		menu.addSeparator();
		menu.add(new JMenuItem(clearAction));
		menu.addSeparator();
        menu.add(createSearchMenu());
		menu.add(new JMenuItem(chatAction));
		menu.add(new JMenuItem(browseAction));
		menu.add(new JMenuItem(editLocationAction));
//		menu.addSeparator();
//		menu.add(createAdvancedSubMenu());
				
		return menu;
    }
	
    private JMenu createSearchMenu() {
        JMenu menu = new JMenu(I18n.tr("Search..."));
        int[] sel = TABLE.getSelectedRows();
        if ( sel.length == 0 )  { // is there any file selected ?
        	menu.setEnabled(false);
            return menu;
        }
        //-- make perform orginal query --
        // get orginal query
        DownloadDataLine line = DATA_MODEL.get(sel[0]);
        Downloader downloader = line.getDownloader();
        Map searchInfoMap = (Map) downloader.getAttribute(
                                        SearchMediator.SEARCH_INFORMATION_KEY );
        if ( searchInfoMap != null ) {
            SearchInformation searchInfo = SearchInformation.createFromMap( searchInfoMap );
            menu.add(new JMenuItem( new SearchAction( searchInfo ) ));
        }
        
        //-- make search for filename action --
        // get name of the file
        java.lang.String filename = line.getFileName();
        // remove extension - searches searches filename not in specified format
        int dotPos = filename.lastIndexOf('.');
        if ( dotPos > 0 )
            filename = filename.substring( 0, dotPos );
        filename = QueryUtils.removeIllegalChars( filename );
        // cut the file name if necessary
        if ( filename.length() > SearchSettings.MAX_QUERY_LENGTH.getValue() )
            filename = filename.substring(0, SearchSettings.MAX_QUERY_LENGTH.getValue());
        SearchInformation info = 
            SearchInformation.createKeywordSearch (filename, null, MediaType.getAnyTypeMediaType());
        if (SearchMediator.validateInfo(info) == SearchMediator.QUERY_VALID)
            menu.add(new JMenuItem( new SearchAction(info,I18nMarker.marktr("Search for Keywords: {0}")) ));
        
        return menu;
    }
    
    /**
     * Handles the selection of the specified row in the download window,
     * enabling or disabling buttons and chat menu items depending on
     * the values in the row.
     *
     * @param row the selected row
     */
    public void handleSelection(int row) {

        DownloadDataLine dataLine = DATA_MODEL.get(row);

        chatAction.setEnabled(dataLine.getChatEnabled());
        browseAction.setEnabled(dataLine.getBrowseEnabled());
        
		boolean inactive = dataLine.isDownloaderInactive();
        boolean pausable = dataLine.getDownloader().isPausable();

		
		if (dataLine.getState() == DownloadStatus.WAITING_FOR_USER) {
			resumeAction.putValue(Action.NAME,
								  I18n.tr("Find More Sources for Download"));
			resumeAction.putValue(LimeAction.SHORT_NAME,
								  I18n.tr("Find Sources"));
			resumeAction.putValue(Action.SHORT_DESCRIPTION,
								  I18n.tr("Try to Find Additional Sources for Downloads"));
		} else {
			resumeAction.putValue(Action.NAME,
								  I18n.tr("Resume Download"));
			resumeAction.putValue(LimeAction.SHORT_NAME, 
								  I18n.tr("Resume"));
			resumeAction.putValue(Action.SHORT_DESCRIPTION,
								  I18n.tr("Reattempt Selected Downloads"));
		}
		
		if (dataLine.isCompleted()) {
			removeAction.putValue(Action.NAME,
					  I18n.tr("Clear Download"));
			removeAction.putValue(LimeAction.SHORT_NAME,
					  I18n.tr("Clear"));
			removeAction.putValue(Action.SHORT_DESCRIPTION,
					  I18n.tr("Clear Selected Downloads"));
			launchAction.putValue(Action.NAME,
					  I18n.tr("Launch Download"));
			launchAction.putValue(LimeAction.SHORT_NAME,
					  I18n.tr("Launch"));
			launchAction.putValue(Action.SHORT_DESCRIPTION,
					  I18n.tr("Launch Selected Downloads"));
			exploreAction.setEnabled(TABLE.getSelectedRowCount() == 1); 
		} else {
			removeAction.putValue(Action.NAME, I18n.tr
					("Cancel Download"));
			removeAction.putValue(LimeAction.SHORT_NAME,
					 I18n.tr("Cancel"));
			removeAction.putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Cancel Selected Downloads"));
			launchAction.putValue(Action.NAME,
					  I18n.tr("Preview Download"));
			launchAction.putValue(LimeAction.SHORT_NAME,
					  I18n.tr("Preview"));
			launchAction.putValue(Action.SHORT_DESCRIPTION,
					  I18n.tr("Preview Selected Downloads"));
			exploreAction.setEnabled(false); 
		}
		
		removeAction.setEnabled(true);
        resumeAction.setEnabled(inactive);
		pauseAction.setEnabled(pausable);
        priorityDownAction.setEnabled(inactive && pausable);
        priorityUpAction.setEnabled(inactive && pausable);
		
		Downloader dl = dataLine.getInitializeObject();
		editLocationAction.setEnabled(TABLE.getSelectedRowCount() == 1 
									  && dl.isRelocatable());
		
		magnetAction.setEnabled(dl.getSha1Urn() != null);
		bitziAction.setEnabled(dl.getSha1Urn() != null);
		launchAction.setEnabled(dl.isLaunchable());
    }

    /**
     * Handles the deselection of all rows in the download table,
     * disabling all necessary buttons and menu items.
     */
    public void handleNoSelection() {
        removeAction.setEnabled(false);
		resumeAction.setEnabled(false);
		launchAction.setEnabled(false);
		pauseAction.setEnabled(false);
		chatAction.setEnabled(false);
		browseAction.setEnabled(false);
		priorityDownAction.setEnabled(false);
		priorityUpAction.setEnabled(false);
		editLocationAction.setEnabled(false);
		magnetAction.setEnabled(false);
		bitziAction.setEnabled(false);
		exploreAction.setEnabled(false); 
    }

    private abstract class RefreshingAction extends AbstractAction {
    	public final void actionPerformed(ActionEvent e) {
    		performAction(e);
    		doRefresh();
    	}
    	
    	protected abstract void performAction(ActionEvent e);
    }
    
	private class RemoveAction extends RefreshingAction {
		
		public RemoveAction() {
			putValue(Action.NAME, I18n.tr
					("Cancel Download"));
			putValue(LimeAction.SHORT_NAME,
					 I18n.tr("Cancel"));
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Cancel Selected Downloads"));
			putValue(LimeAction.ICON_NAME, "DOWNLOAD_KILL");
		}
		
		@Override
        public void performAction(ActionEvent e) {
			removeSelection();
		}
	}
	
	private class ChatAction extends RefreshingAction {
		
		public ChatAction() {
    	    putValue(Action.NAME,
					I18n.tr("Chat with Host"));
		}
		
		@Override
        public void performAction(ActionEvent e) {
            chatWithSelectedDownloads();
        }
	}
	
	private class ClearAction extends RefreshingAction {
		
		public ClearAction() {
			putValue(Action.NAME,
					 I18n.tr("Clear All Inactive Downloads"));
			putValue(LimeAction.SHORT_NAME,
					 I18n.tr("Clear Inactive"));
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Remove Inactive Downloads"));
			putValue(LimeAction.ICON_NAME, "DOWNLOAD_CLEAR");
		}
		
	    @Override
        public void performAction(ActionEvent e) {
            clearCompletedDownloads();
        }
	}

	private class BrowseAction extends RefreshingAction {

		public BrowseAction() {
    	    putValue(Action.NAME,
					I18n.tr("Browse Host"));
		}
		
		@Override
        public void performAction(ActionEvent e) {
			browseSelectedDownloads();
		}
	}

	private class LaunchAction extends RefreshingAction {
		
		public LaunchAction() {
			putValue(Action.NAME,
					 I18n.tr("Preview Download"));
			putValue(LimeAction.SHORT_NAME,
					 I18n.tr("Preview"));
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Preview Selected Downloads"));
			putValue(LimeAction.ICON_NAME, "DOWNLOAD_LAUNCH");
		}

		@Override
        public void performAction(ActionEvent e) {
			launchSelectedDownloads();
		}
	}

	
	private class ResumeAction extends RefreshingAction {

		public ResumeAction() {
    	    putValue(Action.NAME,
					 I18n.tr("Resume Download"));
			putValue(LimeAction.SHORT_NAME, 
					 I18n.tr("Resume"));
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Reattempt Selected Downloads"));
 			putValue(LimeAction.ICON_NAME, "DOWNLOAD_FILE_MORE_SOURCES");
		}
		
		@Override
        public void performAction(ActionEvent e) {
			resumeSelectedDownloads();
		}
	}

	private class PauseAction extends RefreshingAction {

		public PauseAction() {
			putValue(Action.NAME,
					 I18n.tr("Pause Download"));
			putValue(LimeAction.SHORT_NAME,
					 I18n.tr("Pause"));
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Pause Selected Downloads"));
			putValue(LimeAction.ICON_NAME, "DOWNLOAD_PAUSE");
		}
		
		@Override
        public void performAction(ActionEvent e) {
			pauseSelectedDownloads();
		}
	}

	private class ExploreAction extends RefreshingAction { 
		public ExploreAction() { 
	        putValue(Action.NAME, 
	                 I18n.tr("Explore")); 
	        putValue(LimeAction.SHORT_NAME, 
	                 I18n.tr("Explore")); 
	        putValue(Action.SHORT_DESCRIPTION, 
	                 I18n.tr("Open Folder Containing the File")); 
	        putValue(LimeAction.ICON_NAME, "LIBRARY_EXPLORE"); 
	    } 
	     
	    @Override
        public void performAction(ActionEvent e) { 
	        launchExplorer(); 
	    } 
	} 
	private class PriorityUpAction extends RefreshingAction {

		public PriorityUpAction() {
			putValue(LimeAction.SHORT_NAME, "");
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Move the Selected Download Closer to Becoming Active"));
			putValue(LimeAction.ICON_NAME, "DOWNLOAD_PRIORITY_UP");
		}
		
		@Override
        public void performAction(ActionEvent e) {
			if ((e.getModifiers() & ActionEvent.CTRL_MASK) != 0)
				bumpPriority(true, 10); //bump by 10 places
			else if ((e.getModifiers() & ActionEvent.ALT_MASK) != 0)
				bumpPriority(true, 0);  //bump to top priority
			else
				bumpPriority(true, 1);
		}
	}

	private class PriorityDownAction extends RefreshingAction {

		public PriorityDownAction() {
			putValue(LimeAction.SHORT_NAME, "");
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Move the Selected Download Further from Becoming Active"));
			putValue(LimeAction.ICON_NAME, "DOWNLOAD_PRIORITY_DOWN");
		}
		
		@Override
        public void performAction(ActionEvent e) {
			if ((e.getModifiers() & ActionEvent.CTRL_MASK) != 0)
				bumpPriority(false, 10);    //bump by 10 places
			else if ((e.getModifiers() & ActionEvent.ALT_MASK) != 0)
				bumpPriority(false, 0); //bump to top priority
			else
				bumpPriority(false, 1);
		}
	}
	
	private class EditLocationAction extends RefreshingAction {

		public EditLocationAction() {
			putValue(Action.NAME, 
					 I18n.tr
					 ("Change File Location..."));
			putValue(Action.SHORT_DESCRIPTION, 
					 I18n.tr
					 ("Edit the Final Filename of the Download"));
		}

		@Override
        public void performAction(ActionEvent e) {
			editSelectedDownload();
		}
	}
	
	private static class DownloaderProvider implements LaunchableProvider {

		private final Downloader downloader;
		
		public DownloaderProvider(Downloader downloader) {
			this.downloader = downloader;
		}
		
		public Downloader getDownloader() {
			return downloader;
		}

		public File getFile() {
			return null;
		}
		
	}

}
