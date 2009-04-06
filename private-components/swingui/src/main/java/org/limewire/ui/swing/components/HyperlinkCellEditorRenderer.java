package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

/**
 * Table cell renderer and editor that displays a hyperlink and handles the
 * hyperlink action.  When installing HyperlinkCellEditorRenderer in a table,
 * it is customary to create separate instances for the renderer and editor -
 * this is because the renderer may receive paint requests while the editor is
 * active for another cell.
 */
public class HyperlinkCellEditorRenderer extends HyperlinkButton 
    implements TableCellEditor, TableCellRenderer {

    /** List of cell editor listeners. */
    private final List<CellEditorListener> listenerList = new ArrayList<CellEditorListener>();
    
    /**
     * Constructs a HyperlinkCellEditorRenderer with no text or action.
     */
    public HyperlinkCellEditorRenderer() {
        initialize();
    }

    /**
     * Constructs a HyperlinkCellEditorRenderer with the specified action.
     */
    public HyperlinkCellEditorRenderer(Action action) {
        super(action);
        initialize();
    }

    /**
     * Constructs a HyperlinkCellEditorRenderer with the specified text.
     */
    public HyperlinkCellEditorRenderer(String text) {
        super(text);
        initialize();
    }

    /**
     * Constructs a HyperlinkCellEditorRenderer with the specified text and 
     * action.
     */
    public HyperlinkCellEditorRenderer(String text, Action action) {
        super(text, action);
        initialize();
    }

    /**
     * Initializes the properties of this component.
     */
    private void initialize() {
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 4));
    }
    
    /**
     * Overrides superclass method to ignore foreground color changes.
     */
    @Override
    public void setForeground(Color c) {
    }

    /**
     * Returns this component as the cell renderer component.
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
            boolean isSelected, boolean hasFocus, int row, int column) {
        return this;
    }

    /**
     * Returns this component as the cell editor component.
     */
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, 
            boolean isSelected, int row, int column) {
        return this;
    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {
        if (!listenerList.contains(l)) {
            listenerList.add(l);
        }
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
        listenerList.remove(l);
    }

    @Override
    public void cancelCellEditing() {
        ChangeEvent event = new ChangeEvent(this);
        for (int i = 0, size = listenerList.size(); i < size; i++) {
            listenerList.get(i).editingCanceled(event);
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
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        cancelCellEditing();
        return true;
    }

}
