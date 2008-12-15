package org.limewire.ui.swing.table;

import java.awt.Component;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.limewire.ui.swing.components.UnshareButton;

import net.miginfocom.swing.MigLayout;

/**
 * Generic Renderer/Editor that allows for one or more buttons to 
 * be drawn in a table cell. To create clickable buttons, a list of
 * actions is passed into the renderer/editor.
 * <p>
 * This class can be used for both renderering and editing but there must
 * be a unique instance for the renderer and editor. 
 * <p>
 * For the editor to properlly cancel, an instance of the editor must be passed
 * to each button and upon actionPerfomed, the button must call cancelCellEditing.
 * 
 *      public void actionPerformed(ActionEvent e) {
 *          editor.cancelCellEditing();
 *      }
 */
public class MultiButtonTableCellRendererEditor extends JPanel implements TableCellRenderer, TableCellEditor {
     
    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();

    public MultiButtonTableCellRendererEditor() {
        this(new ArrayList<Action>());
    }
    
    public MultiButtonTableCellRendererEditor(List<Action> actions) {
        setLayout(new MigLayout("aligny 50%, alignx left, fillx, gapx 0, insets 0 0 0 0"));
     
        addActions(actions);
    }
    
    public void addActions(List<Action> actions) {
        for(Action action : actions) {
            UnshareButton button = new UnshareButton(action);
            button.setFocusable(false);
                    
            add(button);
        }
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        if(table.getSelectedRow() == row) {
            this.setBackground(table.getSelectionBackground());
            this.setForeground(table.getSelectionForeground());
        }
        else {
            this.setBackground(table.getBackground());
            this.setForeground(table.getForeground());
        }
        

        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        this.setBackground(table.getSelectionBackground());
        this.setForeground(table.getSelectionForeground());
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
