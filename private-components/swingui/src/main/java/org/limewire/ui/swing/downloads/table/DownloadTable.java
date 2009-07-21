package org.limewire.ui.swing.downloads.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.downloads.DownloadItemUtils;
import org.limewire.ui.swing.downloads.table.renderer.DownloadButtonRendererEditor;
import org.limewire.ui.swing.downloads.table.renderer.DownloadCancelRendererEditor;
import org.limewire.ui.swing.downloads.table.renderer.DownloadMessageRenderer;
import org.limewire.ui.swing.downloads.table.renderer.DownloadProgressRenderer;
import org.limewire.ui.swing.downloads.table.renderer.DownloadTitleRenderer;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableColors;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Table showing DownloadItems. Provides popup menus and double click handling.
 * No renderers or editors are set by default.
 */
public class DownloadTable extends MouseableTable {   

    @Resource private int rowHeight;  
    @Resource private int gapMinWidth;  
    @Resource private int gapPrefWidth; 
    @Resource private int gapMaxWidth; 
    @Resource private int titleMinWidth;  
    @Resource private int titlePrefWidth; 
    @Resource private int titleMaxWidth;  
    @Resource private int progressMinWidth;
    @Resource private int progressPrefWidth; 
    @Resource private int progressMaxWidth;  
    @Resource private int messageMinWidth; 
    @Resource private int messagePrefWidth; 
    @Resource private int messageMaxWidth;  
    @Resource private int actionMinWidth; 
    @Resource private int actionPrefWidth;  
    @Resource private int actionMaxWidth; 
    @Resource private int cancelMinWidth; 
    @Resource private int cancelPrefWidth;
    @Resource private int cancelMaxWidth;
    
    private DownloadTableModel model;

    private EventList<DownloadItem> selectedItems;

    @Inject
	public DownloadTable(DownloadTitleRenderer downloadTitleRenderer, DownloadProgressRenderer downloadProgressRenderer, 
	        DownloadMessageRenderer downloadMessageRenderer, DownloadCancelRendererEditor cancelEditor,
	        DownloadButtonRendererEditor buttonEditor, DownloadActionHandler actionHandler, DownloadPopupHandlerFactory downloadPopupHandlerFactory,
	        @Assisted EventList<DownloadItem> downloadItems, DownloadableTransferHandler downloadableTransferHandler) {
        
        GuiUtils.assignResources(this);
                
        initialize(downloadItems, buttonEditor, cancelEditor, downloadPopupHandlerFactory);
        
        TableColors colors = new TableColors();
        setHighlighters(
                new ColorHighlighter(HighlightPredicate.EVEN, colors.evenColor,
                        colors.evenForeground, colors.selectionColor,
                        colors.selectionForeground),
                new ColorHighlighter(HighlightPredicate.ODD, colors.evenColor,
                        colors.evenForeground, colors.selectionColor,
                        colors.selectionForeground));
        
        setShowGrid(true, false);      
        setEmptyRowsPainted(true);
        
        TableCellRenderer gapRenderer = new GapRenderer();

        setUpColumn(DownloadTableFormat.TITLE, downloadTitleRenderer, titleMinWidth, titlePrefWidth, titleMaxWidth);
        setUpColumn(DownloadTableFormat.TITLE_GAP, gapRenderer, gapMinWidth, gapPrefWidth, gapMaxWidth);
        setUpColumn(DownloadTableFormat.PROGRESS, downloadProgressRenderer, progressMinWidth, progressPrefWidth, progressMaxWidth);
        setUpColumn(DownloadTableFormat.PROGRESS_GAP, gapRenderer, gapMinWidth, gapPrefWidth, gapMaxWidth);
        setUpColumn(DownloadTableFormat.MESSAGE, downloadMessageRenderer, messageMinWidth, messagePrefWidth, messageMaxWidth);
        setUpColumn(DownloadTableFormat.MESSAGE_GAP, gapRenderer, gapMinWidth, gapPrefWidth, gapMaxWidth);
        setUpColumn(DownloadTableFormat.ACTION, new DownloadButtonRendererEditor(), actionMinWidth, actionPrefWidth, actionMaxWidth);
        setUpColumn(DownloadTableFormat.ACTION_GAP, gapRenderer, gapMinWidth, gapPrefWidth, gapMaxWidth);
        setUpColumn(DownloadTableFormat.CANCEL, new DownloadCancelRendererEditor(), cancelMinWidth, cancelPrefWidth, cancelMaxWidth);
        
        setTransferHandler(downloadableTransferHandler);
        setDragEnabled(true);
        setRowHeight(rowHeight);        
    }
	
	public DownloadItem getDownloadItem(int row){
	    return model.getDownloadItem(convertRowIndexToModel(row));
	}
	
    /** Returns a copy of all selected items. */
    public List<DownloadItem> getSelectedItems() {
        return new ArrayList<DownloadItem>(selectedItems);
    }
       
    public void selectAndScrollTo(URN urn) {
        if(urn != null) {
            for(int y=0; y < model.getRowCount(); y++) {
                DownloadItem item = getDownloadItem(y);
                if(item != null && item.getUrn() != null && urn.equals(item.getUrn())) {
                    getSelectionModel().setSelectionInterval(y, y);
                    ensureRowVisible(y);
                    break;
                }
            }
        }        
    }
    
    public void selectAndScrollTo(DownloadItem item) {
        for(int y = 0; y < model.getRowCount(); y++) {
            if(item == getDownloadItem(y)) {
                getSelectionModel().setSelectionInterval(y, y);
                ensureRowVisible(y);
                break;
            }
        }
    }
    
    private void setUpColumn(int index, TableCellRenderer renderer, int minWidth, int prefWidth, int maxWidth){
        TableColumn column = getColumnModel().getColumn(index);
        column.setCellRenderer(renderer);
        column.setMinWidth(minWidth);
        column.setPreferredWidth(prefWidth);
        column.setMaxWidth(maxWidth);
    }

    private void initialize(EventList<DownloadItem> downloadItems, DownloadButtonRendererEditor buttonEditor, 
            DownloadCancelRendererEditor cancelEditor, DownloadPopupHandlerFactory downloadPopupHandlerFactory) {
        model = new DownloadTableModel(downloadItems);
        setModel(model);
        
        DefaultEventSelectionModel<DownloadItem> model = new DefaultEventSelectionModel<DownloadItem>(downloadItems);
        setSelectionModel(model);
        model.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        this.selectedItems = model.getSelected();

        TablePopupHandler popupHandler = downloadPopupHandlerFactory.create(this);

        setPopupHandler(popupHandler);

        TableDoubleClickHandler clickHandler = new TableDoubleClickHandler() {
            @Override
            public void handleDoubleClick(int row) {
               launch(row);
            }
        };

        setDoubleClickHandler(clickHandler);        
        
        Action enterAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int row = getSelectedRow();
                if (row > -1) {
                    launch(row);
                }
            }
        };        

        setEnterKeyAction(enterAction);
        
        getColumnModel().getColumn(DownloadTableFormat.ACTION).setCellEditor(buttonEditor);
        getColumnModel().getColumn(DownloadTableFormat.CANCEL).setCellEditor(cancelEditor);

    }
    
    private void launch(int row){
        DownloadItem item = getDownloadItem(row);
        if(item != null && item.isLaunchable()) {
            DownloadItemUtils.launch(item);
        }
    }
    
    private static class GapRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return super.getTableCellRendererComponent(table, null, isSelected, false, row, column);
        }
        
    }
}