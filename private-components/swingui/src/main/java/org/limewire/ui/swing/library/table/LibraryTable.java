package org.limewire.ui.swing.library.table;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.library.LibraryOperable;
import org.limewire.ui.swing.library.Sharable;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.table.ColumnStateHandler;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableColors;
import org.limewire.ui.swing.table.TableColumnSelector;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.GlazedListsSwingFactory;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.EventTableModel;

/**
 * Creates a table that is used when displaying library information. This may
 * be files in your own library or remote files in someone else's library. 
 */
public class LibraryTable<T extends FileItem> extends MouseableTable
    implements Sharable<File>, Disposable, LibraryOperable<T> {
    
    private final LibraryTableFormat<T> format;
    private final TableColors tableColors;
    private final EventList<T> listSelection;
    private final SaveLocationExceptionHandler saveLocationExceptionHandler;
    private final ShareTableRendererEditorFactory shareTableRendererEditorFactory;
    
    private TableRendererEditor shareEditor;
    private ShareWidget<File> shareWidget;
    private ColumnStateHandler columnStateHandler;
    
    protected final int rowHeight = 20;
    
    public LibraryTable(EventList<T> libraryItems, LibraryTableFormat<T> format, SaveLocationExceptionHandler saveLocationExceptionHandler, ShareTableRendererEditorFactory shareTableRendererEditorFactory) {
        super(new LibraryTableModel<T>(libraryItems, format));

        this.format = format;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        this.shareTableRendererEditorFactory = shareTableRendererEditorFactory;
        
        tableColors = new TableColors();
        setStripesPainted(true);
        setShowHorizontalLines(false);
        setShowGrid(false, true);
        
        EventSelectionModel<T> model = GlazedListsSwingFactory.eventSelectionModel(libraryItems);
        setSelectionModel(model);
        model.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        this.listSelection = model.getSelected();
        setHighlighters(tableColors.getEvenHighlighter(), tableColors.getOddHighlighter(), 
                new ColorHighlighter(new DisabledHighlightPredicate(this), null, tableColors.getDisabledForegroundColor(), null, tableColors.getDisabledForegroundColor()));
       
        setFillsViewportHeight(true);
        setDragEnabled(true);
        setRowHeight(rowHeight);
        
        // Add mouse listener to display popup menu on column header.  We use 
        // MousePopupListener to detect the popup trigger, which differs on 
        // Windows, Mac, and Linux.
        JTableHeader header = getTableHeader();
        header.addMouseListener(new MousePopupListener() {
            @Override
            public void handlePopupMouseEvent(MouseEvent e) {
                showHeaderPopupMenu(e.getPoint());
            }
        });       
        
        setupCellRenderers(format);
        setupColumnHandler();
    }

    /**
     * Creates a column handler that handles saving column state
     */
    private void setupColumnHandler() {
        columnStateHandler = new ColumnStateHandler(this, getTableFormat());
    }
    
    protected void setupCellRenderers(LibraryTableFormat<T> format) {
        TableCellRenderer defaultRenderer = new DefaultLibraryRenderer();
        for (int i = 0; i < format.getColumnCount(); i++) {
            Class clazz = format.getColumnClass(i);
            if (clazz == String.class) {
                setCellRenderer(i, defaultRenderer);
            } 
        }   
    }
    
    protected void setCellRenderer(int column, TableCellRenderer cellRenderer) {
        TableColumnModel tcm = this.getColumnModel();
        TableColumn tc = tcm.getColumn(column);
        tc.setCellRenderer(cellRenderer);
    }
    
    public void showHeaderPopupMenu(Point p) {
        JPopupMenu menu = createColumnMenu();
        menu.show(getTableHeader(), p.x, p.y);
    }
    
    public LibraryTableFormat<T> getTableFormat() {
        return format;
    }
    
    public JPopupMenu createColumnMenu() {
        return new TableColumnSelector(this, format).getPopupMenu();
    }
    
    public void enableMyLibrarySharing(ShareWidget<File> shareWidget) {
        this.shareWidget = shareWidget;
        shareEditor = shareTableRendererEditorFactory.createShareTableRendererEditor(new ShareAction(I18n.tr("Share")));
        getColumnModel().getColumn(format.getActionColumn()).setCellEditor(shareEditor);
        getColumnModel().getColumn(format.getActionColumn()).setCellRenderer(shareTableRendererEditorFactory.createShareTableRendererEditor(null));
        getColumnModel().getColumn(format.getActionColumn()).setPreferredWidth(shareEditor.getPreferredSize().width);
        getColumnModel().getColumn(format.getActionColumn()).setWidth(shareEditor.getPreferredSize().width);
        setRowHeight(rowHeight);
        setSavedColumnSettings();
    }
    
    public void enableDownloading(DownloadListManager downloadListManager, LibraryNavigator libraryNavigator, LibraryFileList libraryList){
        LibraryDownloadAction downloadAction = new LibraryDownloadAction(I18n.tr("download"), downloadListManager, this, libraryNavigator, libraryList);
        
        setDoubleClickHandler(new LibraryDownloadDoubleClickHandler(downloadAction));
        
        setSavedColumnSettings();
    }
    
    public void enableSharing() {
        setSavedColumnSettings();
    }
    
    public void dispose() {
        ((EventSelectionModel)getSelectionModel()).dispose();
        ((EventTableModel)getModel()).dispose();
        columnStateHandler.removeListeners();
    }
    
    @SuppressWarnings("unchecked")
    public LibraryTableModel<T> getLibraryTableModel(){
        return (LibraryTableModel<T>)getModel();
    }
    
    /** Returns a copy of all selected items. */
    public List<T> getSelectedItems() {
        return new ArrayList<T>(listSelection);
    }
    
    @Override
    public void setModel(TableModel newModel) {
        assert getModel() == null : "cannot change model!";
        super.setModel(newModel);
    }
    
    /**
	 * Loads the saved state of the columns. 
     *
	 * NOTE: currently this must be called after the renderers and editors
     * have been loaded. The order in which width/visiblity/order are called
     * is also expected to be in that order.
 	 */
    protected void setSavedColumnSettings(){
        columnStateHandler.setupColumnWidths();
        columnStateHandler.setupColumnVisibility();
        columnStateHandler.setupColumnOrder();
    }
    
    private class ShareAction extends AbstractAction {
        
        public ShareAction(String text){
            super(text);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // highlight the row that has the share widget was pressed in
            int selectedIndex = convertRowIndexToModel(getEditingRow());
            getSelectionModel().setSelectionInterval(selectedIndex, selectedIndex);
            
            shareWidget.setShareable(((LocalFileItem) ((LibraryTableModel)getModel()).getElementAt(selectedIndex)).getFile());
            shareWidget.show(shareEditor);
            shareEditor.cancelCellEditing();
        }
        
    }
    
    private class LibraryDownloadDoubleClickHandler implements TableDoubleClickHandler {
        private LibraryDownloadAction action;

        public LibraryDownloadDoubleClickHandler(LibraryDownloadAction action) {
            this.action = action;
        }

        @Override
        public void handleDoubleClick(int row) {
            action.download(row);
        }
    }

    private class LibraryDownloadAction extends AbstractAction {
        private LibraryFileList libraryFileList;
        private DownloadListManager downloadListManager;
        private LibraryNavigator libraryNavigator;
        private LibraryTable table;
        
        public LibraryDownloadAction(String text, DownloadListManager downloadListManager, LibraryTable table, LibraryNavigator navigator, LibraryFileList libraryList){
            super(text);
            this.downloadListManager = downloadListManager;
            this.table = table;
            this.libraryFileList = libraryList;
            this.libraryNavigator = navigator;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            download(table.convertRowIndexToModel(table.getEditingRow()));
        }
        
        public void download(int row) {
            final RemoteFileItem file = (RemoteFileItem) ((LibraryTableModel) table.getModel()).getElementAt(row);
            if(libraryFileList.contains(file.getUrn())) {
                libraryNavigator.selectInLibrary(file.getUrn(), file.getCategory());            
            } else {
                try {
                    downloadListManager.addDownload(file);
                    TableCellEditor editor = table.getCellEditor();
                    if (editor != null) {          
                       editor.cancelCellEditing();
                    }
                } catch (SaveLocationException e) {
                    saveLocationExceptionHandler.handleSaveLocationException(new DownloadAction() {
                        @Override
                        public void download(File saveFile, boolean overwrite)
                                throws SaveLocationException {
                            downloadListManager.addDownload(file, saveFile, overwrite);
                        }
                    }, e, true);
                }
            }
        }
    }
    
    /**
     * Ensures the selected row is visible.
     */
    public void ensureSelectionVisible() {
        ensureRowVisible(getSelectedRow());
    }
    
    public boolean isCellEditable(int row, int column) {
        return super.isCellEditable(row, column) && !isRowDisabled(row);
    }
    
    
    public boolean isRowDisabled(int row) {
        FileItem item = getLibraryTableModel().getFileItem(convertRowIndexToModel(row));
        if (item instanceof LocalFileItem) {
            return ((LocalFileItem) item).isIncomplete();
        }
        return item == null;
    }
    
    private static class DisabledHighlightPredicate implements HighlightPredicate {
        private LibraryTable table;
        public DisabledHighlightPredicate (LibraryTable table) {
            this.table = table;
        }
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {           
            return table.isRowDisabled(adapter.row);
        }
    }
    
    @Override
    public void selectAndScrollTo(File file) {
        LibraryTableModel<T> model = getLibraryTableModel();
        for(int y=0; y < model.getRowCount(); y++) {
            FileItem fileItem = model.getElementAt(y);
            if(!(fileItem instanceof LocalFileItem)) {
                break; // Never going to find it.
            }
            if(file.equals(((LocalFileItem)fileItem).getFile())) {
                getSelectionModel().setSelectionInterval(y, y);
                break;
            }
        }
        ensureSelectionVisible();
    }
    
    @Override
    public void selectAndScrollTo(URN urn) {
        LibraryTableModel<T> model = getLibraryTableModel();
        for(int y=0; y < model.getRowCount(); y++) {
            FileItem fileItem = model.getElementAt(y);
            if(urn.equals(fileItem.getUrn())) {
                getSelectionModel().setSelectionInterval(y, y);
                break;
            }
        }
        ensureSelectionVisible();
    }
    
    @Override
    public File getNextItem(File file) {
        LibraryTableModel<T> model = getLibraryTableModel();
        for(int y=0; y < model.getRowCount()-1 ; y++) {
            FileItem fileItem = model.getElementAt(y);
            if(!(fileItem instanceof LocalFileItem)) {
                break; // Never going to find it.
            }
            if(file.equals(((LocalFileItem)fileItem).getFile())) {
                fileItem = model.getElementAt(y+1);
                if (fileItem instanceof LocalFileItem) {
                    return ((LocalFileItem) fileItem).getFile();
                }

                // No chance of success if we already found the next
                //  and it is not a local file
                return null;
            }
        }
        return null;
    }
    
    @Override
    public File getPreviousItem(File file) {
        LibraryTableModel<T> model = getLibraryTableModel();
        for(int y=0; y < model.getRowCount() ; y++) {
            FileItem fileItem = model.getElementAt(y);
            if(!(fileItem instanceof LocalFileItem)) {
                break; // Never going to find it.
            }
            if(file.equals(((LocalFileItem)fileItem).getFile())) {
                // There is nothing before the first element.
                if (y==0) {
                    return null;
                }
                
                fileItem = model.getElementAt(y-1);
                if (fileItem instanceof LocalFileItem) {
                    return ((LocalFileItem) fileItem).getFile();
                }
                
                // No chance of success if we already found the previous
                //  and it is not a local file
                return null;
            }
        }
        return null;
    }
    
    public void selectAll() {
        if (getRowCount() > 0) {
            getSelectionModel().setSelectionInterval(0, getRowCount() - 1);
        }
    }

}
