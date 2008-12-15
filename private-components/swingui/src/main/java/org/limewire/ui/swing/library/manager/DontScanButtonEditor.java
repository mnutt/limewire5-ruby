package org.limewire.ui.swing.library.manager;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

public class DontScanButtonEditor extends JCheckBox implements TableCellEditor {

    private @Resource Icon icon;
    private @Resource Icon selectedIcon;
    
    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
    
    public DontScanButtonEditor(final LibraryManagerTreeTable treeTable) {
        
        GuiUtils.assignResources(this);
        
        this.setIcon(icon);
        this.setSelectedIcon(selectedIcon);
                
        setHorizontalAlignment(SwingConstants.CENTER);
        
        addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = treeTable.getSelectedRow();
                LibraryManagerItem item = (LibraryManagerItem) treeTable.getModel().getValueAt(index, LibraryManagerModel.SCAN_INDEX);
                item.setScanned(!item.isScanned());
                
                treeTable.repaint();
            }
        });
    }
    
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        setBackground(table.getSelectionBackground());
        LibraryManagerItem item = (LibraryManagerItem) value;
        setSelected(!item.isScanned());
        
        return this;
    }

    @Override
    public void addCellEditorListener(CellEditorListener lis) {
        synchronized (listeners) {
            if (!listeners.contains(lis)) listeners.add(lis);
        }
    }

    @Override
    public void cancelCellEditing() {
        synchronized (listeners) {
            for (int i=0, N=listeners.size(); i<N; i++) {
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
            if (listeners.contains(lis)) listeners.remove(lis);
        }
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        cancelCellEditing();
        return true;
    }

}
