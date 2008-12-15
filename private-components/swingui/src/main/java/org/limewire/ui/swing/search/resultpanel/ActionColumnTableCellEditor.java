package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class is both a table cell renderer and a table cell editor
 * for displaying the "Download", "View File Info" and "Mark as Junk" buttons.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ActionColumnTableCellEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
        
    private final DownloadHandler downloadHandler;
    private ActionButtonPanel panel;
    private VisualSearchResult vsr;
    
    public ActionColumnTableCellEditor(DownloadHandler downloadHandler) {
        this.downloadHandler = downloadHandler;
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }
    
    private ActionButtonPanel getPanel(final JTable table) {
        if (panel != null) return panel;
        
        panel = new ActionButtonPanel(downloadHandler, table);
        panel.setOpaque(false);
        
        JToggleButton junkButton = panel.getSpamButton();
        junkButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                boolean spam = event.getStateChange() == ItemEvent.SELECTED;
                vsr.setSpam(spam);
                table.editingStopped(new ChangeEvent(table));
            }
        });
        
        return panel;
    }

    @Override
    public Component getTableCellEditorComponent(
        JTable table, Object value, boolean isSelected,
        int row, int column) {
        
        ActionButtonPanel panel = getPanel(table);
        vsr = (VisualSearchResult) value;
        
        panel.prepareForDisplay(vsr);
        
        return panel;
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected,
        boolean hasFocus, int row, int column) {
        
        return getTableCellEditorComponent(
            table, value, isSelected, row, column);
    }
    
    @Override
    public boolean isCellEditable(EventObject event) {
        return true;
    }
}