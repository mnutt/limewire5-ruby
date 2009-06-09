package org.limewire.ui.swing.downloads.table;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.components.RemoteHostWidgetFactory;
import org.limewire.ui.swing.components.RemoteHostWidget.RemoteWidgetType;
import org.limewire.ui.swing.downloads.DownloadItemUtils;
import org.limewire.ui.swing.downloads.table.renderer.DownloadButtonRendererEditor;
import org.limewire.ui.swing.downloads.table.renderer.DownloadMessageRenderer;
import org.limewire.ui.swing.downloads.table.renderer.DownloadProgressRenderer;
import org.limewire.ui.swing.downloads.table.renderer.DownloadTitleRenderer;
import org.limewire.ui.swing.search.resultpanel.classic.FromTableCellRenderer;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableColors;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.GlazedListsSwingFactory;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.swing.EventSelectionModel;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Table showing DownloadItems. Provides popup menus and double click handling.
 * No renderers or editors are set by default.
 */
public class DownloadTable extends MouseableTable {   
    
    @Resource private int rowHeight;    
    
    private DownloadTableModel model;

    private EventList<DownloadItem> selectedItems;

    @Inject
	public DownloadTable(DownloadTitleRenderer downloadTitleRenderer, DownloadProgressRenderer downloadProgressRenderer, 
	        DownloadMessageRenderer downloadMessageRenderer, DownloadButtonRendererEditor buttonRenderer,
	        DownloadButtonRendererEditor buttonEditor, DownloadActionHandler actionHandler, 
	        @Assisted EventList<DownloadItem> downloadItems, RemoteHostWidgetFactory remoteHostWidgetFactory) {
        
        GuiUtils.assignResources(this);
                
        initialize(downloadItems, actionHandler, buttonEditor);
        
        TableColors colors = new TableColors();
        setHighlighters(
                new ColorHighlighter(HighlightPredicate.EVEN, colors.evenColor,
                        colors.evenForeground, colors.selectionColor,
                        colors.selectionForeground),
                new ColorHighlighter(HighlightPredicate.ODD, colors.evenColor,
                        colors.evenForeground, colors.selectionColor,
                        colors.selectionForeground));
        
        setShowGrid(true, false);        

        getColumnModel().getColumn(DownloadTableFormat.TITLE).setCellRenderer(downloadTitleRenderer);
        getColumnModel().getColumn(DownloadTableFormat.PROGRESS).setCellRenderer(downloadProgressRenderer);
        getColumnModel().getColumn(DownloadTableFormat.NUM_SOURCES).setCellRenderer(new FromTableCellRenderer(remoteHostWidgetFactory.create(RemoteWidgetType.TABLE)));
        getColumnModel().getColumn(DownloadTableFormat.NUM_SOURCES).setCellEditor(new FromTableCellRenderer(remoteHostWidgetFactory.create(RemoteWidgetType.TABLE)));
        getColumnModel().getColumn(DownloadTableFormat.MESSAGE).setCellRenderer(downloadMessageRenderer);
        getColumnModel().getColumn(DownloadTableFormat.ACTION).setCellRenderer(buttonRenderer);
        
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

    private void initialize(EventList<DownloadItem> downloadItems, DownloadActionHandler actionHandler,
            DownloadButtonRendererEditor buttonEditor) {
        model = new DownloadTableModel(downloadItems);
        setModel(model);
        
        EventSelectionModel<DownloadItem> model = GlazedListsSwingFactory.eventSelectionModel(downloadItems);
        setSelectionModel(model);
        model.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
        this.selectedItems = model.getSelected();

        TablePopupHandler popupHandler = new DownloadPopupHandler(actionHandler, this);

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

        buttonEditor.setActionHandler(actionHandler);
        getColumnModel().getColumn(DownloadTableFormat.ACTION).setCellEditor(buttonEditor);

    }
    
    private void launch(int row){
        DownloadItem item = getDownloadItem(row);
        if(item != null && item.isLaunchable()) {
            DownloadItemUtils.launch(item);
        }
    }
}