package org.limewire.ui.swing.library;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.TooManyListenersException;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.collection.glazedlists.PluggableList;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendManager;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.dnd.GhostDropTargetListener;
import org.limewire.ui.swing.library.EmptyFriendLibraryMessagePanel.MessageTypes;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.DNDUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;

/**
 * Displays files and information about a friend or a browse host. There is a
 * single FriendLibraryPanel for all friends. A composite list of all files
 * from all friends is created and filtered on depending on what friend is
 * currently selected. 
 * <p>
 * If a friend is selected and no files are being shared, an empty panel replaces
 * the table and displays a message to the user.
 */
public class FriendLibraryPanel extends AbstractFileListPanel {

    private final CategoryIconManager categoryIconManager;
    private final LibraryTableFactory tableFactory;
    private final DownloadListManager downloadListManager;
    private final LibraryManager libraryManager;
    private final LibraryNavigator libraryNavigator;
    private final RemoteLibraryManager remoteLibraryManager;
    private final ShareListManager shareListManager;
    private final FriendManager friendManager;
    
    private final FriendLibraryListSourceChanger currentFriendFilterChanger;
    
    /** 
	 * Transfer library, will add dropped files to My Library and share them with the
     * selected friend.
     */
    private final FriendLibraryTransferHandler transferHandler;
    
    /** Friend that is currently selected, null if all files are being shown*/
    private Friend currentFriend;
    
    /** Empty Panel to replace the table with if no files are to be displayed */
    private EmptyFriendLibraryMessagePanel emptyMessagePanel;
    
    @Inject
    public FriendLibraryPanel(@Assisted PluggableList<RemoteFileItem> baseLibraryList,
            @Assisted FriendLibraryListSourceChanger currentFriendFilterChanger,
            HeaderBarDecorator headerBarFactory,
            TextFieldDecorator textFieldDecorator, CategoryIconManager categoryIconManager,
            LibraryTableFactory libraryTableFactory, DownloadListManager downloadListManager,
            LibraryManager libraryManager, LibraryNavigator libraryNavigator,
            final RemoteLibraryManager remoteLibraryManager,
            ShareListManager shareListManager, EmptyFriendLibraryMessagePanel emptyMessagePanel,
            GhostDragGlassPane ghostGlassPane, FriendManager friendManager) {
        super(headerBarFactory, textFieldDecorator);
        
        this.categoryIconManager = categoryIconManager;
        this.tableFactory = libraryTableFactory;
        this.downloadListManager = downloadListManager;
        this.libraryManager = libraryManager;
        this.libraryNavigator = libraryNavigator;
        this.remoteLibraryManager = remoteLibraryManager;
        this.shareListManager = shareListManager;
        this.emptyMessagePanel = emptyMessagePanel;
        this.friendManager = friendManager;
        this.currentFriendFilterChanger = currentFriendFilterChanger;
        
        createMyCategories(baseLibraryList);
        
        setEmptyPanel(emptyMessagePanel);
        
        transferHandler = new FriendLibraryTransferHandler(this);
        setTransferHandler(transferHandler);
        try {
            getDropTarget().addDropTargetListener(new GhostDropTargetListener(this, ghostGlassPane, currentFriendFilterChanger));
        } catch (TooManyListenersException ignoreException) {            
        }   
    }
    
    /**
     * Sets the friend that is currently in the view. Setting a friend here
     * will apply a filter to the tables to show only files from that friend. 
     * To view All Friends, friend may be set to null. 
     */
    public void setFriend(Friend friend) {
        if(friend == null)
            getHeaderPanel().setText(I18n.tr("Browse Files from All Friends"));
        else
            getHeaderPanel().setText(I18n.tr("Browse Files from {0}", friend.getRenderName()));
        
        currentFriend = friend;
        currentFriendFilterChanger.setFriend(friend);
        
        emptyMessagePanel.setFriend(friend);
        selectFirstVisible();
        
        updateEmptyMessage(friend);
    }
    
    /**
     * Listens for changes in friend libraries.
     */
    @Inject
    void registerToRemoteLibraryManager() {    
        currentFriendFilterChanger.registerListeners();
        remoteLibraryManager.getSwingFriendLibraryList().addListEventListener(new ListEventListener<FriendLibrary>(){
            @Override
            public void listChanged(ListEvent<FriendLibrary> listChanges) {
                while(listChanges.next()) {
                    EventList<FriendLibrary> item = listChanges.getSourceList();
                    for(FriendLibrary library : item) {
                        updateLibrary(library);
                    }
                }
            }            
        });
    }
    
    /**
     * If the library of the friend that is currently selected has changed, 
     * update the library state this friend. If the friend is
     * not currently displayed, don't perform any task. 
     * 
     * This handles most of the work of updating the state of the current library. If the
     * state changed and no library is applied, the message will be immediately updated. 
     */
    private void updateLibrary(final FriendLibrary friendLibrary) {
          // if the presence changed for the friend currently displayed, update the library state
          if(currentFriend != null && friendLibrary.getFriend().getId().equals(currentFriend.getId())) {
              updateEmptyMessage(friendLibrary.getFriend());
          }
    }
    
    /**
     * Adds a listener that is notified when the friend in the current view has changed.
     */
    public void addFriendListener(ListSourceChanger.ListChangedListener listener) {
        currentFriendFilterChanger.addListener(listener);
    }
    
