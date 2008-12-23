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
                HorizontalDownloadTableModel.this.downloadItems.getReadWriteLock().readLock().lock();
                try {
                    while(listChanges.nextBlock()) {
                        if(listChanges.getType() == ListEvent.INSERT || listChanges.getType() == ListEvent.DELETE){
                            fireTableModelEvent(new TableModelEvent(HorizontalDownloadTableModel.this,TableModelEvent.HEADER_ROW));
                        } else if(listChanges.getType() == ListEvent.UPDATE){
                            fireTableModelEvent(new TableModelEvent(HorizontalDownloadTableModel.this, TableModelEvent.UPDATE));
                          }
                    }
                } finally {
                    HorizontalDownloadTableModel.this.downloadItems.getReadWriteLock().readLock().unlock();
                }
            }
        });
     
    }
    
    public DownloadItem getDownloadItem(int index){
        return downloadItems.get(downloadItems.size() - 1 - index);
    }

    public int indexOf(DownloadItem item) {
        return downloadItems.size() - 1 - downloadItems.indexOf(item);
    }


    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex >= downloadItems.size()){
            throw new IllegalArgumentException("Unknown column:" + columnIndex);
        }
        return DownloadItem.class;
    }

    @Override
    public int getColumnCount() {
        return downloadItems.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex >= downloadItems.size()){
            throw new IllegalArgumentException("Unknown column:" + columnIndex);
        }
        return Integer.toString(columnIndex);
    }

    @Override
    public int getRowCount() {
        return 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex >= downloadItems.size()){
            throw new IllegalArgumentException("Unknown column:" + columnIndex);
        }
        if (rowIndex > 0){
            throw new IllegalArgumentException("Unknown row:" + rowIndex);
        }
        return getDownloadItem(columnIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex >= downloadItems.size()){
            throw new IllegalArgumentException("Unknown column:" + columnIndex);
        }
        if (rowIndex > 0){
            throw new IllegalArgumentException("Unknown row:" + rowIndex);
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
