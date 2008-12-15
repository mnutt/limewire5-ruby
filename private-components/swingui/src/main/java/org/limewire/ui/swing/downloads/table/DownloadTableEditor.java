package org.limewire.ui.swing.downloads.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class DownloadTableEditor implements TableCellEditor {

    
    private DownloadTableCell cellComponent;
	    
    private DownloadActionHandler actionHandler;
    private DownloadEditorListener editorListener;
	
    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();

    private DownloadItem editItem;

	public DownloadTableEditor(DownloadTableCell cellComponent) {
	    this.cellComponent = cellComponent;
	}	

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSel, int row, int col) {
	    
	    editItem = (DownloadItem) value;
	    cellComponent.update(editItem);
        return cellComponent.getComponent();
	}
	
	public ActionListener getEditorListener() {
	    return this.editorListener;
	}
	
	

    /**
     * Binds editor to downloadItems so that the editor automatically updates
     * when downloadItems changes and popup menus work.  This is required for Editors
     */
	public void initialiseEditor(EventList<DownloadItem> downloadItems, DownloadActionHandler actionHandler) {
        this.editorListener = new DownloadEditorListener();
        this.cellComponent.setEditorListener(this.editorListener);
	    
	    this.actionHandler = actionHandler;
	    
        downloadItems.addListEventListener(new ListEventListener<DownloadItem>() {
            @Override
            public void listChanged(ListEvent<DownloadItem> listChanges) {
                // TODO: only update if relevant downloadItem was updated
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (cellComponent.getComponent().isVisible()) {
                            updateEditor();
                        }
                    }
                });
            }
        });
    }
	
	private void updateEditor(){
	    if (editItem != null) {
	        cellComponent.update(editItem);
        }
	}
	
	
	
    @Override
	public final void addCellEditorListener(CellEditorListener lis) {
		synchronized (listeners) {
			if (!listeners.contains(lis))
				listeners.add(lis);
		}
	}

	@Override
	public final void cancelCellEditing() {
		synchronized (listeners) {
			for (int i = 0, N = listeners.size(); i < N; i++) {
				listeners.get(i).editingCanceled(new ChangeEvent(this));
			}
		}
	}

	@Override
	public final Object getCellEditorValue() {
		return null;
	}

	@Override
	public boolean isCellEditable(EventObject e) {
		return true;
	}

	@Override
	public final void removeCellEditorListener(CellEditorListener lis) {
		synchronized (listeners) {
			if (listeners.contains(lis))
				listeners.remove(lis);
		}
	}

	@Override
	public final boolean shouldSelectCell(EventObject e) {
		return true;
	}

	@Override
	public final boolean stopCellEditing() {
		synchronized (listeners) {
			for (int i = 0, N = listeners.size(); i < N; i++) {
				listeners.get(i).editingStopped(new ChangeEvent(this));
			}
		}
		return true;
	}
	

    private class DownloadEditorListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            actionHandler.performAction(e.getActionCommand(), editItem);
            cancelCellEditing();
        }

    }
    
	
	
}
