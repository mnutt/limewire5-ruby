package com.limegroup.gnutella.gui.playlist;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.CellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.QuestionsHandler;
import org.limewire.i18n.I18nMarker;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.gui.FileChooserHandler;
import com.limegroup.gnutella.gui.GUIConstants;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.PaddedPanel;
import com.limegroup.gnutella.gui.DialogOption;
import com.limegroup.gnutella.gui.dnd.CompositeTransferable;
import com.limegroup.gnutella.gui.dnd.DNDUtils;
import com.limegroup.gnutella.gui.dnd.DropInfo;
import com.limegroup.gnutella.gui.dnd.FileTransferable;
import com.limegroup.gnutella.gui.dnd.LimeTransferHandler;
import com.limegroup.gnutella.gui.mp3.AudioSource;
import com.limegroup.gnutella.gui.mp3.MediaPlayerComponent;
import com.limegroup.gnutella.gui.mp3.PlayList;
import com.limegroup.gnutella.gui.mp3.PlayListItem;
import com.limegroup.gnutella.gui.tables.AbstractTableMediator;
import com.limegroup.gnutella.gui.tables.LimeJTable;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;

/**
 * This class acts as a mediator between all of the components of the
 * playlist table.
 */
public final class PlaylistMediator extends AbstractTableMediator<PlaylistModel, PlaylistDataLine, 
        PlayListItem> implements ThemeObserver{

    private static final Log LOG = LogFactory.getLog(PlaylistMediator.class);
    
    /**
     * Instance of singleton access
     */
    private static final PlaylistMediator INSTANCE = new PlaylistMediator();
    
    /**
     * Performs the rendering of the Name column in the playlist
     */
    private StoreNameRendererEditor STORE_RENDERER;
    
    /**
     * Allows the buttons to be processed in the Name column 
     */
    private StoreNameRendererEditor STORE_EDITOR; 
    
    /**
     * Adds a row number to each row in the table
     */
    private NumberTableCellRenderer TABLE_NUMBER_RENDERER;
    
    /**
     * A lock to use for access to pl stuff.
     */
    private final Object PLAY_LOCK = new Object();
    
    /**
     * If true automatically loads next song in playlist when current song stops
     */
    private boolean isContinuous = true;
    
	/**
	 * The last playlist that was opened.
	 */
	private File lastOpenedPlaylist;
	
	/**
	 * The last playlist that was saved.
	 */
	private File lastSavedPlaylist;    

    /**
     * Listeners so buttons and possibly future right-click menu share.
     */
    ActionListener LOAD_LISTENER;
    ActionListener SAVE_LISTENER;
    ActionListener CLEAR_LISTENER;
    ActionListener CONTINUOUS_LISTENER;
    ActionListener SHUFFLE_LISTENER;
    
    /**
     * DATA_MODEL casted to a PlaylistModel so we don't have to do
     * lots of casts.
     */
    private PlaylistModel MODEL;
	
    /**
     * Constructor -- private for Singleton access
     */
    private PlaylistMediator() {
        super("PLAYLIST_TABLE");
        ThemeMediator.addThemeObserver(this);
    }
    
    public static PlaylistMediator getInstance() { 
        return INSTANCE; 
    }
	
    /**
     * Build the listeners
     */
    @Override
    protected void buildListeners() {
        super.buildListeners();
        LOAD_LISTENER = new LoadListener();
        SAVE_LISTENER = new SaveListener();
        CLEAR_LISTENER = new ClearListener();
        CONTINUOUS_LISTENER = new ContinuousListener();
        SHUFFLE_LISTENER = new ShuffleListener();
        REMOVE_LISTENER = new RemoveListener();
    }

    /**
	 * Set up the necessary constants.
	 */
	@Override
    protected void setupConstants() {
		MAIN_PANEL = new PaddedPanel(I18n.tr("Playlist"));
		DATA_MODEL = MODEL = new PlaylistModel();
		TABLE = new LimeJTable(DATA_MODEL);
//        TABLE.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        BUTTON_ROW = (new PlaylistButtons(this)).getComponent();
        STORE_RENDERER = new StoreNameRendererEditor();
        STORE_EDITOR = new StoreNameRendererEditor();
        TABLE_NUMBER_RENDERER = new NumberTableCellRenderer();
    }

    @Override
    public void updateTheme() {
        super.updateTheme();
        
        STORE_RENDERER.updateTheme();
        STORE_EDITOR.updateTheme();
    }

    /**
     * Update the splash screen
     */
	@Override
    protected void updateSplashScreen() {
		GUIMediator.setSplashScreenString(
            I18n.tr("Loading Playlist Window..."));
    }

    // inherit doc comment
    @Override
    protected JPopupMenu createPopupMenu() {
        return null;
    }
    
    /**
     * Builds the main panel, with checkboxes next to the buttons.
     */
    @Override
    protected void setupMainPanel() {
        JPanel jp = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        jp.add(getScrolledTablePane(), gbc);

        gbc.gridy = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        jp.add(Box.createVerticalStrut(GUIConstants.SEPARATOR), gbc);
        
        gbc.gridy = 2;
        jp.add(buildOptionsPanel(), gbc);

        MAIN_PANEL.add(jp);
        MAIN_PANEL.setMinimumSize(ZERO_DIMENSION);
        
        setButtonEnabled( PlaylistButtons.CLEAR_BUTTON, false );
    }
    
    /**
     * Sets the default renderers to be used in the table.
     */
    @Override
    protected void setDefaultRenderers() {
        super.setDefaultRenderers();
        TABLE.setDefaultRenderer(StoreName.class, STORE_RENDERER);
        TABLE.setDefaultRenderer(NumberCell.class, TABLE_NUMBER_RENDERER );
    }
    

    /**
     * Sets the default editors.
     */
    @Override
    protected void setDefaultEditors() {
        super.setDefaultEditors();
        TABLE.setDefaultEditor(StoreName.class,  STORE_EDITOR);
    }
    
    /**
     * Sets up dnd
     */
    @Override
    protected void setupDragAndDrop() {
		TABLE.setDragEnabled(true);
        TABLE.setTransferHandler(new PlaylistFileTransferHandler());
    }

	/**
	 * Handles the selection of the specified row in the connection window,
	 * enabling or disabling buttons
	 *
	 * @param row the selected row
	 */
	public void handleSelection(int row) {
	    setButtonEnabled( PlaylistButtons.REMOVE_BUTTON, true );
	}

	/**
	 * Handles the deselection of all rows in the download table,
	 * disabling all necessary buttons and menu items.
	 */
	public void handleNoSelection() {
	    setButtonEnabled( PlaylistButtons.REMOVE_BUTTON, false );
        // cancel any editing that may be occuring in table
        if (TABLE.isEditing()) {
            CellEditor editor = TABLE.getCellEditor();
            editor.cancelCellEditing();
        }
	}

    /**
     * Plays the currently selected song.
     */
    public void handleActionKey() {
        playSong();
    }

    /**
     * Sets the model to the specifiedsong
     */
    public void setSelectedIndex(PlayListItem item){
        if( item == null )
            throw new NullPointerException();
        MODEL.setCurrentSong(item);
    }

    /**
     * Returns the next playListItem to play. Wraps to the 
     * beginning of the list when its reached the end
     */
    public PlayListItem getNextSong() {
        return getSongToPlay();
    }
    
    /**
     * Returns all the playlist items.
     * 
     * @return all the playlist items
     */
    public List<PlayListItem> getSongs() {
        return MODEL.getSongs();
    }
    
    /**
     * Returns the prev playListItem to play, wraps around
     * to end if at beginning of the list
     */
    public PlayListItem getPrevSong() {
        MODEL.setBackwardsMode();
        return getSongToPlay();
    }
    
    /**
     * Returns the next available song to play
     */
    private PlayListItem getSongToPlay(){
        PlayListItem retFile = null;
                
        synchronized(PLAY_LOCK) {                        
            retFile = MODEL.getNextSong();
        }
        
        GUIMediator.safeInvokeAndWait(new Runnable() {
            public void run() {
                synchronized(PLAY_LOCK) {
                    refresh(); // update the colors on the table.
                    int playIndex = MODEL.getCurrentSongIndex();
                    if(playIndex >= 0)
                        TABLE.ensureRowVisible(playIndex);
                }
                if (TABLE.isEditing()) {
                    CellEditor editor = TABLE.getCellEditor();
                    editor.cancelCellEditing();
                }
            }
        });
        return retFile;
    }        
    
    /**
     * Creates a list of <code>PlayListItem</code>s from an array of files. If a folder
     * exists, it recursively checks the folder for playable files also
     * 
     * Note: This can be a very time consuming operation depending on the number of files
     * to read. This should never be executed on the swing thread
     */
    public static List<PlayListItem> createItemList(File[] files){
        List<PlayListItem> items = new ArrayList<PlayListItem>();
        for(File f: files) {
            if( f != null ){
                if( isPlayableFile(f))
                    items.add( new PlayListItem(f));
                else if( f.isDirectory())
                    items.addAll( createItemList(f.listFiles()));
            }
        }
        return items;
    }
    
    /**
     * Adds an array of files to the playlist safely.
     */
    public void addFilesToPlaylist(final File[] fs) {
        addFilesToPlaylist(fs, -1);
    }
    
    /**
     * Adds an array of files to the playlist starting at a given index
     * @param fs
     * @param index - index in table to begin at
     */
    public void addFilesToPlaylist(final File[] fs, final int index) {
        if(fs == null || fs.length == 0)
            return;
        BackgroundExecutorService.schedule(new Runnable() {
            public void run(){
                addFilesToPlayList( createItemList(fs), index);
            }
        });
    }
    
    /**
     * Safely adds a list of PlayListItems to the table model. This should
     * be used whenever more than one item is being added over addFiletoPlaylist
     * to avoid excessive thread creation
     * 
     * @param items - list of PlayListItems to add
     * @param index - index to begin adding the files at
     */
    private void addFilesToPlayList(final List<PlayListItem> items, final int index ){
        GUIMediator.safeInvokeAndWait(new Runnable() {
            public void run() { 
                synchronized(PLAY_LOCK) { 
                    for( PlayListItem item: items )
                        add( item, index);
                }
                setButtonEnabled( PlaylistButtons.CLEAR_BUTTON, true );
            }
        });
    }
    
    
    /**
     * Adds a url to the playlist to stream audio over
     * 
     * @param url - location to stream the song from
     * @param name - default name to display
     * @param isFile - true if this url is on local file system, false otherwise
     * @param storePreview - true if its a preview from the LWS
     * @param map - contains the meta data to display in the playlist
     */
    public void addFileToPlaylist(final URL url, final String name, final boolean isFile, 
            final boolean storePreview, final Map<String,String> map) {
        addFileToPlaylist(url, name, isFile, storePreview, map, -1);
    }
    
    /**
     * Adds a url to the playlist. 
     * 
     * @param url - location to stream the song from
     * @param name - default name to display
     * @param isFile - true if this url is on the local file system, false otherwise
     * @param storePreview - is a preview from the LWS
     * @param map - contains meta data to display in the playlist
     * @param index - index to add the playlist item
     */
    public void addFileToPlaylist(final URL url, final String name, final boolean isFile, 
            final boolean storePreview, final Map<String,String> map, final int index) {
        BackgroundExecutorService.schedule(new Runnable() {
            public void run(){
                try {
                    addFileToPlayList(new PlayListItem(url.toURI(), new AudioSource(url),name, isFile, storePreview, map), index);
                } catch (URISyntaxException e) {
                    //TODO: notify user about failure
                }
            }
         });
    }
	
    /**
     * Adds a file to the playlist.
     */
    public void addFileToPlaylist(final File f) { 
        addFileToPlaylist(f, -1);
    }
    
    /**
     * Adds a file to the playlist at a specified index
     * 
     * @param f - file to be added to the playlist
     * @param index - [0,MODEL.size()-1] will insert the file at the specified index
     *                  index < 0 || index >= MODEL.size() will insert the file at
     *                  the end of the playlist
     */
    public void addFileToPlaylist(final File f, final int index){
        BackgroundExecutorService.schedule(new Runnable() {
            public void run(){
                addFileToPlayList( new PlayListItem(f), index);
            }
        });
    }
    
    /**
     * Performs the actual add of a single playlist item to the table model. 
     * The add is performed on the Swing event queue. 
     * 
     * @param item - item to add
     * @param index - location in table row to add it to
     */
    private void addFileToPlayList(final PlayListItem item, final int index ){
        GUIMediator.safeInvokeAndWait(new Runnable() {
            public void run() {
                synchronized(PLAY_LOCK) {
                    add( item, index);
                }
                setButtonEnabled( PlaylistButtons.CLEAR_BUTTON, true );
            }
        });
    }
    
    /**
     * Removes a file from the playlist model at a given index
     * 
     * @param index - index to remove from the list
     * @return <code>true</code> if we remove the file, <code>false</code>
     *         for out of bounds indices
     */
    public boolean removeFileFromPlaylist(final int index) { 
        if( index < 0 || index > MODEL.getRowCount() )
            return false;

        synchronized(PLAY_LOCK) { 
            removeRow(index);
        }

        if( MODEL.getRowCount() <= 0) {
            GUIMediator.safeInvokeLater(new Runnable(){
                public void run(){
                    setButtonEnabled( PlaylistButtons.CLEAR_BUTTON, false );
                }
            });
        }
        return true;
    }
        
    /**
     * Whether or not the list should repeat.
     */
    public boolean isContinuous() {
        return isContinuous;
    }
    
    /**
     * Return true if the last song in the list has been played, false otherwise
     */
    public boolean isEndOfList() {
        return !MODEL.isShuffleSet() && MODEL.getCurrentSongIndex() == getSize()-1;
    }
    
    /**
     * Notification that play started on a song.
     */
    public void playStarted() {
        GUIMediator.safeInvokeAndWait(new Runnable() {
            public void run() {
                refresh(); // update the colors.
            }
        });
    }
    
    /**
     * Notification that a song has stopped.  
     */
    public void playComplete() {       
        GUIMediator.safeInvokeAndWait(new Runnable() {
            public void run() {
                refresh(); // update the colors on the table.
            }
        });
    }
    
    /**
     * Constructs the options panel.
     */
    private JPanel buildOptionsPanel() {
        JLabel options = new JLabel(
            I18n.tr("Play Options:"));
            
        JCheckBox shuffle = new JCheckBox(
            I18n.tr("Shuffle"), 
            false);
        shuffle.addActionListener(SHUFFLE_LISTENER);
        
        JCheckBox continuous = new JCheckBox(
            I18n.tr("Continuous"), 
            true);
        continuous.addActionListener(CONTINUOUS_LISTENER);
        
        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.add(options);
        checkBoxPanel.add(continuous);
        checkBoxPanel.add(shuffle);

        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        optionsPanel.add(BUTTON_ROW, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0;
        optionsPanel.add(checkBoxPanel, gbc);
        return optionsPanel;
    }
    
    /**
     * Determines if the file is playable on LimeWire's media player.
     */
    public static boolean isPlayableFile(File file) {
		String name = file.getName().toLowerCase(Locale.US);
		return name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav");
	}

	/**
     * Plays the first selected item.
     */
    private void playSong() {
        PlaylistDataLine line = DATA_MODEL.get(TABLE.getSelectedRow());
        if(line == null)
            return;
            
        PlayListItem f = line.getPlayListItem();
        MODEL.setCurrentSong(f);
        MediaPlayerComponent.getInstance().loadSong(f);
    }

    
    public boolean openIfPlaylist(File file) {
        String name = file.getName().toLowerCase(Locale.US);
        if (!name.endsWith(".m3u"))
            return false;       
        loadPlayListFile(file, false);
        return true;
    }
    
    /**
     * Loads a playlist.
     */
    private void loadPlaylist() {
		File parentFile = null;
		       
		if (lastOpenedPlaylist != null) {
			String parent = lastOpenedPlaylist.getParent();
			if(parent != null)	
				parentFile = new File(parent);
		}

		if(parentFile == null)
			parentFile = CommonUtils.getCurrentDirectory();
			
		final File selFile = 
			FileChooserHandler.getInputFile(getComponent(), 
			    I18nMarker.marktr("Open Playlist (.m3u)"), parentFile,
				new PlaylistListFileFilter());

        // nothing selected? exit.
        if(selFile == null || !selFile.isFile())
            return;
            
        loadPlayListFile(selFile, false);
    }
    
    /**
     * Loads playlist from a specified file
     */
    private void loadPlayListFile(File selFile, boolean overwrite) {
        String path = selFile.getPath();
        try {
            path = FileUtils.getCanonicalPath(selFile);
        } catch(IOException ignored) {
            LOG.warn("unable to get canonical path for file: " + selFile, ignored);
        }

        // create a new thread off of the event queue to process reading the files from
        //  disk
        loadPlaylist(selFile, path, overwrite);
    }
    
    /**
     * Performs the actual reading of the PlayList and generation of the PlayListItems from
     * the PlayList. Once we have done the heavy weight construction of the PlayListItem
     * list, the list is handed to the swing event queue to process adding the files to
     * the actual table model
     * 
     * 
     * @param selFile - file that we're reading from
     * @param path - path of file to open
     * @param overwrite - true if the table should be cleared of all entries prior to loading
     *          the new playlist
     */
    private void loadPlaylist(final File selFile, final String path, final boolean overwrite) {
        BackgroundExecutorService.schedule(new Runnable() {
            public void run(){
                final PlayList pl = new PlayList(path);
                synchronized(PLAY_LOCK) {
                    lastOpenedPlaylist = selFile;
                    if (overwrite)
                        clearTable();
                    
                    // put the playlist onto the swing event queue to load the playlistitem
                    //  into the actual table
                    GUIMediator.safeInvokeLater( new Runnable(){
                        public void run(){
                            MODEL.addSongs(pl);
                            if( MODEL.getRowCount() > 0 )
                                setButtonEnabled( PlaylistButtons.CLEAR_BUTTON, true );
                        }
                    });
                }
            }
        });
    }
    
    /**
     * Saves a playlist.
     */
    private void savePlaylist() {
        // get the user to select a new one....
        File suggested;
        if(lastSavedPlaylist != null)
            suggested = lastSavedPlaylist;
        else if(lastOpenedPlaylist != null)
            suggested = lastOpenedPlaylist;
        else
            suggested = new File(CommonUtils.getCurrentDirectory(), "limewire.m3u");
		
		File selFile =
		    FileChooserHandler.getSaveAsFile(
		        getComponent(), 
		        I18nMarker.marktr("Save Playlist As"),
		        suggested,
		        new PlaylistListFileFilter());
				
        // didn't select a file?  nothing we can do.
        if(selFile == null)
            return;
        
        // if the file already exists and not the one just opened, ask if it should be
        //  overwritten. 
        //TODO: this should be handled in the jfilechooser
        if(selFile.exists() && !selFile.equals(lastOpenedPlaylist)) {
            DialogOption choice = GUIMediator.showYesNoMessage(I18n.tr("Warning: a file with the name {0} already exists in the folder. Overwrite this file?", selFile.getName()), 
                        QuestionsHandler.PLAYLIST_OVERWRITE_OK, DialogOption.NO);
            if(choice != DialogOption.YES)
                return;
        }
        
        String path = selFile.getPath();
        try {
            path = FileUtils.getCanonicalPath(selFile);
        } catch(IOException ignored) {
            LOG.warn("unable to get canonical path for file: " + selFile, ignored);
        }
        // force m3u on the end.
        if(!path.toLowerCase(Locale.US).endsWith(".m3u"))
            path += ".m3u";

        // create a new thread to handle saving the playlist to disk
        savePlaylist(path);

    }
    
    /**
     * Handles actually copying and writing the playlist to disk. 
     * @param path - file location to save the list to
     */
    private void savePlaylist( final String path ) {
        BackgroundExecutorService.schedule(new Runnable() {
            public void run(){
                PlayList pl = new PlayList(path);
                // lock the list and get a copy of the songs
                synchronized(PLAY_LOCK) {
                    lastSavedPlaylist = new File(path);
                    pl.setSongs(MODEL.getLocalFiles());
                }
                
                try {
                    pl.save();
                } catch(IOException ignored) {
                    
                    LOG.warn("Unable to save playlist", ignored);
                    GUIMediator.safeInvokeLater( new Runnable(){
                        public void run(){
                            GUIMediator.showError("Unable to save playlist");
                        }
                    });
                }
            }
        });
    }
    
    /**
	 *	Listener that removes all files from the playlist
	 */
    private void clearPlaylist(){
        MODEL.clear();
        GUIMediator.safeInvokeLater( new Runnable(){
            public void run(){
                setButtonEnabled( PlaylistButtons.CLEAR_BUTTON, false );
            }
        });
    }
    
    @Override
    public void removeSelection(){
        super.removeSelection();
        if(MODEL.getRowCount() <= 0 ) {
            setButtonEnabled( PlaylistButtons.CLEAR_BUTTON, false );
        }
    }
    
    /**
	 *	Listener that removes the selected item from the playlist
	 */
    private class RemoveListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            removeSelection();
        }
    }
    
    /** 
	 * Listener that loads a playlist file.
	 */
    private class LoadListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            loadPlaylist();
        }
    }
    
    /** 
	 * Listener that saves a playlist file.
	 */
    private class SaveListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            savePlaylist();
        }
    }
    
    private class ClearListener implements ActionListener {
        public void actionPerformed(ActionEvent arg0) {
            clearPlaylist();
        }        
    }
    
    /**
     * Listener that toggles the 'continuous' setting.
     */
    private class ContinuousListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            AbstractButton b = (AbstractButton)e.getSource();
            isContinuous = b.isSelected();
        }
    }
    
    /**
     * Listener that toggles the 'shuffle' setting.
     */
    private class ShuffleListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            AbstractButton b = (AbstractButton)e.getSource();
            MODEL.setShuffle(b.isSelected());
        }
    }
    
	/**
	 * <tt>FileFilter</tt> class for only displaying m3u file types in
	 * the directory chooser.
	 */
	private static class PlaylistListFileFilter extends FileFilter {
		@Override
        public boolean accept(File f) {
		    return f.isDirectory() ||
		           f.getName().toLowerCase(Locale.US).endsWith("m3u");
		}

		@Override
        public String getDescription() {
			return I18n.tr("Playlist Files (*.m3u)");
		}
	}
    
    /**
     * Allows drops from playlist table. A drop from the playlist
     * table is equivalent to rearranging the list. To accomplish this the following must
     * happen.
     *      - 1) record the index where the drop initated
     *      - 2) add an export done method, this will only get called when the drag
     *              initiated from the playlist table
     *      - 3) add the dropped object at the selected index during the drop
     *      - 4) delete the previous object from the list
     *      
     *      Since we are updating the list at dynamic indices, new insert and remove
     *      methods need to get to added to the dataline
     */
    private class PlaylistFileTransferHandler extends LimeTransferHandler {

        DataFlavor playlistFlavor = 
            new DataFlavor(PlaylistTransferable.class, "LimeWire PlaylistTransfer");
        
        int removeIndex = -1; // location of where items where removed
        int addIndex = -1; //Location where items were added
        
        public PlaylistFileTransferHandler() {
            super(COPY_OR_MOVE | DnDConstants.ACTION_LINK);
        }
        
        @Override
        public boolean canImport(JComponent c, DataFlavor[] flavors, DropInfo ddi) { 
            return canImport(c, flavors);
        }
        
        @Override
        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            return DNDUtils.containsFileFlavors(transferFlavors)
                || DNDUtils.DEFAULT_TRANSFER_HANDLER.canImport(comp, transferFlavors);
        }
        
        @Override
        public boolean importData(JComponent c, Transferable t, DropInfo ddi) {
            return importData(c, t);
        }

        @Override
        public boolean importData(JComponent comp, Transferable t) {
            if (!canImport(comp, t.getTransferDataFlavors())) {
                return false;
            }
            try {
                //We'll drop at the current selected index.
                int index = ((JTable)comp).getSelectedRow();
                if( index < 0 || index > MODEL.getRowCount())
                    index = MODEL.getRowCount();
                
                addIndex = index;

                // when dragging an item down the list, must add 1 to the index
                //  since an item below it will later be removed
                if( index > removeIndex )
                    index += 1;

                File[] files = DNDUtils.getFiles(t);
                
                // Determine if this is a move or a drop from outside playlist
                if( !DNDUtils.contains(t.getTransferDataFlavors(),  playlistFlavor )) {
                    // if its an add, execute the drop on a new thread
                    addFilesToPlaylist(files, index);
                }

                if (files.length > 0) {
                    return true;
                }

            } catch (UnsupportedFlavorException e) {
            } catch (IOException e) {
            }
            return DNDUtils.DEFAULT_TRANSFER_HANDLER.importData(comp, t);
        }
        
        /**
         * Begin transfer of item in the playlist.  Returns null if nothing is selected.
         */
        @Override
        protected Transferable createTransferable(JComponent comp) { 
            //cancel any editing that may be currently running prior to
            //  exporting
            if (TABLE.isEditing()) {
                CellEditor editor = TABLE.getCellEditor();
                editor.cancelCellEditing();
            }

            int[] rows = TABLE.getSelectedRows(); 
            if (rows.length == 0) {
                return null;
            } else {
                List<File> list = new ArrayList<File>(rows.length);
                synchronized (PLAY_LOCK) {
                    for (int i = 0; i < rows.length; i++) {
                        list.add(MODEL.get(rows[i]).getFile());
                    }
                }
                removeIndex = rows[0];

                return new CompositeTransferable(new PlaylistTransferable(), new FileTransferable(
                        list));
            }
        }

        /**
         * When drag is initiated from the playlist
         */
        @Override
        protected void exportDone(JComponent c, Transferable data, int action) { 
            if( action == TransferHandler.MOVE ) { 
                // if drag and drop both occur from the playlist, rearrange
                //  the indices.
                if( (removeIndex != -1 || addIndex != -1) && removeIndex >= 0 &&
                        addIndex >= 0 && removeIndex < TABLE.getRowCount() &&
                        addIndex < TABLE.getRowCount()) {
                    handleMove(removeIndex, addIndex);
                }
                removeIndex = -1;
                addIndex = -1;
            }
        }
        
        /**
         * Move an item in the table to a new row index
         * @param addLocation - the row index to move
         * @param removeLocation - the row index to move it to
         */
        private void handleMove(final int removeLocation, final int addLocation){
            synchronized(PLAY_LOCK) {
                moveRow(removeLocation, addLocation);
                TABLE.setSelectedRow(addLocation);
            }
        }
    }
    
    /**
     * Pure marker transferable to discern drags from the playlist panel. 
     */
    public static class PlaylistTransferable implements Transferable { 

        public static final DataFlavor playlistFlavor =
            new DataFlavor(PlaylistTransferable.class, "LimeWire PlaylistTransfer");
        
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { playlistFlavor };
        }
        
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(playlistFlavor);
        }
        
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if(isDataFlavorSupported(flavor))
                return null;
            else
                throw new UnsupportedFlavorException(flavor);
        }
    }
    
    /**
     * Processes a button click on a 'buy' button from the playlist table when a user
     * is previewing a song from the LimeWire Store (LWS)
     */
    public void buyProduct(PlaylistDataLine line){
        JOptionPane.showMessageDialog(null, "buy: " + line.getPlayListItem().getName());
    }
    
    /**
     * Processes a button click on a 'info' button from the playlist table when a user
     * is previewing a sing from the LimewireStore
     */
    public void infoProduct(PlaylistDataLine line){
        JOptionPane.showMessageDialog(null, "try: " + line.getPlayListItem().getName());
    }
}
