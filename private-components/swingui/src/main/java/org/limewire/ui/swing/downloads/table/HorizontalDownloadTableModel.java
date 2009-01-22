package org.limewire.ui.swing.downloads.table;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.limewire.core.api.download.DownloadItem;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class HorizontalDownloadTableModel implements TableModel {
    private List<TableModelListener> listeners;
    private EventList<DownloadItem> downloadItems;
    
    
    public HorizontalDownloadTableModel(EventList<DownloadItem> downloadItems) {
        this.downloadItems = downloadItems;
        listeners = new ArrayList<TableModelListener>();
        downloadItems.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                while(listChanges.nextBlock()) {
                    if(listChanges.getType() == ListEvent.INSERT || listChanges.getType() == ListEvent.DELETE){
                        fireTableModelEvent(new TableModelEvent(HorizontalDownloadTableModel.this,TableModelEvent.HEADER_ROW));
                    } else if(listChanges.getType() == ListEvent.UPDATE){
                        fireTableModelEvent(new TableModelEvent(HorizontalDownloadTableModel.this, TableModelEvent.UPDATE));
                    }
                }
            }
        });
     
    }
    
    public DownloadItem getDownloadItem(int index){
        //in accessible tables, can be 0 even if no items exist        
        if(index >= downloadItems.size()) {
            return null;
        } else {
            return downloadItems.get(downloadItems.size() - 1 - index);
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        // in accessible tables, columnIndex can be larger than column count
        return DownloadItem.class;
    }

    @Override
    public int getColumnCount() {
        return downloadItems.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        // in accessible tables, columnIndex can be larger than column count
        return Integer.toString(columnIndex);
    }

    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        // in accessible tables, rowIndex and columnIndex can be larger than row and column counts
        if (columnIndex >= downloadItems.size() || rowIndex > 0){
            return null;
        }
        return getDownloadItem(columnIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // in accessible tables, rowIndex and columnIndex can be larger than row and column counts
        if (columnIndex >= downloadItems.size() || rowIndex > 0){
            return false;
        }
        return true;
    }

  

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        //do nothing
    }


    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
        
    }

   protected void fireTableModelEvent(TableModelEvent e) {
        for (TableModelListener l : listeners) {
            l.tableChanged(e);
        }
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

}
