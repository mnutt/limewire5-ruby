package org.limewire.ui.swing.library.table;

import java.io.File;
import java.util.TooManyListenersException;

import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.TransferHandler;
import javax.swing.table.DefaultTableCellRenderer;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.api.playlist.Playlist;
import org.limewire.inject.LazySingleton;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.components.RemoteHostWidgetFactory;
import org.limewire.ui.swing.components.RemoteHostWidget.RemoteWidgetType;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.dnd.GhostDropTargetListener;
import org.limewire.ui.swing.dnd.MyLibraryTransferHandler;
import org.limewire.ui.swing.library.LibraryListSourceChanger;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.image.LibraryImageSubPanelFactory;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.playlist.PlaylistLibraryTable;
import org.limewire.ui.swing.library.playlist.PlaylistPopupHandler;
import org.limewire.ui.swing.library.playlist.PlaylistTableFormat;
import org.limewire.ui.swing.library.playlist.PlaylistTransferHandler;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.table.menu.FriendLibraryPopupHandler;
import org.limewire.ui.swing.library.table.menu.FriendLibraryPopupMenu;
import org.limewire.ui.swing.library.table.menu.MyLibraryPopupHandler;
import org.limewire.ui.swing.library.table.menu.MyLibraryPopupMenuFactory;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.search.resultpanel.classic.FromTableCellRenderer;
import org.limewire.ui.swing.table.CalendarRenderer;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.IconLabelRenderer;
import org.limewire.ui.swing.table.IconLabelRendererFactory;
import org.limewire.ui.swing.table.NameRenderer;
import org.limewire.ui.swing.table.QualityRenderer;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.EventListJXTableSorting;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.EventSelectionModel;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class LibraryTableFactoryImpl implements LibraryTableFactory {

    private LibraryManager libraryManager;

    private ShareListManager shareListManager;

    private AudioPlayer player;
    
    private Provider<LibraryImageSubPanelFactory> subPanelFactory;
    
    private final Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler;
    
    private final RemoteHostWidgetFactory fromWidgetFactory;
    
    private final Provider<TimeRenderer> timeRenderer;
    private final Provider<FileSizeRenderer> fileSizeRenderer;
    private final Provider<CalendarRenderer> calendarRenderer;
    private final Provider<QualityRenderer> qualityRenderer;
    private final TableCellHeaderRenderer rightAlignedHeader = new TableCellHeaderRenderer(JLabel.TRAILING);
    private final IconLabelRenderer iconLabelRenderer;
    private final Provider<NameRenderer> nameRenderer;
    private final Provider<IconManager> iconManager;
    private final ShareTableRendererEditorFactory shareTableRendererEditorFactory;
    private final GhostDragGlassPane ghostPane;
    private final LibraryNavigator libraryNavigator;
    private final FileInfoDialogFactory fileInfoFactory;
    private final MyLibraryPopupMenuFactory popupFactory;
    private final Provider<FriendLibraryPopupMenu> friendLibraryPopupMenu;
    private final DefaultTableCellRenderer defaultTableCellRenderer;

    @Inject
    public LibraryTableFactoryImpl(Provider<IconManager> iconManager,
            LibraryManager libraryManager, 
            ShareListManager shareListManager, 
            AudioPlayer player,
            Provider<LibraryImageSubPanelFactory> factory, 
            Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler,
            ShareTableRendererEditorFactory shareTableRendererEditorFactory, 
            GhostDragGlassPane ghostPane, 
            RemoteHostWidgetFactory fromWidgetfactory,
            LibraryNavigator libraryNavigator,
            FileInfoDialogFactory fileInfoFactory,
            MyLibraryPopupMenuFactory popupFactory,
            Provider<FriendLibraryPopupMenu> friendLibraryPopupMenu,
            Provider<TimeRenderer> timeRenderer, Provider<FileSizeRenderer> fileSizeRenderer,
            Provider<CalendarRenderer> calendarRenderer, Provider<NameRenderer> nameRenderer,
            IconLabelRendererFactory iconLabelRendererFactory,
            Provider<QualityRenderer> qualityRenderer, DefaultTableCellRenderer defaultTableCellRenderer) {
        this.iconManager = iconManager;
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.player = player;
        this.subPanelFactory = factory;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        this.ghostPane = ghostPane;
        this.fromWidgetFactory = fromWidgetfactory;
        this.libraryNavigator = libraryNavigator;
        this.fileInfoFactory = fileInfoFactory;
        this.popupFactory = popupFactory;
        this.friendLibraryPopupMenu = friendLibraryPopupMenu;
        this.timeRenderer = timeRenderer;
        this.fileSizeRenderer = fileSizeRenderer;
        this.calendarRenderer = calendarRenderer;
        this.nameRenderer = nameRenderer;
        this.qualityRenderer = qualityRenderer;
        this.defaultTableCellRenderer = defaultTableCellRenderer;
        
        this.shareTableRendererEditorFactory = shareTableRendererEditorFactory;
        iconLabelRenderer = iconLabelRendererFactory.createIconRenderer(false);
    }


    /**
     * Creates a table for MyLibrary
     */
    public <T extends LocalFileItem> LibraryTable<T> createMyTable(Category category,
            EventList<T> eventList, LibraryListSourceChanger listChanger) {

        final LibraryTable<T> libTable;
        SortedList<T> sortedList = new SortedList<T>(eventList);

        switch (category) {
        case AUDIO:
            libTable = new AudioLibraryTable<T>(sortedList, player, saveLocationExceptionHandler, shareTableRendererEditorFactory, timeRenderer, fileSizeRenderer, nameRenderer, defaultTableCellRenderer);
            libTable.getColumnModel().getColumn(AudioTableFormat.LENGTH_INDEX).setHeaderRenderer(rightAlignedHeader);
            libTable.getColumnModel().getColumn(AudioTableFormat.QUALITY_INDEX).setCellRenderer(qualityRenderer.get());
            break;
        case VIDEO:
            libTable = new LibraryTable<T>(sortedList,  new VideoTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory, defaultTableCellRenderer);
            libTable.getColumnModel().getColumn(VideoTableFormat.LENGTH_INDEX).setHeaderRenderer(rightAlignedHeader);
            libTable.getColumnModel().getColumn(VideoTableFormat.LENGTH_INDEX).setCellRenderer(timeRenderer.get());
            libTable.getColumnModel().getColumn(VideoTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer.get());
            libTable.getColumnModel().getColumn(VideoTableFormat.NAME_INDEX).setCellRenderer(nameRenderer.get());
            break;
        case DOCUMENT:
            libTable = new LibraryTable<T>(sortedList, new DocumentTableFormat<T>(iconManager), saveLocationExceptionHandler, shareTableRendererEditorFactory, defaultTableCellRenderer);
            libTable.getColumnModel().getColumn(DocumentTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(DocumentTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer.get());
            break;
        case OTHER:
            libTable = new LibraryTable<T>(sortedList, new OtherTableFormat<T>(iconManager), saveLocationExceptionHandler, shareTableRendererEditorFactory, defaultTableCellRenderer);
            libTable.getColumnModel().getColumn(OtherTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(OtherTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer.get());
            break;
        case PROGRAM:
            libTable = new LibraryTable<T>(sortedList, new ProgramTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory, defaultTableCellRenderer);
            libTable.getColumnModel().getColumn(ProgramTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(ProgramTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer.get());
            break;
        default:
            throw new IllegalArgumentException("Unknown category: " + category);
        }

        libTable.setTransferHandler(new MyLibraryTransferHandler(getSelectionModel(libTable), libraryManager.getLibraryManagedList(), shareListManager, listChanger));
        libTable.setPopupHandler(new MyLibraryPopupHandler(castToLocalLibraryTable(libTable), popupFactory.createMyLibraryPopupMenu(category)));
        
        try {
            libTable.getDropTarget().addDropTargetListener(new GhostDropTargetListener(libTable, ghostPane, listChanger));
        } catch (TooManyListenersException ingoreException) {
        }

        EventListJXTableSorting.install(libTable, sortedList, libTable.getTableFormat());
        libTable.setDropMode(DropMode.ON);

        return libTable;
    }    

	/**
	 * Creates Image Library for My Library which displays thumbnails.
	 */
    @Override
    public LibraryImagePanel createMyImagePanel(EventList<LocalFileItem> eventList,
            JScrollPane scrollPane, ShareWidget<File> sharePanel,
            LibraryListSourceChanger listChanger) {
        LibraryImagePanel imagePanel = new LibraryImagePanel(I18n.tr(Category.IMAGE.name()), eventList,
                libraryManager.getLibraryManagedList(), scrollPane, subPanelFactory,
                sharePanel, listChanger);

        TransferHandler noDragTransferHandler = new MyLibraryTransferHandler(null, libraryManager
                .getLibraryManagedList(), shareListManager, listChanger) {
            @Override
            public int getSourceActions(JComponent comp) {
                return NONE;
            }
        };
        
        imagePanel.setTransferHandler(noDragTransferHandler);
        try {
            imagePanel.getDropTarget().addDropTargetListener(new GhostDropTargetListener(imagePanel, ghostPane, listChanger));
        } catch (TooManyListenersException ingoreException) {
        }
        return imagePanel;
    }    
    
    /**
     * Creates a table when viewing a Friend's Library or performing a BrowseHost
     */
    public <T extends RemoteFileItem> LibraryTable<T> createFriendTable(Category category, EventList<T> eventList) {

        LibraryTable<T> libTable;
        SortedList<T> sortedList = GlazedListsFactory.sortedList(eventList);

        switch (category) {
        case AUDIO:
            libTable = new LibraryTable<T>(sortedList, new RemoteAudioTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory, defaultTableCellRenderer);
            libTable.getColumnModel().getColumn(RemoteAudioTableFormat.LENGTH_INDEX).setHeaderRenderer(rightAlignedHeader);
            libTable.getColumnModel().getColumn(RemoteAudioTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(RemoteAudioTableFormat.LENGTH_INDEX).setCellRenderer(timeRenderer.get());
            libTable.getColumnModel().getColumn(RemoteAudioTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer.get());
            libTable.getColumnModel().getColumn(RemoteAudioTableFormat.QUALITY_INDEX).setCellRenderer(qualityRenderer.get());
            libTable.getColumnModel().getColumn(RemoteAudioTableFormat.FROM_INDEX).setCellRenderer(new FromTableCellRenderer(fromWidgetFactory.create(RemoteWidgetType.TABLE)));
            libTable.getColumnModel().getColumn(RemoteAudioTableFormat.FROM_INDEX).setCellEditor(new FromTableCellRenderer(fromWidgetFactory.create(RemoteWidgetType.TABLE)));
            break;
        case VIDEO:
            libTable = new LibraryTable<T>(sortedList, new RemoteVideoTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory, defaultTableCellRenderer);
            libTable.getColumnModel().getColumn(RemoteVideoTableFormat.LENGTH_INDEX).setHeaderRenderer(rightAlignedHeader);
            libTable.getColumnModel().getColumn(RemoteVideoTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(RemoteVideoTableFormat.LENGTH_INDEX).setCellRenderer(timeRenderer.get());
            libTable.getColumnModel().getColumn(RemoteVideoTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer.get());
            libTable.getColumnModel().getColumn(RemoteVideoTableFormat.QUALITY_INDEX).setCellRenderer(qualityRenderer.get());
            libTable.getColumnModel().getColumn(RemoteVideoTableFormat.FROM_INDEX).setCellRenderer(new FromTableCellRenderer(fromWidgetFactory.create(RemoteWidgetType.TABLE)));
            libTable.getColumnModel().getColumn(RemoteVideoTableFormat.FROM_INDEX).setCellEditor(new FromTableCellRenderer(fromWidgetFactory.create(RemoteWidgetType.TABLE)));
            break;
        case DOCUMENT:
            libTable = new LibraryTable<T>(sortedList, new RemoteDocumentTableFormat<T>(iconManager), saveLocationExceptionHandler, shareTableRendererEditorFactory, defaultTableCellRenderer);
            libTable.getColumnModel().getColumn(RemoteDocumentTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(RemoteDocumentTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer.get());
            libTable.getColumnModel().getColumn(RemoteDocumentTableFormat.CREATED_INDEX).setCellRenderer(calendarRenderer.get());
            libTable.getColumnModel().getColumn(RemoteDocumentTableFormat.FROM_INDEX).setCellRenderer(new FromTableCellRenderer(fromWidgetFactory.create(RemoteWidgetType.TABLE)));
            libTable.getColumnModel().getColumn(RemoteDocumentTableFormat.FROM_INDEX).setCellEditor(new FromTableCellRenderer(fromWidgetFactory.create(RemoteWidgetType.TABLE)));
            break;
        case IMAGE:
            libTable = new LibraryTable<T>(sortedList, new RemoteImageTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory, defaultTableCellRenderer);
            libTable.getColumnModel().getColumn(RemoteImageTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(RemoteImageTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer.get());
            libTable.getColumnModel().getColumn(RemoteImageTableFormat.CREATED_INDEX).setCellRenderer(calendarRenderer.get());
            libTable.getColumnModel().getColumn(RemoteImageTableFormat.FROM_INDEX).setCellRenderer(new FromTableCellRenderer(fromWidgetFactory.create(RemoteWidgetType.TABLE)));
            libTable.getColumnModel().getColumn(RemoteImageTableFormat.FROM_INDEX).setCellEditor(new FromTableCellRenderer(fromWidgetFactory.create(RemoteWidgetType.TABLE)));
            break;
        case OTHER:
            libTable = new LibraryTable<T>(sortedList, new RemoteOtherTableFormat<T>(iconManager), saveLocationExceptionHandler, shareTableRendererEditorFactory, defaultTableCellRenderer);
            libTable.getColumnModel().getColumn(RemoteOtherTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(RemoteOtherTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer.get());
            libTable.getColumnModel().getColumn(RemoteOtherTableFormat.FROM_INDEX).setCellRenderer(new FromTableCellRenderer(fromWidgetFactory.create(RemoteWidgetType.TABLE)));
            libTable.getColumnModel().getColumn(RemoteOtherTableFormat.FROM_INDEX).setCellEditor(new FromTableCellRenderer(fromWidgetFactory.create(RemoteWidgetType.TABLE)));
            break;
        case PROGRAM:
            libTable = new LibraryTable<T>(sortedList, new RemoteProgramTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory, defaultTableCellRenderer);
            libTable.getColumnModel().getColumn(RemoteProgramTableFormat.NAME_INDEX).setCellRenderer(iconLabelRenderer);
            libTable.getColumnModel().getColumn(RemoteProgramTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer.get());
            libTable.getColumnModel().getColumn(RemoteProgramTableFormat.FROM_INDEX).setCellRenderer(new FromTableCellRenderer(fromWidgetFactory.create(RemoteWidgetType.TABLE)));
            libTable.getColumnModel().getColumn(RemoteProgramTableFormat.FROM_INDEX).setCellEditor(new FromTableCellRenderer(fromWidgetFactory.create(RemoteWidgetType.TABLE)));
            break;
        default:
            throw new IllegalArgumentException("Unknown category: " + category);
        }
        libTable.setPopupHandler(new FriendLibraryPopupHandler(castToRemoteLibraryTable(libTable), friendLibraryPopupMenu.get()));

        EventListJXTableSorting.install(libTable, sortedList, libTable.getTableFormat());
        libTable.setDropMode(DropMode.ON);

        return libTable;

    }    

    /**
     * Creates a table to display a playlist.
     */
    @Override
    public <T extends LocalFileItem> LibraryTable<T> createPlaylistTable(
            Playlist playlist, EventList<T> eventList) {
        // Create sorted list.
        SortedList<T> sortedList = GlazedListsFactory.sortedList(eventList);

        // Create table.
        LibraryTable<T> libTable = new PlaylistLibraryTable<T>(playlist, sortedList, 
                new PlaylistTableFormat<T>(), player,
                saveLocationExceptionHandler, shareTableRendererEditorFactory, timeRenderer, fileSizeRenderer, nameRenderer, qualityRenderer, defaultTableCellRenderer);
        
        // Install popup menu handler.
        libTable.setPopupHandler(new PlaylistPopupHandler(libTable, playlist, libraryNavigator, fileInfoFactory));

        // Install transfer handler to reorder playlist items.
        libTable.setTransferHandler(new PlaylistTransferHandler(playlist));
        
        // Possible drag-and-drop upgrades:
        // - Start drag to category buttons to remove file from playlist
        // - Use GhostDropTargetListener to display image on drag operation
        // - Accept drop from OS file explorer to add file to library/playlist
        
        // Install sort support.
        EventListJXTableSorting.install(libTable, sortedList, libTable.getTableFormat());
        
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
}
