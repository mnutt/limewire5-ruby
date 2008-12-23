package org.limewire.ui.swing.library.sharing;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.components.IconButton;

public class ShareRendererEditor extends JPanel implements  TableCellEditor, TableCellRenderer {
    private JButton button;
    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
    
    private SharingTarget friend;
    
    public ShareRendererEditor(Icon icon, Icon rolloverIcon, Icon pressedIcon){
        super(new MigLayout("nocache, fill, ins 0 0 0 0 , gap 0! 0!, novisualpadding"));
        setOpaque(true);
        button = new IconButton(icon, rolloverIcon, pressedIcon);
        add(button, "aligny 50%");
    }
    
    public SharingTarget getFriend() {
        return friend;
    }

    public void addActionListener(ActionListener listener){
        button.addActionListener(listener);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        friend = (SharingTarget)value;
        return this;
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
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
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
    public boolean shouldSelectCell(EventObject anEvent) {
        return false;
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

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        return this;
    }
    
}