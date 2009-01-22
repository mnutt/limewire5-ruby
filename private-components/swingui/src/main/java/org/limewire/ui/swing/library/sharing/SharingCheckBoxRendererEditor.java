package org.limewire.ui.swing.library.sharing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.GnutellaFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.settings.LibrarySettings;

public class SharingCheckBoxRendererEditor extends JCheckBox implements TableCellRenderer, TableCellEditor {

    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
    
    private LocalFileList localFileList;
    
    private LocalFileItem currentItem = null;
    
    public SharingCheckBoxRendererEditor(final LocalFileList localFileList, final JTable table) {
        this.localFileList = localFileList;
        addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if(currentItem != null) {
                    //if the box was checked, add it to the fileList
                    if(isSelected()) {
                        localFileList.addFile(currentItem.getFile());
                    } else { // remove it from the fileList
                        localFileList.removeFile(currentItem.getFile());
                    }
                    table.repaint();
                }
            }
        });
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
		if(value == null || !(value instanceof LocalFileItem)) {
		    setEnabled(false);
            setSelected(false);
            return this;
		}
			
        LocalFileItem fileItem = (LocalFileItem) value;
        updateCheckbox(fileItem);
        
        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        if(value == null || !(value instanceof LocalFileItem)) {
            setEnabled(false);
            setSelected(false);
            return this;
        }
        
        currentItem = (LocalFileItem) value;
        updateCheckbox(currentItem);
        return this;
    }
    
    private void updateCheckbox(LocalFileItem fileItem) {
        if(!fileItem.isShareable()) {
            setEnabled(false);
            setSelected(false);
        } else if(fileItem.getCategory() == Category.PROGRAM && !LibrarySettings.ALLOW_PROGRAMS.getValue()) {
            setEnabled(false);
            setSelected(false);
        } else if(fileItem.getCategory() == Category.DOCUMENT && !LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue() && GnutellaFileList.class.isInstance(localFileList)) {
            setEnabled(false);
            setSelected(false);
        } else {
            setSelected(localFileList.contains(fileItem.getFile()));
            setEnabled(true);    
        }
    }

    @Override
    public void addCellEditorListener(CellEditorListener lis) {
        synchronized (listeners) {
            if (!listeners.contains(lis))
                listeners.add(lis);
        }
    }

    @Override
    public void cancelCellEditing() {
        synchronized (listeners) {
            for (int i = 0, N = listeners.size(); i < N; i++) {
                listeners.get(i).editingCanceled(new ChangeEvent(this));
            }
        }
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    @Override
    public void removeCellEditorListener(CellEditorListener lis) {
        synchronized (listeners) {
            if (listeners.contains(lis))
                listeners.remove(lis);
        }
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return false;
    }

    @Override
    public boolean stopCellEditing() {
        synchronized (listeners) {
            for (int i = 0, N = listeners.size(); i < N; i++) {
                listeners.get(i).editingStopped(new ChangeEvent(this));
            }
        }
        return true;
    }
}
