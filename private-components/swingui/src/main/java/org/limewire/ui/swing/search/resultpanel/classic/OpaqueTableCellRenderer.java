package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.swing.EventTableModel;

/**
 * Creates a TableCellRenderer for classic search results. If the result
 * is spam or is marked as spam, the contents of the cell are grayed out.
 * 
 * Any new TableCellRenderer added to the classic search view should 
 * subclass this renderer.
 */
public class OpaqueTableCellRenderer implements TableCellRenderer {

    private static final int HGAP = 4;
    private static final int VGAP = 5;
    
    private final JXPanel panel;
    private final FlowLayout flowLayout;
    
    public OpaqueTableCellRenderer(int alignment) {
        panel = new JXPanel();
        flowLayout = new FlowLayout(alignment, HGAP, VGAP);
        panel.setLayout(flowLayout);
    }
    
    public void addComponent(JComponent component) {
        panel.add(component);
    }
    
    public void setLayoutAlignment(int alignment) {
        flowLayout.setAlignment(alignment);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        if(value == null) {
            value = "";
        } else {
            EventTableModel tableModel = (EventTableModel) table.getModel();
            VisualSearchResult vsr = (VisualSearchResult) tableModel.getElementAt(row);
            panel.setAlpha(vsr.isSpam() ? 0.2f : 1.0f);
        }
        return panel;
    }
}
