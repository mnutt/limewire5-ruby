/**
 * 
 */
package org.limewire.ui.swing.table;

import java.awt.Component;
import java.awt.Point;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.rollover.RolloverProducer;

/**
 * A {@link TableCellRenderer} that only paints if the cell is
 * selected or rolled over.  This allows the parent's background
 * to be painted if the cell has no custom background.
 */
public class ClearCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        JComponent renderer = (JComponent) super.getTableCellRendererComponent(table,
                value, isSelected, hasFocus, row, column);
        Point rolloverPoint = (Point)table.getClientProperty(RolloverProducer.ROLLOVER_KEY);
        boolean rolledOver = rolloverPoint != null && row == rolloverPoint.y;
        renderer.setOpaque(isSelected || rolledOver);
        return renderer;
    }
}