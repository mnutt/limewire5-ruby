package org.limewire.ui.swing.library.manager;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.util.I18n;

class RemoveButtonEditor extends HyperlinkButton implements TableCellEditor {
    
    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
    
    public RemoveButtonEditor(final LibraryManagerTreeTable treeTable) {
        super(new AbstractAction(I18n.tr("remove")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                int idx = treeTable.getEditingRow();
                LibraryManagerItem item = (LibraryManagerItem)treeTable.getModel().getValueAt(idx, LibraryManagerModel.REMOVE_INDEX);
                if(item != null) {
                    ((LibraryManagerModel)treeTable.getTreeTableModel()).excludeChild(item);
                }
                treeTable.repaint();
            }
        });
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 4));
    }

    /**
     * Changes to foreground using this method will be ignored
     */
    @Override
    public void setForeground(Color c) {
    }
    
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        return this;
    }

    @Override
    public void addCellEditorListener(CellEditorListener lis) {
        if (!listeners.contains(lis))
            listeners.add(lis);
    }

    @Override
    public void cancelCellEditing() {
        for (int i = 0, N = listeners.size(); i < N; i++) {
            listeners.get(i).editingCanceled(new ChangeEvent(this));
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
        listeners.remove(lis);
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
