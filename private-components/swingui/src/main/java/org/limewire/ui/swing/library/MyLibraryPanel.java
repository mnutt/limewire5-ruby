package org.limewire.ui.swing.library;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TooManyListenersException;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.effect.LayerEffect;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;
import org.jdesktop.swingx.JXPanel;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.collection.glazedlists.PluggableList;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.api.playlist.Playlist;
import org.limewire.core.api.playlist.PlaylistManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.MessageComponent;
import org.limewire.ui.swing.components.ShareAllComboBox;
import org.limewire.ui.swing.components.LimeComboBox.SelectionListener;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.dnd.GhostDropTargetListener;
import org.limewire.ui.swing.dnd.MyLibraryTransferHandler;
import org.limewire.ui.swing.friends.login.FriendsSignInPanel;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.playlist.PlaylistButtonDropListener;
import org.limewire.ui.swing.library.playlist.PlaylistFileItemFunction;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableModel;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.player.PlayerPanel;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPService;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class MyLibraryPanel extends LibraryPanel implements EventListener<FriendEvent> {
    
    @Resource(key="LibraryPanel.selectionPanelBackgroundOverride")
    private Color selectionPanelBackgroundOverride = null;
    @Resource
    private Icon closeButton;
    @Resource
    private Icon closeHoverButton;
    @Resource
    private Icon playQuicklistIcon;
    @Resource
    private Font subFont;
    @Resource
    private Icon gnutellaIcon;
    @Resource
    private Icon friendIcon;
    
    private final LibraryTableFactory tableFactory;
    private final CategoryIconManager categoryIconManager;
    private final PlayerPanel playerPanel;
    private final LibraryManager libraryManager;
    private final LibraryNavigator libraryNavigator;
    private final Map<Catalog, LibraryOperable<? extends LocalFileItem>> selectableMap;
    private final ShareWidgetFactory shareFactory;    
    private final PlaylistManager playlistManager;
    private final ShareListManager shareListManager;
    
    /** Map of JXLayers and categories they exist in */
    private Map<Category, JXLayer> map = new HashMap<Category, JXLayer>();
    private Map<Category, LockableUI> lockMap = new HashMap<Category, LockableUI>();
    
    private Timer repaintTimer;
    private ListenerSupport<XMPPConnectionEvent> connectionListeners;
    private ListenerSupport<FriendEvent> knownFriendsListeners;

    /**
     * set of known friends helps keep correct share numbers
     */
    private final Set<String> knownFriends = new HashSet<String>();

    private SharingFilterComboBox sharingComboBox;
    private HyperlinkButton sharingButton;
    
    private final LibraryListSourceChanger currentFriendFilterChanger;
    
    private SharingMessagePanel messagePanel;
    
    private ShareAllComboBox shareAllComboBox;
    private GhostDropTargetListener ghostDropTargetListener;
    private XMPPService xmppService;
    
    /**
     * Set to true if the current message overlay has a clickable feature to hide it.
     * This boolean is needed to properlly hide the message when the user doesn't
     * close a closeable message but the program changes the message (ie changing to
     * sharing view when not sharing with that friend). 
     */
    //TODO: revisit all this message overlay stuff. There needs to be a new class to
    // handle this logic of showing/hiding closeable/noncloseable messages.
    // the usage and complexity has changed dramatically since it was first written
    private boolean isClickMessageView = false;

    @Inject
    public MyLibraryPanel(LibraryManager libraryManager,
                          LibraryNavigator libraryNavigator,
                          IconManager iconManager,
                          LibraryTableFactory tableFactory,
                          CategoryIconManager categoryIconManager,
                          ShareWidgetFactory shareFactory,
                          HeaderBarDecorator headerBarDecorator,
                          PlayerPanel player, 
                          GhostDragGlassPane ghostPane,
                          ListenerSupport<XMPPConnectionEvent> connectionListeners,
                          ShareListManager shareListManager,
                          TextFieldDecorator textFieldDecorator,
                          ComboBoxDecorator comboDecorator,
                          ButtonDecorator buttonDecorator,
                          XMPPService xmppService,
                          FriendsSignInPanel friendSignInPanel,
                          PlaylistManager playlistManager) {
        
        super(headerBarDecorator, textFieldDecorator);
        
        GuiUtils.assignResources(this);
        
        this.libraryManager = libraryManager;
        this.libraryNavigator = libraryNavigator;
        this.tableFactory = tableFactory;
        this.categoryIconManager = categoryIconManager;    
        this.shareFactory = shareFactory;
        this.playerPanel = player;
        this.selectableMap = new HashMap<Catalog, LibraryOperable<? extends LocalFileItem>>();
        this.connectionListeners = connectionListeners;
        this.xmppService = xmppService;
        this.playlistManager = playlistManager;
        this.shareListManager = shareListManager;
        
        if (selectionPanelBackgroundOverride != null) { 
            getSelectionPanel().setBackground(selectionPanelBackgroundOverride);
        }
        PluggableList<LocalFileItem> baseLibraryList = new PluggableList<LocalFileItem>(libraryManager.getLibraryListEventPublisher(), libraryManager.getReadWriteLock());
        currentFriendFilterChanger = new LibraryListSourceChanger(baseLibraryList, libraryManager, shareListManager);
        sharingComboBox = new SharingFilterComboBox(currentFriendFilterChanger, this, shareListManager);
        comboDecorator.decorateLinkComboBox(sharingComboBox);
        sharingComboBox.setText(I18n.tr("What I'm Sharing"));
        sharingComboBox.setVisible(false);
        
        getSelectionPanel().add(sharingComboBox, "gaptop 5, gapbottom 5, alignx 50%, hidemode 3");
        
        
        sharingComboBox.addSelectionListener(new SelectionListener(){
            @Override
            public void selectionChanged(Action item) {
                messagePanel.setMessage(currentFriendFilterChanger.getCurrentFriend());
                messagePanel.setVisible(true);
                sharingComboBox.setVisible(false);
            }
        });
        
        sharingButton = new HyperlinkButton();
        sharingButton.setText(I18n.tr("What I'm Sharing"));
        sharingButton.setVisible(false);
        buttonDecorator.decorateLinkButton(sharingButton);
        sharingButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                sharingComboBox.selectFriend(SharingTarget.GNUTELLA_SHARE.getFriend());
                sharingButton.setVisible(false);
            }
        });
        getSelectionPanel().add(sharingButton, "gaptop 5, gapbottom 5, alignx 50%, hidemode 3");

        showCorrectButton();
        
        createMyCategories(baseLibraryList);
        createMyPlaylists(libraryManager.getLibraryManagedList());
        selectFirstVisible();

        this.knownFriends.add(Friend.P2P_FRIEND_ID);
        getSelectionPanel().updateCollectionShares(knownFriends);
        
        shareAllComboBox = new ShareAllComboBox(xmppService, shareFactory, this, friendSignInPanel, shareListManager);
        comboDecorator.decorateDarkFullComboBox(shareAllComboBox);
        shareAllComboBox.setText(I18n.tr("Share"));
        
        addHeaderComponent(shareAllComboBox, "cell 0 0, alignx left");
        addHeaderComponent(playerPanel, "cell 0 0, grow");
        playerPanel.setMaximumSize(new Dimension(999,999));
        playerPanel.setPreferredSize(new Dimension(999,999));

        
        setTransferHandler(new MyLibraryTransferHandler(null, libraryManager.getLibraryManagedList(), shareListManager, currentFriendFilterChanger));
        ghostDropTargetListener = new GhostDropTargetListener(this,ghostPane, currentFriendFilterChanger);
        try {
            getDropTarget().addDropTargetListener(ghostDropTargetListener);
        } catch (TooManyListenersException ignoreException) {            
        }      
        
        shareListManager.getCombinedShareList().getModel().addListEventListener(new ListEventListener<LocalFileItem>(){
            @Override
            public void listChanged(ListEvent<LocalFileItem> listChanges) {
                //coalesces repaint calls. Updates usually come in bulk, ie you sign on/off,
                // share a collection, etc..
                if(repaintTimer.isRunning())
                    repaintTimer.restart();
                else
                    repaintTimer.start();
            }
        });
        
        repaintTimer = new Timer(250, new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                MyLibraryPanel.this.repaint();
            }
        });
        repaintTimer.setRepeats(false);
    }

    @Inject
    public void register(@Named("known") ListenerSupport<FriendEvent> knownFriendsListeners, ListenerSupport<XMPPConnectionEvent> connectionListeners) {
        this.knownFriendsListeners = knownFriendsListeners;
        this.knownFriendsListeners.addListener(this);
        
        connectionListeners.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch(event.getType()) {
                case CONNECTED:
                    showCorrectButton();
                    break;
                case DISCONNECTED:
                    showCorrectButton();
                    break;
                }
            }
        });
    }


    @Override
    public void handleEvent(FriendEvent event) {
        Friend friend = event.getData();

        switch (event.getType()) {
            case ADDED:
                sharingComboBox.addFriend(event.getData());
                knownFriends.add(friend.getId());
                break;
            case REMOVED:
                sharingComboBox.removeFriend(event.getData());
                // fallthrough...
            case DELETE:
                knownFriends.remove(friend.getId());
                break;
            default:
                return;
        }
        getSelectionPanel().updateCollectionShares(knownFriends);
    }
    
    public Category getCategory() {
        return getSelectedCategory();
    }
    
    public void addFriendListener(LibraryListSourceChanger.FriendChangedListener listener) {
        currentFriendFilterChanger.addListener(listener);
    }
    
    public Friend getCurrentFriend() {
        return currentFriendFilterChanger.getCurrentFriend();
    }
    
    /**
	 * Will programtically select a friend to filter on.
	 */
    public void showSharingState(Friend friend) {
        sharingComboBox.selectFriend(friend);
    }
    
    /**
	 * Will cancel filtering and return My Library to a normal state.
	 */
    public void showAllFiles() {
        currentFriendFilterChanger.setFriend(null);
        showCorrectButton();
        messagePanel.setVisible(false);
        
        // don't hide the overlay if its the first time seeing the library or
        // im logged in and its the first time logging in. Do hide the overlay if one of the above is true
        // but the program changed the message to a notcloseable message.
        if(!isClickMessageView || !(SwingUiSettings.SHOW_FIRST_TIME_LIBRARY_OVERLAY_MESSAGE.getValue() == true ||
                (xmppService.isLoggedIn() && SwingUiSettings.SHOW_FRIEND_OVERLAY_MESSAGE.getValue() == true)))
            hideEmptyFriend();
        //reselect the current category in case we filtered on a friend that wasn't 
        //sharing anything
        Category category = getSelectedCategory();
        if(category == null)
            selectFirstVisible();
        else
            select(category);
    }
    
    /**
	 * Override the components to add in our message panel that gets displayed
     * above the table and nav when filtering on a friend.
	 */
    @Override
    protected void layoutComponent() {
        setLayout(new MigLayout("fill, gap 0, insets 0"));

        addHeaderPanel();
        addMessage();
        addNavPanel();
        addMainPanels();
    }
    
    /**
	 * Panel to display above nav and table when filtering is enabled.
	 */
    private void addMessage() {
        messagePanel = new SharingMessagePanel();
        
        add(messagePanel, "dock north, growx, hidemode 3");  
    }
    
    private void createMyCategories(EventList<LocalFileItem> sourceList) {
        // Display heading.
        addHeading(new HeadingPanel(I18n.tr("CATEGORIES")), Catalog.Type.CATEGORY);
        
        for(Category category : Category.getCategoriesInOrder()) {        
            CatalogSelectionCallback callback = null;
            if (category == Category.AUDIO) {
                callback = new CatalogSelectionCallback() {
                    @Override
                    public void catalogSelected(Catalog catalog, boolean state) {
                        playerPanel.setVisible(state);
                    }
                };
            }
            
            FilterList<LocalFileItem> filtered = GlazedListsFactory.filterList(sourceList//libraryFileList.getSwingModel()
                    , new CategoryFilter(category));
            addCategory(categoryIconManager.getIcon(category), category, 
                    createMyCategoryAction(category, filtered), filtered, callback);
            addDisposable(filtered);
            addLibraryInfoBar(category, filtered);
        }
    }

    private void showCorrectButton(){
        if(xmppService.isLoggedIn()){
            sharingButton.setVisible(false);
            sharingComboBox.setVisible(true);
        } else {
            sharingButton.setVisible(true);
            sharingComboBox.setVisible(false);  
        }
    }
    
    private JComponent createMyCategoryAction(final Category category, EventList<LocalFileItem> filtered) {        
        final ShareWidget<File> fileShareWidget = shareFactory.createFileShareWidget();
        addDisposable(fileShareWidget);             
        JScrollPane scrollPane;        
        Catalog catalog = new Catalog(category);
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<LocalFileItem>(getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));
        if (category != Category.IMAGE) {
            LibraryTable<LocalFileItem> table = tableFactory.createMyTable(category, filterList, currentFriendFilterChanger);
            table.enableMyLibrarySharing(fileShareWidget);
            table.setDoubleClickHandler(new MyLibraryDoubleClickHandler(catalog, getTableModel(table)));
            selectableMap.put(catalog, table);
            scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());    
            addDisposable(table);
        } else { //Category.IMAGE 
            scrollPane = new JScrollPane();
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            LibraryImagePanel imagePanel = tableFactory.createMyImagePanel(filterList, scrollPane, fileShareWidget, currentFriendFilterChanger);
            selectableMap.put(catalog, imagePanel);
            scrollPane.setViewportView(imagePanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());            
            addDisposable(imagePanel);
        }           
        
        // The glass layer we display messages in.
        LockableUI blurUI = new LockedUI();
        JXLayer<JComponent> jxlayer = new JXLayer<JComponent>(scrollPane, blurUI);
        map.put(category, jxlayer);
        lockMap.put(category, blurUI);
        
        // only do this if the message hasn't been shown before
        if(SwingUiSettings.SHOW_FRIEND_OVERLAY_MESSAGE.getValue() == true || 
            SwingUiSettings.SHOW_FIRST_TIME_LIBRARY_OVERLAY_MESSAGE.getValue() == true) {
            connectionListeners.addListener(new EventListener<XMPPConnectionEvent>() {
                @Override
                @SwingEDTEvent
                public void handleEvent(XMPPConnectionEvent event) {
                    switch(event.getType()) { 
                    case CONNECTED:
                        if(SwingUiSettings.SHOW_FRIEND_OVERLAY_MESSAGE.getValue() == true && 
                           SwingUiSettings.SHOW_FIRST_TIME_LIBRARY_OVERLAY_MESSAGE.getValue() == true) {
                            JPanel panel = new JPanel(new MigLayout("fill"));
                            panel.setOpaque(false);
                            panel.add(getFirstTimeMyLibraryMessageAndSignedInComponent(), "align 50% 40%");
                            JXLayer layer = map.get(category);
                            layer.getGlassPane().removeAll();
                            layer.getGlassPane().add(panel);
                            layer.getGlassPane().setVisible(true);
                            if(!SwingUiSettings.HAS_LOGGED_IN_AND_SHOWN_LIBRARY.getValue()) {
                                libraryNavigator.selectLibrary();
                                SwingUiSettings.HAS_LOGGED_IN_AND_SHOWN_LIBRARY.setValue(true);
                            }
                        } else if(SwingUiSettings.SHOW_FRIEND_OVERLAY_MESSAGE.getValue() == true) {
                            JPanel panel = new JPanel(new MigLayout("fill"));
                            panel.setOpaque(false);
                            panel.add(getFirstTimeLoggedInMessageComponent(), "align 50% 40%");
                            JXLayer layer = map.get(category);
                            layer.getGlassPane().removeAll();
                            layer.getGlassPane().add(panel);
                            layer.getGlassPane().setVisible(true);
                            if(!SwingUiSettings.HAS_LOGGED_IN_AND_SHOWN_LIBRARY.getValue()) {
                                libraryNavigator.selectLibrary();
                                SwingUiSettings.HAS_LOGGED_IN_AND_SHOWN_LIBRARY.setValue(true);
                            }
                        }
                    }
                }
            });
            
            if(SwingUiSettings.SHOW_FIRST_TIME_LIBRARY_OVERLAY_MESSAGE.getValue() == true) {
                JPanel panel = new JPanel(new MigLayout("fill"));
                panel.setOpaque(false);
                panel.add(getFirstTimeMyLibraryMessageComponent(), "align 50% 40%");
                JXLayer layer = map.get(category);
                layer.getGlassPane().removeAll();
                layer.getGlassPane().add(panel);
                layer.getGlassPane().setVisible(true);
            }
        }

            return jxlayer;
        }

    /**
     * Overrides superclass method to create catalog selection button.  This
     * method installs a DropTargetListener on playlist buttons to accept drop
     * operations.
     */
    @Override
    protected <T extends FileItem> JComponent createCatalogButton(Action action,
            Catalog catalog, FilterList<T> filteredAllFileList) {
        // Create catalog selection button.
        JComponent button = super.createCatalogButton(action, catalog, filteredAllFileList);
        
        // Install listener to accept drops on playlist button.  We should
        // consider installing a TransferHandler instead of a DropTarget.
        if (catalog.getType() == Catalog.Type.PLAYLIST) {
            new DropTarget(button, new PlaylistButtonDropListener(catalog.getPlaylist()));
        }
        
        return button;
    }
    
    /**
     * Adds the playlists to the container using the specified library file 
     * list. 
     */
    private void createMyPlaylists(LibraryFileList libraryFileList) {
        // Display heading.
        addHeading(new HeadingPanel(I18n.tr("PLAYLISTS")), Catalog.Type.PLAYLIST);

        // Get playlists from manager.
        playlistManager.renameDefaultPlaylist(I18n.tr("Quicklist"));
        List<Playlist> playlists = playlistManager.getPlaylists();
        for (Playlist playlist : playlists) {
            // Create library catalog.
            Catalog playCatalog = new Catalog(playlist);

            CatalogSelectionCallback callback = new CatalogSelectionCallback() {
                @Override
                public void catalogSelected(Catalog catalog, boolean state) {
                    playerPanel.setVisible(state);
                }
            };

            // Create list by applying filter to library file list.
            FilterList<LocalFileItem> filtered = GlazedListsFactory.filterList(
                    libraryFileList.getSwingModel(), playlist.getFilter());

            // Create playlist display component.
            JComponent component = createMyPlaylistAction(playlist, filtered); 

            // Add playlist to container.  This adds the playlist component and
            // selection button to the container.
            addCatalog(playQuicklistIcon, playCatalog, component,
                    null, filtered, callback);

            addDisposable(filtered);
            addLibraryInfoBar(playCatalog, filtered);
        }
    }
    
    @Override
    protected void playListSelected(boolean value) {
        // hide the share button when a playlist is shown
        shareAllComboBox.setVisible(!value);
    }
    
    /**
     * Creates the component used to display a single playlist.
     */
    private JComponent createMyPlaylistAction(Playlist playlist, EventList<LocalFileItem> filtered) {
        // Convert the list to one containing PlaylistFileItem elements, which 
        // include the playlist position index.
        EventList<? extends LocalFileItem> functionList = 
            GlazedListsFactory.functionList(filtered, new PlaylistFileItemFunction(playlist));
        
        // Note that the playlist is not filtered using the filter text field.
        // To revise this, we could apply a TextComponentMatcherEditor to the 
        // function list using a LibraryTextFilterator.
        
        // Create playlist table using factory.
        LibraryTable<? extends LocalFileItem> table =
            tableFactory.createPlaylistTable(playlist, functionList);
        
        // Apply saved column settings to table.
        table.applySavedColumnSettings();

        // Install double-click handler.
        table.setDoubleClickHandler(new MyLibraryDoubleClickHandler(
                new Catalog(playlist), getTableModel(table)));
        
        // Add table to selectable map.  The map is referenced when we select
        // the next/previous item for the media player.
        selectableMap.put(new Catalog(playlist), table);
        
        // Add table for disposal.
        addDisposable(table);
        
        // Create scroll pane containing table.
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportView(table);
        return scrollPane;
    }
    
    @SuppressWarnings("unchecked")
    private LibraryTableModel<LocalFileItem> getTableModel(LibraryTable table){
        return (LibraryTableModel<LocalFileItem>)table.getModel();
    }   
    
    /**
	 * Displays the Not Sharing Message in this category.
	 */
    private void showEmptyFriend(Category category) {
        if(category != null) {
            JPanel panel = new JPanel(new MigLayout("fill"));
            panel.setOpaque(false);
            panel.add(getEmptyLibraryMessageComponent(currentFriendFilterChanger.getCurrentFriend()), "align 50% 40%");
            JXLayer layer = map.get(category);
            layer.getGlassPane().removeAll(); 
            layer.getGlassPane().add(panel);
            layer.getGlassPane().setVisible(true);
        }
    }
    
    /**
	 * Displays the Sharing Collection Message. This also locks the 
     * underlying table and displays a gradient over the table.
	 */
    private void showCollectionShare(Category category, Friend friend) {
        if(category != null) {
            JPanel panel = new JPanel(new MigLayout("fill"));
            panel.setOpaque(true);
            panel.setBackground(new Color(147,170,209,80));
            panel.add(getLockedLayer(category, friend), "align 50% 40%");
            
            JXLayer layer = map.get(category);
            layer.getGlassPane().removeAll(); 
            layer.getGlassPane().add(panel);
            layer.getGlassPane().setVisible(true);
            layer.getGlassPane().repaint();
        }
    }
    
    /**
	 * Hides the Not Sharing Message in this particular category.
	 */
    private void hideEmptyFriend(Category category) {
        if(category != null) {
            JXLayer layer = map.get(category);
            layer.getGlassPane().setVisible(false);
            LockableUI locked = lockMap.get(category);
            locked.setLocked(false);
        }
    }
    
    /**
	 * Hides the Not Sharing Message in all the tables.
	 */
    private void hideEmptyFriend() {
        for(Category category : Category.values()) {
            JXLayer layer = map.get(category);
            layer.getGlassPane().setVisible(false);
            LockableUI locked = lockMap.get(category);
            locked.setLocked(false);
        }
    }
    
    @Override
    public void dispose() {
        super.dispose();
        
        knownFriendsListeners.removeListener(this);
    }
    
    private class MyLibraryDoubleClickHandler implements TableDoubleClickHandler{
        private final Catalog catalog;
        private LibraryTableModel<LocalFileItem> model;

        public MyLibraryDoubleClickHandler(Catalog catalog, LibraryTableModel<LocalFileItem> model){
            this.catalog = catalog;
            this.model = model;
        }

        @Override
        public void handleDoubleClick(int row) {
            File file = model.getFileItem(row).getFile();
            switch (model.getFileItem(row).getCategory()){
            case AUDIO:
                libraryNavigator.setActiveCatalog(catalog);
                PlayerUtils.playOrLaunch(file);
                break;
            case OTHER:
            case PROGRAM:
                NativeLaunchUtils.launchExplorer(file);
                break;
            case IMAGE:
            case VIDEO:
            case DOCUMENT:
                NativeLaunchUtils.safeLaunchFile(file);
            }
        }
    }
    
    public LibraryFileList getLibrary() {
        return libraryManager.getLibraryManagedList();
    }

    public void selectItem(File file, Catalog catalog) {
        showAllFiles();
        select(catalog);
        if (catalog != null) selectableMap.get(catalog).selectAndScrollTo(file);
    }

    public File getNextItem(File file, Catalog catalog) {
        return (catalog != null) ? selectableMap.get(catalog).getNextItem(file) : null;
    }

    public File getPreviousItem(File file, Catalog catalog) {
        return (catalog != null) ? selectableMap.get(catalog).getPreviousItem(file) : null;
    }

    public void selectItem(URN urn, Catalog catalog) {
        showAllFiles();
        select(catalog);
        if (catalog != null) selectableMap.get(catalog).selectAndScrollTo(urn);        
    }    
    
    /**
	 * Returns the Table that is currently shown. Returns null if a playlist
	 * is being shown.
	 */ 
    @SuppressWarnings("unchecked")
    public SelectAllable<LocalFileItem> getTable() {
        Category category = getSelectedCategory();
        if(category == null)
            return null;
        else
            return (SelectAllable<LocalFileItem>) selectableMap.get(new Catalog(category));
    }
    
    /**
     * Creates a locked layer over a table. This layer prevents the user from
     * interacting with the contents underneath it.
     */
    private class LockedUI extends LockableUI {
        private JXPanel panel;
        
        public LockedUI(LayerEffect... lockedEffects) {
            panel = new JXPanel(new MigLayout("aligny 50%, alignx 50%"));
            panel.setBackground(new Color(147,170,209,80));
            panel.setVisible(false);     
        }
        
        @SuppressWarnings("unchecked")
        public void installUI(JComponent c) {
            super.installUI(c);
            JXLayer<JComponent> l = (JXLayer<JComponent>) c;
            l.getGlassPane().setLayout(new BorderLayout());
            l.getGlassPane().add(panel, BorderLayout.CENTER);
        }
        
        @SuppressWarnings("unchecked")
        public void uninstall(JComponent c) {
            super.uninstallUI(c);
            JXLayer<JComponent> l = (JXLayer<JComponent>) c;
            l.getGlassPane().setLayout(new FlowLayout());
            l.getGlassPane().remove(panel);
        }
        
        public void setLocked(boolean isLocked) {
            super.setLocked(isLocked);
            panel.setVisible(isLocked);
        }
        
        @Override
        public Cursor getLockedCursor() {
            return Cursor.getDefaultCursor();
        }
    }
    
    /**
	 * Creates a Message Component when displaying a share collection. 
	 */
    public MessageComponent getLockedLayer(Category category, Friend friend) {
 
        MessageComponent messageComponent = new MessageComponent(6, 22, 18, 6);
        
        JLabel label = new JLabel(I18n.tr("Sharing entire {0} Collection with {1}", category.getSingularName(), friend.getRenderName()));
        messageComponent.decorateHeaderLabel(label);

        // {0}: name of category (Audio, Video...)
        JLabel minLabel = new JLabel(I18n.tr("New {0} files that are added to your Library will be automatically shared with this person", category.getSingularName()));
        messageComponent.decorateSubLabel(minLabel);

        messageComponent.addComponent(label, "wrap");
        messageComponent.addComponent(minLabel, "");
        
        isClickMessageView = false;
        
        return messageComponent;
    }
    
    /**
     * Creates the MessageComponent when the user first signs in.
	 * This message is displayed atop the table and gives helpful
     * advice about what to do
     */
    public MessageComponent getFirstTimeLoggedInMessageComponent() {
        MessageComponent messageComponent;
        messageComponent = new MessageComponent(6, 22, 18, 6);
        
        JLabel headerLabel = new JLabel(I18n.tr("Now What?"));
        messageComponent.decorateHeaderLabel(headerLabel);
        
        JLabel minLabel = new JLabel(I18n.tr("Click "));
        JLabel minLabel2 = new JLabel(I18n.tr(" to share or unshare with your friends"), friendIcon, JLabel.LEFT);
        messageComponent.decorateSubLabel(minLabel);
        messageComponent.decorateSubLabel(minLabel2);
        
        JButton cancelButton = new JButton(closeButton);
        cancelButton.setBorder(BorderFactory.createEmptyBorder());
        cancelButton.setRolloverIcon(closeHoverButton);
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelButton.setContentAreaFilled(false);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                for(JXLayer layer : map.values()) {
                    layer.getGlassPane().setVisible(false);
                }       
                SwingUiSettings.SHOW_FRIEND_OVERLAY_MESSAGE.setValue(false);
            }
        });
        
        isClickMessageView = true;
        
        messageComponent.addComponent(cancelButton, "span, alignx right");
        messageComponent.addComponent(headerLabel, "push, wrap");
        messageComponent.addComponent(minLabel, "split");
        messageComponent.addComponent(minLabel2, "wrap, gapright 16");

        return messageComponent;
    }
    
    /**
     * Creates the MessageComponent when the user goes to My Library
     * for the first time.
     */
    public MessageComponent getFirstTimeMyLibraryMessageComponent() {
        MessageComponent messageComponent;
        messageComponent = new MessageComponent(6, 22, 18, 6);
        
        JLabel headerLabel = new JLabel(I18n.tr("No more shared folders"));
        messageComponent.decorateHeaderLabel(headerLabel);
        
        JLabel minLabel = new JLabel(I18n.tr("Click "));
        JLabel minLabel2 = new JLabel(I18n.tr(" to share or unshare with the P2P Network"), gnutellaIcon, JLabel.LEFT);
        messageComponent.decorateSubLabel(minLabel);
        messageComponent.decorateSubLabel(minLabel2);
        
        JButton cancelButton = new JButton(closeButton);
        cancelButton.setBorder(BorderFactory.createEmptyBorder());
        cancelButton.setRolloverIcon(closeHoverButton);
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelButton.setContentAreaFilled(false);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                for(JXLayer layer : map.values()) {
                    layer.getGlassPane().setVisible(false);
                }
                SwingUiSettings.SHOW_FIRST_TIME_LIBRARY_OVERLAY_MESSAGE.setValue(false);
            }
        });

        messageComponent.addComponent(cancelButton, "span, alignx right");
        messageComponent.addComponent(headerLabel, "push, wrap");
        messageComponent.addComponent(minLabel, "split");
        messageComponent.addComponent(minLabel2, "wrap, gapright 16");
        
        isClickMessageView = true;

        return messageComponent;
    }
    
    /**
     * Creates the MessageComponent when the user first signs in.
     * This message is displayed atop the table and gives helpful
     * advice about what to do
     */
    public MessageComponent getFirstTimeMyLibraryMessageAndSignedInComponent() {
        MessageComponent messageComponent;
        messageComponent = new MessageComponent(6, 22, 18, 6);
        
        JLabel headerLabel = new JLabel(I18n.tr("No more shared folders"));
        messageComponent.decorateHeaderLabel(headerLabel);
        
        JLabel minLabel = new JLabel(I18n.tr("Click "));
        JLabel minLabel2 = new JLabel(I18n.tr(" to share or unshare with the P2P Network"), gnutellaIcon, JLabel.LEFT);
        messageComponent.decorateSubLabel(minLabel);
        messageComponent.decorateSubLabel(minLabel2);
        
        JLabel minLabel3 = new JLabel(I18n.tr("Click "));
        JLabel minLabel4 = new JLabel(I18n.tr(" to share or unshare with your friends"), friendIcon, JLabel.LEFT);
        messageComponent.decorateSubLabel(minLabel3);
        messageComponent.decorateSubLabel(minLabel4);
        
        JButton cancelButton = new JButton(closeButton);
        cancelButton.setBorder(BorderFactory.createEmptyBorder());
        cancelButton.setRolloverIcon(closeHoverButton);
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelButton.setContentAreaFilled(false);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                for(JXLayer layer : map.values()) {
                    layer.getGlassPane().setVisible(false);
                }
                SwingUiSettings.SHOW_FRIEND_OVERLAY_MESSAGE.setValue(false);
                SwingUiSettings.SHOW_FIRST_TIME_LIBRARY_OVERLAY_MESSAGE.setValue(false);
            }
        });

        messageComponent.addComponent(cancelButton, "span, alignx right");
        messageComponent.addComponent(headerLabel, "push, wrap");
        messageComponent.addComponent(minLabel, "split");
        messageComponent.addComponent(minLabel2, "wrap, gapright 16");
        
        messageComponent.addComponent(minLabel3, "split");
        messageComponent.addComponent(minLabel4, "wrap, gapright 16");
        
        isClickMessageView = true;
        
        return messageComponent;
    }
    
    /**
	 * Returns the MessageComponent when not sharing with a friend.
	 */
    public MessageComponent getEmptyLibraryMessageComponent(Friend friend) {
        NotSharingPanel notSharingPanel = new NotSharingPanel();          
        notSharingPanel.setFriend(friend);
        
        isClickMessageView = false;
        
        return notSharingPanel.getMessageComponent();
    }

    /**
     * Message to be displayed when filtering on a friend and 
     * not sharing anything with them.
     */
    private class NotSharingPanel {
        private MessageComponent messageComponent;
        private JLabel headerLabel;
        private HyperlinkButton hyperlinkButton;
        
        public NotSharingPanel() {
            messageComponent = new MessageComponent(6, 22, 18, 6);
            
            headerLabel = new JLabel();
            messageComponent.decorateHeaderLabel(headerLabel);
            
            hyperlinkButton = new HyperlinkButton(I18n.tr("Show All Files"));
            hyperlinkButton.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    showAllFiles();
                }
            });
            hyperlinkButton.setFont(subFont);
            
            messageComponent.addComponent(headerLabel, "wrap, gapright 16");
            messageComponent.addComponent(hyperlinkButton, "gapright 16");
        }
    
        public void setFriend(Friend friend) {
            if(friend.getId().equals(SharingTarget.GNUTELLA_SHARE.getFriend().getId()))
                headerLabel.setText(I18n.tr("No files shared with the P2P Network"));
            else
                headerLabel.setText(I18n.tr("No files shared with {0}", friend.getRenderName()));
        }
        
        public MessageComponent getMessageComponent() {
            return messageComponent;
        }
    }
    
    /**
     * Component used to display catalog heading in the category/playlist
     * navigation bar.
     */
    private static class HeadingPanel extends JPanel {
        @Resource
        private Color textColor;
        @Resource
        private Font textFont;
        
        private JLabel label = new JLabel();
        
        public HeadingPanel(String text) {
            super(new MigLayout("insets 0, fill"));
            
            GuiUtils.assignResources(this);
            
            setOpaque(false);
            
            label.setBorder(BorderFactory.createEmptyBorder(2,8,2,0));
            label.setFont(textFont);
            label.setForeground(textColor);
            label.setText(text);
            
            add(label, "growx, push");
        }
    }
    
    /**
     * Message bar that sits atop main panel. Displayed when filtering on a sharing list.
     */
    private class SharingMessagePanel extends JPanel {
        @Resource 
        private Color backgroundColor;
        @Resource
        private Color labelColor;
        @Resource
        private Font labelFont;
        @Resource
        private Icon gnutellaIcon;
        @Resource
        private Icon friendIcon;
        @Resource
        private Color bottomLine;
        
        private final JLabel sharingLabel;
        private final HyperlinkButton showAllButton;
        
        public SharingMessagePanel() {            
            GuiUtils.assignResources(this);
            
            setLayout(new MigLayout("fill, alignx 50%, gap 0, insets 2 10 2 10", "", "25!"));
            setBackground(backgroundColor);
            
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, bottomLine));
            
            sharingLabel = new JLabel();
            sharingLabel.setForeground(labelColor);
            sharingLabel.setFont(labelFont);
            

            showAllButton = new HyperlinkButton(I18n.tr("Show All Files"));
            showAllButton.setFont(labelFont);
            FontUtils.bold(showAllButton);
            showAllButton.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e) {
                    showAllFiles();
                }
            });

            add(sharingLabel, "push");
            add(showAllButton);
            
            setVisible(false);
        }
        
        /**	Sets the Friend's name to display when filtering on a Friend*/
        public void setMessage(Friend friend) {
            if(friend == null || friend.getId() == null) {
                setVisible(false);
                return;
            }
            if(friend.getId().equals(SharingTarget.GNUTELLA_SHARE.getFriend().getId()))
                sharingLabel.setIcon(gnutellaIcon);
            else
                sharingLabel.setIcon(friendIcon);
            sharingLabel.setText( "<html>" + I18n.tr("What I'm Sharing With {0}", boldName(friend.getRenderName())) + "</html>");
        }
        
        private String boldName(String name) {
            return "<b>" + name  + "</b>";
        }
    }
    
    @Override
    protected <T extends FileItem> void addCategorySizeListener(Category category,
            Action action, FilterList<T> filteredAllFileList, FilterList<T> filteredList) {
        ButtonSizeListener<T> listener = new ButtonSizeListener<T>(category, action, filteredList);
        filteredList.addListEventListener(listener);
        ShareAllListener<T> shareAllListener = new ShareAllListener<T>(filteredList, action);
        filteredList.addListEventListener(shareAllListener);
        addDisposable(listener);
        addDisposable(shareAllListener);
    }
    
    @Override
    protected <T extends FileItem> void addCatalogSizeListener(Catalog catalog,
            Action action, FilterList<T> filteredAllFileList, FilterList<T> filteredList) {
        switch (catalog.getType()) {
        case CATEGORY:
            addCategorySizeListener(catalog.getCategory(), action, filteredAllFileList, filteredList);
            break;
        case PLAYLIST:
            PlayListListener<T> listener = new PlayListListener<T>(action);
            addDisposable(listener);
            break;
        }
    }
    
    private void enableShareAllComboBox(boolean value) {
        if(shareAllComboBox != null)
            shareAllComboBox.setEnabled(value);
    }
    
    private class PlayListListener<T> implements Disposable {
        private Action action;
        
        public PlayListListener(Action action) {
            this.action = action;
            
            currentFriendFilterChanger.addListener(new LibraryListSourceChanger.FriendChangedListener() {
                @Override
                public void friendChanged(Friend currentFriend) {
                    updateList();
                }
            });
        }
        
        private void updateList() {
            //if not filtering
            if(currentFriendFilterChanger.getCurrentFriend() == null) {
                action.setEnabled(true);
                getSelectionPanel().setHeadingVisible(Catalog.Type.PLAYLIST, true);
            } else {
                action.setEnabled(false); 
                getSelectionPanel().setHeadingVisible(Catalog.Type.PLAYLIST, false);
                if (getSelectedCategory() == null) {
                    select(Category.AUDIO);
                }
            }
        }
        
        @Override
        public void dispose() {
        }
    }

    private class ShareAllListener<T> implements Disposable, ListEventListener<T>, PropertyChangeListener {
        private final FilterList<T> list;
        private final Action action;
        
        public ShareAllListener(FilterList<T> list, Action action) {
            this.list = list;
            this.action = action;
            
            this.action.addPropertyChangeListener(this);
            this.list.addListEventListener(this);
        }
        
        @Override
        public void dispose() {
            list.removeListEventListener(this);
            action.removePropertyChangeListener(this);
        }
        
        private void updateShareAll() {
            if(action.getValue(Action.SELECTED_KEY) == Boolean.TRUE) {
                enableShareAllComboBox(list.size() > 0);
            }   
        }

        @Override
        public void listChanged(ListEvent<T> listChanges) {
            updateShareAll();
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            updateShareAll();
        }
    }
    
    /**
     * Hide any category that has no files in it.
     */
    private class ButtonSizeListener<T> implements Disposable, ListEventListener<T>, SettingListener {
        private final Category category;
        private final Action action;
        private final FilterList<T> list;
        
        private ButtonSizeListener(Category category, Action action, FilterList<T> list) {
            this.category = category;
            this.action = action;
            this.list = list;
            action.putValue(Action.NAME, I18n.tr(category.toString()));
            setText();
            if(category == Category.PROGRAM) {
                LibrarySettings.ALLOW_PROGRAMS.addSettingListener(this);
            }
            
            currentFriendFilterChanger.addListener(new LibraryListSourceChanger.FriendChangedListener() {
                @Override
                public void friendChanged(Friend currentFriend) {
                    setText();
                }
            });
        }

        private void setText() {
            //if not filtering
            if(currentFriendFilterChanger.getCurrentFriend() == null || currentFriendFilterChanger.getCurrentFriend().getId() == null) {
                //disable other category if size is 0
                if(category == Category.OTHER) {
                    action.setEnabled(list.size() > 0);
                } else if(category == Category.PROGRAM) { // hide program category is not enabled
                    action.setEnabled(LibrarySettings.ALLOW_PROGRAMS.getValue());
                } else {
                    action.setEnabled(true);
                }
            } else { //filtering on a friend
                FriendFileList fileList;
                if(currentFriendFilterChanger.getCurrentFriend().getId().equals(SharingTarget.GNUTELLA_SHARE.getFriend().getId()))
                    fileList = shareListManager.getGnutellaShareList();
                else
                    fileList = shareListManager.getFriendShareList(currentFriendFilterChanger.getCurrentFriend());
                
                // if category shaaring, lock the ui
                if( (category == Category.AUDIO || category == Category.IMAGE || category == Category.VIDEO) && 
                        fileList != null && fileList.isCategoryAutomaticallyAdded(category)) {
                        showCollectionShare(category, currentFriendFilterChanger.getCurrentFriend());
                        lockMap.get(category).setLocked(true);
                } else {
                    if(category == Category.PROGRAM) { // hide program category is not enabled
                        action.setEnabled(LibrarySettings.ALLOW_PROGRAMS.getValue());
                    }
                    //disable any category if size is 0
                    action.setEnabled(list.size() > 0);
                    if(list.size() > 0)
                        hideEmptyFriend(category);
                    else
                        showEmptyFriend(category);
                }
            }
        }
        
        @Override
        public void dispose() {
            list.removeListEventListener(this);
            if(category == Category.PROGRAM) {
                LibrarySettings.ALLOW_PROGRAMS.removeSettingListener(this);
            }
        }

        @Override
        public void listChanged(ListEvent<T> listChanges) {
            setText();
        }

        @Override
        public void settingChanged(SettingEvent evt) {
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    setText();                    
                }
            });
        }
    }    
}