    private void updateEmptyMessage(Friend friend) {
        // if friend is null must be All Friends view
        if(friend != null) {
            FriendLibrary friendLibrary = remoteLibraryManager.getFriendLibrary(friend);
            if(friendLibrary != null)
                setLibraryState(friendLibrary.getState());
            else {
                // no library presence which means they're either offline or not using LW
                if(friendManager.containsAvailableFriend(friend.getId()))
                    emptyMessagePanel.setMessageType(MessageTypes.ONLINE);
                else
                    emptyMessagePanel.setMessageType(MessageTypes.OFFLINE);
            }
        } else {
            // empty library in All Friends view
            emptyMessagePanel.setMessageType(MessageTypes.ALL_FRIENDS);
        }
    }
    
    /**
     * Displays the appropriate empty library state depending on the state of the 
     * library.
     */
    private void setLibraryState(LibraryState libraryState) {
        switch(libraryState) { 
        case FAILED_TO_LOAD:
            emptyMessagePanel.setMessageType(MessageTypes.LW_CONNECTION_ERROR);
            break;
        case LOADED:
            emptyMessagePanel.setMessageType(MessageTypes.LW_NO_FILES);
            break;
        case LOADING:
            emptyMessagePanel.setMessageType(MessageTypes.LW_LOADING);
            break;
        }
    }
    
    /**
     * Updates a Set of friends that are online. Currently there is no easy way
     * to lookup if a friend is online, we must keep our own Set as a result. If the
     * friend's status changes while they are in view, the message that is displayed
     * is updated appropriately. In all cases the set will be updated appropriately.
     */
    @Inject void register(@Named("available")ListenerSupport<FriendEvent> availListeners) {
        availListeners.addListener(new EventListener<FriendEvent>(){
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendEvent event) {
                switch(event.getType()) {
                case ADDED:
                    //if friend signed on, show online view
                    if(currentFriend != null && event.getData().getId().equals(currentFriend.getId()))
                        updateEmptyMessage(currentFriend);
                    break;
                case REMOVED:
                    //if this friend signed off, show offline view
                    if(currentFriend != null && event.getData().getId().equals(currentFriend.getId()))
                        updateEmptyMessage(currentFriend);
                    break;
                }
            }
        });
    }
       
    /**
     * Returns the currently selected Friend. If All Friends
     * is selected, this will return null.
     */
    public Friend getSelectedFriend() {
        return currentFriend;
    }
    
    protected void createMyCategories(EventList<RemoteFileItem> eventList) {
        for(Category category : Category.getCategoriesInOrder()) {
            FilterList<RemoteFileItem> filtered = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
            JComponent component = createMyCategoryAction(category, filtered);
            if(component != null) {
                addCategory(categoryIconManager.getIcon(category), category, component, filtered, null);                
            }
            addDisposable(filtered);
        }
    }
    
    protected JComponent createMyCategoryAction(Category category, EventList<RemoteFileItem> filtered) {
        addFriendInfoBar(category, filtered);
        
        FilterList<RemoteFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<RemoteFileItem>(getFilterTextField(), new LibraryTextFilterator<RemoteFileItem>()));
        addDisposable(filterList);
        
        LibraryTable table = tableFactory.createFriendTable(category, filterList);
        table.setTransferHandler(transferHandler); 
        table.enableDownloading(downloadListManager, libraryNavigator, libraryManager.getLibraryManagedList());
        addDisposable(table);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setColumnHeaderView(table.getTableHeader());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        return scrollPane;
    }
    
    @Override
    protected <T extends FileItem> void addCategorySizeListener(Category category,
            Action action, FilterList<T> filteredAllFileList, FilterList<T> filteredList) {
        // Comment this out & return null if you don't want sizes added to library panels.
        ButtonSizeListener<T> listener = new ButtonSizeListener<T>(category, action, filteredList);
        filteredList.addListEventListener(listener);
        addDisposable(listener);
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
        }

        private void setText() {
            if(category == Category.PROGRAM) { // hide program category is not enabled
                action.setEnabled(LibrarySettings.ALLOW_PROGRAMS.getValue());
            }
            //disable any category if size is 0
            action.setEnabled(list.size() > 0);
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
    
    /**
     * Drops with this handler will add the file to the ManagedLibrary and share
     * with this friend
     */
    private class FriendLibraryTransferHandler extends TransferHandler {
        private FriendLibraryPanel libraryPanel;

        public FriendLibraryTransferHandler(FriendLibraryPanel panel) {
            this.libraryPanel = panel;
        }

        @Override
        public int getSourceActions(JComponent comp) {
            return COPY;
        }

        /**
         * Don't allow drops on All Friends or anonymous browse hosts
         */
        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            return DNDUtils.containsFileFlavors(info) && libraryPanel.getSelectedFriend() != null 
                    && !libraryPanel.getSelectedFriend().isAnonymous();
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!info.isDrop()) {
                return false;
            }

            Transferable t = info.getTransferable();

            final List<File> fileList;
            try {
                fileList = Arrays.asList(DNDUtils.getFiles(t));
            } catch (Exception e) {
                return false;
            }

            for (File file : fileList) {
                if (file.isDirectory()) {
                    shareListManager.getFriendShareList(libraryPanel.getSelectedFriend()).addFolder(file);
                } else {
                    shareListManager.getFriendShareList(libraryPanel.getSelectedFriend()).addFile(file);
                }
            }
            return true;
        }
    }
}
