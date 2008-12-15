package org.limewire.ui.swing.library.table;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.table.TableColumnExt;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.library.Disposable;
import org.limewire.ui.swing.library.LibraryOperable;
import org.limewire.ui.swing.library.Sharable;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableColumnSelector;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.swing.EventSelectionModel;

/**
 * Creates a table that is used when displaying library information. This may
 * be files in your own library or remote files in someone else's library. 
 */
public class LibraryTable<T extends FileItem> extends MouseableTable
    implements Sharable<File>, Disposable, LibraryOperable {
    
    private final LibraryTableFormat<T> format;
    private final TableColors tableColors;
    private final EventList<T> listSelection;
    private final SaveLocationExceptionHandler saveLocationExceptionHandler;
    private final ShareTableRendererEditorFactory shareTableRendererEditorFactory;
    
    private TableRendererEditor shareEditor;
    private ShareWidget<File> shareWidget;
    
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
        
        EventSelectionModel<T> model = new EventSelectionModel<T>(libraryItems);
        setSelectionModel(model);
        model.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        this.listSelection = model.getSelected();
        setHighlighters(tableColors.getEvenHighLighter(), tableColors.getOddHighLighter(), 
                new ColorHighlighter(new DisabledHighlightPredicate(this), null, tableColors.getDisabledForegroundColor(), null, tableColors.getDisabledForegroundColor()));
       
        setFillsViewportHeight(true);
        setDragEnabled(true);
        setRowHeight(rowHeight);
        
        final JTableHeader header = getTableHeader();
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e)){
                    showHeaderPopupMenu(e.getPoint());
                }
            }
        });       
        
        setupCellRenderers(format);
        setupColumnWidths(format);
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
    
    private void setupColumnWidths(LibraryTableFormat<T> format) {
        for(int i = 0; i < format.getColumnCount(); i++) {
            getColumn(i).setPreferredWidth(format.getInitialWidth(i));
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
        hideColumns();
        
        //this isn't necessary if we aren't showing "share" on mouse over
//        removeMouseMotionListener(mouseOverEditorListener);
//        addMouseMotionListener(new MouseMotionAdapter(){
//            @Override
//            public void mouseMoved(MouseEvent e) {
//                int editRow = rowAtPoint(e.getPoint());
//                editCellAt(editRow, convertColumnIndexToView(format.getActionColumn()));
//            }
//            
//        });
    }
    
    public void enableDownloading(DownloadListManager downloadListManager){
        LibraryDownloadAction downloadAction = new LibraryDownloadAction(I18n.tr("download"), downloadListManager, this);
        
        setDoubleClickHandler(new LibraryDownloadDoubleClickHandler(downloadAction));
        
        hideColumns();
    }
    
    public void enableSharing() {
        hideColumns();
    }
    
    public void dispose() {
        ((EventSelectionModel)getSelectionModel()).dispose();
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
    
    //only works if column indexes are in descending orde
    protected void hideColumns(){
        for(int i = format.getColumnCount()-1; i > 0; i--) {
            ((TableColumnExt) getColumnModel().getColumn(i)).setVisible(format.isColumnHiddenAtStartup(i));
        }
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
        private DownloadListManager downloadListManager;
        private LibraryTable table;
        
        public LibraryDownloadAction(String text, DownloadListManager downloadListManager, LibraryTable table){
            super(text);
            this.downloadListManager = downloadListManager;
            this.table = table;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            download(table.convertRowIndexToModel(table.getEditingRow()));
        }
        
        public void download(int row) {
            final RemoteFileItem file = (RemoteFileItem) ((LibraryTableModel) table.getModel()).getElementAt(row);
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
    
    /**
     * Ensures the selected row is visible.
     */
    public void ensureSelectionVisible() {
        ensureRowVisible(getSelectedRow());
    }
    
    public boolean isCellEditable(int row, int column) {
        return super.isCellEditable(row, column) && !isRowDisabled(row);
    }
    
    /**
     * Ensures the given row is visible.
     */
    public void ensureRowVisible(int row) {
        if(row != -1) {
            Rectangle cellRect = getCellRect(row, 0, false);
            Rectangle visibleRect = getVisibleRect();
            if( !visibleRect.intersects(cellRect) )
                scrollRectToVisible(cellRect);
        }
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
}
