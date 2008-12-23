package org.limewire.ui.swing.library;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.dnd.LocalFileListTransferHandler;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableModel;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.table.MouseableTable.TableColors;
import org.limewire.ui.swing.util.CategoryIconManager;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

abstract class AbstractFriendLibraryPanel extends LibraryPanel {

    private final CategoryIconManager categoryIconManager;
    private final LibraryTableFactory tableFactory;
    private final DownloadListManager downloadListManager;
    private final LibraryManager libraryManager;
    private final Friend friend;
    
    public AbstractFriendLibraryPanel(Friend friend,
                    FriendFileList friendFileList,
                    EventList<RemoteFileItem> eventList, 
                    CategoryIconManager categoryIconManager, 
                    LibraryTableFactory tableFactory,
                    DownloadListManager downloadListManager,
                    LibraryManager libraryManager,
                    LimeHeaderBarFactory headerBarFactory) {        
        super(headerBarFactory);
        this.friend = friend;
        this.categoryIconManager = categoryIconManager;
        this.tableFactory = tableFactory;
        this.downloadListManager = downloadListManager;
        this.libraryManager = libraryManager;
        
       //All friends panel gives a null friend
        if(friend != null) {
            setTransferHandler(new LocalFileListTransferHandler(friendFileList));
        }
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
        FilterList<RemoteFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<RemoteFileItem>(getFilterTextField(), new LibraryTextFilterator<RemoteFileItem>()));
        addDisposable(filterList);
        
        LibraryTable table = tableFactory.createFriendTable(category, filterList, friend);
        table.enableDownloading(downloadListManager);
        addDisposable(table);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setColumnHeaderView(table.getTableHeader());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        if (table.isColumnControlVisible()) {
            scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER, table.getColumnControl());
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        }
        TableColors tableColors = new TableColors();
        table.addHighlighter(new ColorHighlighter(new AlreadyDownloadedHighlightPredicate(getTableModel(table), libraryManager.getLibraryManagedList(), downloadListManager), 
                    null, tableColors.getDisabledForegroundColor(), null, tableColors.getDisabledForegroundColor()));
        
        return scrollPane;
    }
    
    @SuppressWarnings("unchecked")
    private LibraryTableModel<RemoteFileItem> getTableModel(LibraryTable table){
        return (LibraryTableModel<RemoteFileItem>)table.getModel();
    }   
    
    /**
     * Grays out a table row if a file is already downloading or
     * if the file already exists in your library.
     */
    private static class AlreadyDownloadedHighlightPredicate implements HighlightPredicate {
        private final LibraryTableModel<RemoteFileItem> libraryTableModel;
        private final LocalFileList myLibraryList;
        private final DownloadListManager downloadListManager;
        
        public AlreadyDownloadedHighlightPredicate (LibraryTableModel<RemoteFileItem> libraryTableModel, LocalFileList myLibraryList, DownloadListManager downloadListManager) {
            this.libraryTableModel = libraryTableModel;
            this.myLibraryList = myLibraryList;
            this.downloadListManager = downloadListManager;
        }
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            RemoteFileItem fileItem = libraryTableModel.getFileItem(adapter.row);
            //TODO cache values?
            return myLibraryList.contains(fileItem.getUrn()) || downloadListManager.contains(fileItem.getUrn());
        }       
    }
}