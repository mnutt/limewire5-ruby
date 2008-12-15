package org.limewire.ui.swing.library.table;

import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.TransferHandler;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.dnd.FriendLibraryNavTransferHandler;
import org.limewire.ui.swing.dnd.MyLibraryTransferHandler;
import org.limewire.ui.swing.dnd.RemoteFileTransferable;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.image.LibraryImageSubPanelFactory;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.library.sharing.SharingCheckBoxRendererEditor;
import org.limewire.ui.swing.library.table.menu.FriendLibraryPopupHandler;
import org.limewire.ui.swing.library.table.menu.MyLibraryPopupHandler;
import org.limewire.ui.swing.library.table.menu.MyImageLibraryPopupHandler.ImageLibraryPopupParams;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.IconLabelRenderer;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.DNDUtils;
import org.limewire.ui.swing.util.EventListJXTableSorting;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.EventSelectionModel;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class LibraryTableFactoryImpl implements LibraryTableFactory {

    private LibraryManager libraryManager;

    private ShareListManager shareListManager;

    private AudioPlayer player;

    // only accessed on EDT
  //  private List<SharingTarget> friendList = new ArrayList<SharingTarget>();

    private DownloadListManager downloadListManager;

    private MagnetLinkFactory magnetLinkFactory;

    private PropertiesFactory<LocalFileItem> localItemPropFactory;

    private PropertiesFactory<RemoteFileItem> remoteItemPropFactory;
    
    private LibraryImageSubPanelFactory subPanelFactory;
    
    private final SaveLocationExceptionHandler saveLocationExceptionHandler;
    
    private final TimeRenderer timeRenderer = new TimeRenderer();
    private final FileSizeRenderer fileSizeRenderer = new FileSizeRenderer();
    private final CalendarRenderer calendarRenderer = new CalendarRenderer();
    private final IconLabelRenderer iconLabelRenderer;
    private final IconManager iconManager;
    private final ShareTableRendererEditorFactory shareTableRendererEditorFactory;

    private Collection<Friend> allFriends;

    private ShareWidgetFactory shareFactory;

    @Inject
    public LibraryTableFactoryImpl(IconManager iconManager,
            LibraryManager libraryManager, 
            ShareListManager shareListManager, 
            AudioPlayer player,
            DownloadListManager downloadListManager, 
            MagnetLinkFactory magnetLinkFactory,
            PropertiesFactory<LocalFileItem> localItemPropFactory,
            PropertiesFactory<RemoteFileItem> remoteItemPropFactory,
            LibraryImageSubPanelFactory factory, 
            SaveLocationExceptionHandler saveLocationExceptionHandler,
            @Named("known") Collection<Friend> allFriends,
            ShareTableRendererEditorFactory shareTableRendererEditorFactory, 
            ShareWidgetFactory shareFactory) {
        this.iconManager = iconManager;
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.player = player;
        this.downloadListManager = downloadListManager;
        this.magnetLinkFactory = magnetLinkFactory;
        this.localItemPropFactory = localItemPropFactory;
        this.remoteItemPropFactory = remoteItemPropFactory;
        this.subPanelFactory = factory;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        this.allFriends = allFriends;
        this.shareFactory = shareFactory;
        
        this.shareTableRendererEditorFactory = shareTableRendererEditorFactory;
        iconLabelRenderer = new IconLabelRenderer(iconManager);
    }

//    @Inject void register(@Named("known") ListenerSupport<FriendEvent> knownFriends) {
//        knownFriends.addListener(new EventListener<FriendEvent>() {
//            @Override
//            @SwingEDTEvent
//            public void handleEvent(FriendEvent event) {
//                switch(event.getType()) {
//                case ADDED:
//                    friendList.add(new SharingTarget(event.getSource()));
//                    break;
//                case REMOVED:
//                    friendList.remove(new SharingTarget(event.getSource()));
//                    break;
//                }
//            }
//        });
//    }
//    
    /**
     * Creates an panel that displays images as thumbnails.
     */
    @Override
    public LibraryImagePanel createImagePanel(EventList<LocalFileItem> eventList,
            JScrollPane scrollPane, ShareWidget<File> sharePanel) {
        ImageLibraryPopupParams params = new ImageLibraryPopupParams(libraryManager,
                shareListManager, magnetLinkFactory, allFriends, localItemPropFactory, shareFactory);
        
        LibraryImagePanel imagePanel = new LibraryImagePanel(I18n.tr(Category.IMAGE.name()), params, eventList,
                libraryManager.getLibraryManagedList(), scrollPane, subPanelFactory,
                sharePanel, null);       

        TransferHandler noDragTransferHandler = new MyLibraryTransferHandler(null, libraryManager.getLibraryManagedList()){
            @Override
            public int getSourceActions(JComponent comp) {
                return NONE;
            }
        };
        
        imagePanel.setTransferHandler(noDragTransferHandler);
        return imagePanel;
    }
    
    @Override
    public LibraryImagePanel createSharingImagePanel(EventList<LocalFileItem> eventList,
            JScrollPane scrollPane, LocalFileList currentFriendList) {
        ImageLibraryPopupParams params = new ImageLibraryPopupParams(libraryManager,
                shareListManager, magnetLinkFactory, allFriends, localItemPropFactory, shareFactory);
        return new LibraryImagePanel(I18n.tr(Category.IMAGE.name()), params, eventList,
                libraryManager.getLibraryManagedList(), scrollPane,
                subPanelFactory, null, currentFriendList);
    }

    /**
     * Creates a table for MyLibrary
     */
    public <T extends LocalFileItem> LibraryTable<T> createMyTable(Category category, EventList<T> eventList) {

        final LibraryTable<T> libTable;
        SortedList<T> sortedList = new SortedList<T>(eventList);

        switch (category) {
        case AUDIO:
            libTable = new AudioLibraryTable<T>(sortedList, player, saveLocationExceptionHandler, shareTableRendererEditorFactory);
            break;
        case VIDEO:
            libTable = new LibraryTable<T>(sortedList,  new VideoTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(VideoTableFormat.Columns.LENGTH.ordinal()).setCellRenderer(timeRenderer);
            libTable.getColumnModel().getColumn(VideoTableFormat.Columns.SIZE.ordinal()).setCellRenderer(fileSizeRenderer);
            break;
        case DOCUMENT:
            libTable = new LibraryTable<T>(sortedList, new DocumentTableFormat<T>(iconManager), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(DocumentTableFormat.Columns.NAME.ordinal()).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(DocumentTableFormat.Columns.SIZE.ordinal()).setCellRenderer(fileSizeRenderer);
            break;
        case OTHER:
            libTable = new LibraryTable<T>(sortedList, new OtherTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(OtherTableFormat.Columns.NAME.ordinal()).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(OtherTableFormat.Columns.SIZE.ordinal()).setCellRenderer(fileSizeRenderer);
            break;
        case PROGRAM:
            libTable = new LibraryTable<T>(sortedList, new ProgramTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(ProgramTableFormat.Columns.NAME.ordinal()).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(ProgramTableFormat.Columns.SIZE.ordinal()).setCellRenderer(fileSizeRenderer);
            break;
        default:
            throw new IllegalArgumentException("Unknown category: " + category);
        }

        libTable.setTransferHandler(new MyLibraryTransferHandler(getSelectionModel(libTable), libraryManager.getLibraryManagedList()));
        libTable.setPopupHandler(new MyLibraryPopupHandler(castToLocalLibraryTable(libTable),
                category, libraryManager, shareListManager, magnetLinkFactory, allFriends,
                localItemPropFactory, shareFactory));

        EventListJXTableSorting.install(libTable, sortedList);
        libTable.setDropMode(DropMode.ON);

        return libTable;

    }    
    
    /**
     * Creates a table when viewing a Friend's Library or performing a BrowseHost
     */
    public <T extends RemoteFileItem> LibraryTable<T> createFriendTable(Category category, EventList<T> eventList, Friend friend) {

        LibraryTable<T> libTable;
        SortedList<T> sortedList = GlazedListsFactory.sortedList(eventList);

        switch (category) {
        case AUDIO:
            libTable = new LibraryTable<T>(sortedList, new RemoteAudioTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(RemoteAudioTableFormat.Columns.LENGTH.ordinal()).setCellRenderer(timeRenderer);
            libTable.getColumnModel().getColumn(RemoteAudioTableFormat.Columns.SIZE.ordinal()).setCellRenderer(fileSizeRenderer);
            break;
        case VIDEO:
            libTable = new LibraryTable<T>(sortedList, new RemoteVideoTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(RemoteVideoTableFormat.Columns.LENGTH.ordinal()).setCellRenderer(timeRenderer);
            libTable.getColumnModel().getColumn(RemoteVideoTableFormat.Columns.SIZE.ordinal()).setCellRenderer(fileSizeRenderer);
            break;
        case DOCUMENT:
            libTable = new LibraryTable<T>(sortedList, new RemoteDocumentTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(RemoteDocumentTableFormat.Columns.NAME.ordinal()).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(RemoteDocumentTableFormat.Columns.SIZE.ordinal()).setCellRenderer(fileSizeRenderer);
            libTable.getColumnModel().getColumn(RemoteDocumentTableFormat.Columns.CREATED.ordinal()).setCellRenderer(calendarRenderer);
            break;
        case IMAGE:
            libTable = new LibraryTable<T>(sortedList, new RemoteImageTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(RemoteImageTableFormat.Columns.SIZE.ordinal()).setCellRenderer(fileSizeRenderer);
            libTable.getColumnModel().getColumn(RemoteImageTableFormat.Columns.CREATED.ordinal()).setCellRenderer(calendarRenderer);
            break;
        case OTHER:
            libTable = new LibraryTable<T>(sortedList, new RemoteOtherTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(RemoteOtherTableFormat.Columns.NAME.ordinal()).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(RemoteOtherTableFormat.Columns.SIZE.ordinal()).setCellRenderer(fileSizeRenderer);
            break;
        case PROGRAM:
            libTable = new LibraryTable<T>(sortedList, new RemoteProgramTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(RemoteProgramTableFormat.Columns.NAME.ordinal()).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(RemoteProgramTableFormat.Columns.SIZE.ordinal()).setCellRenderer(fileSizeRenderer);
            break;
        default:
            throw new IllegalArgumentException("Unknown category: " + category);
        }

        if(friend != null) {
            libTable.setTransferHandler(new FriendLibraryTransferHandler(libTable, friend));
        }
        libTable.setPopupHandler(new FriendLibraryPopupHandler(
                castToRemoteLibraryTable(libTable), downloadListManager, magnetLinkFactory,
                remoteItemPropFactory, saveLocationExceptionHandler));

        EventListJXTableSorting.install(libTable, sortedList);
        libTable.setDropMode(DropMode.ON);

        return libTable;

    }    

    /**
     * Creates a table for sharing files from your library with a friend.
     */
    @Override
    public <T extends LocalFileItem> LibraryTable<T> createSharingTable(Category category, EventList<T> eventList, LocalFileList friendFileList) {
        final LibraryTable<T> libTable;
        SortedList<T> sortedList = new SortedList<T>(eventList);
        
        switch (category) {
        case AUDIO:
            libTable = new AudioLibraryTable<T>(sortedList, player, saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(AudioTableFormat.Columns.ACTION.ordinal()).setCellRenderer(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().getColumn(AudioTableFormat.Columns.ACTION.ordinal()).setCellEditor(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().moveColumn(AudioTableFormat.Columns.ACTION.ordinal(), 0);
            break;
        case VIDEO:
            libTable = new LibraryTable<T>(sortedList, new SharedVideoTableFormat<T>(friendFileList), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(VideoTableFormat.Columns.LENGTH.ordinal()).setCellRenderer(timeRenderer);
            libTable.getColumnModel().getColumn(VideoTableFormat.Columns.SIZE.ordinal()).setCellRenderer(fileSizeRenderer);
            libTable.getColumnModel().getColumn(VideoTableFormat.Columns.ACTION.ordinal()).setCellRenderer(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().getColumn(VideoTableFormat.Columns.ACTION.ordinal()).setCellEditor(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().moveColumn(VideoTableFormat.Columns.ACTION.ordinal(), 0);
            break;
        case DOCUMENT:
            libTable = new LibraryTable<T>(sortedList, new SharedDocumentTableFormat<T>(friendFileList), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(DocumentTableFormat.Columns.NAME.ordinal()).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(DocumentTableFormat.Columns.SIZE.ordinal()).setCellRenderer(fileSizeRenderer);
            libTable.getColumnModel().getColumn(DocumentTableFormat.Columns.ACTION.ordinal()).setCellRenderer(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().getColumn(DocumentTableFormat.Columns.ACTION.ordinal()).setCellEditor(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().moveColumn(DocumentTableFormat.Columns.ACTION.ordinal(), 0);
            break;
        case OTHER:
            libTable = new LibraryTable<T>(sortedList, new SharedOtherTableFormat<T>(friendFileList), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(OtherTableFormat.Columns.NAME.ordinal()).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(OtherTableFormat.Columns.SIZE.ordinal()).setCellRenderer(fileSizeRenderer);
            libTable.getColumnModel().getColumn(OtherTableFormat.Columns.ACTION.ordinal()).setCellRenderer(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().getColumn(OtherTableFormat.Columns.ACTION.ordinal()).setCellEditor(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().moveColumn(OtherTableFormat.Columns.ACTION.ordinal(), 0);
            break;
        case PROGRAM:
            libTable = new LibraryTable<T>(sortedList, new SharedProgramTableFormat<T>(friendFileList), saveLocationExceptionHandler, shareTableRendererEditorFactory);
            libTable.getColumnModel().getColumn(ProgramTableFormat.Columns.NAME.ordinal()).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(ProgramTableFormat.Columns.SIZE.ordinal()).setCellRenderer(fileSizeRenderer);
            libTable.getColumnModel().getColumn(ProgramTableFormat.Columns.ACTION.ordinal()).setCellRenderer(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().getColumn(ProgramTableFormat.Columns.ACTION.ordinal()).setCellEditor(new SharingCheckBoxRendererEditor(friendFileList, libTable));
            libTable.getColumnModel().moveColumn(ProgramTableFormat.Columns.ACTION.ordinal(), 0);
            break;
        default:
            throw new IllegalArgumentException("Unknown category: " + category);
        }
        
        
        libTable.setTransferHandler(new FriendLibraryNavTransferHandler(friendFileList));
        libTable.setPopupHandler(new MyLibraryPopupHandler(castToLocalLibraryTable(libTable), category, libraryManager, shareListManager, 
                    magnetLinkFactory, allFriends, localItemPropFactory, shareFactory));
        
        EventListJXTableSorting.install(libTable, sortedList);
        libTable.setDropMode(DropMode.ON);
        
        return libTable;
    }

    @SuppressWarnings( { "unchecked", "cast" })
    private LibraryTable<RemoteFileItem> castToRemoteLibraryTable(LibraryTable table) {
        return (LibraryTable<RemoteFileItem>) table;
    }

    @SuppressWarnings( { "unchecked", "cast" })
    private LibraryTable<LocalFileItem> castToLocalLibraryTable(LibraryTable table) {
        return (LibraryTable<LocalFileItem>) table;
    }
    
    @SuppressWarnings("unchecked")
    private EventSelectionModel<LocalFileItem> getSelectionModel(LibraryTable table){
        return (EventSelectionModel<LocalFileItem>) table.getSelectionModel();
    }


    private class FriendLibraryTransferHandler extends TransferHandler {

        private LibraryTable table;

        private Friend friend;

        public FriendLibraryTransferHandler(LibraryTable table, Friend friend) {
            this.table = table;
            this.friend = friend;
        }

        @Override
        public int getSourceActions(JComponent comp) {
            return COPY;
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            return DNDUtils.containsFileFlavors(info);
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
                    shareListManager.getFriendShareList(friend).addFolder(file);
                } else {
                    shareListManager.getFriendShareList(friend).addFile(file);
                }
            }
            return true;
        }

        @Override
        public Transferable createTransferable(JComponent comp) {
            int indices[] = table.getSelectedRows();
            List<RemoteFileItem> files = new ArrayList<RemoteFileItem>();
            for (int i = 0; i < indices.length; i++) {
                files.add((RemoteFileItem) ((LibraryTableModel) table.getModel())
                        .getFileItem(indices[i]));
            }
            return new RemoteFileTransferable(files);
        }
    }
}
