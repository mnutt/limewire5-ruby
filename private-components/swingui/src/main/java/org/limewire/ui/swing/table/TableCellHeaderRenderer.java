package org.limewire.ui.swing.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.FilterPipeline;
import org.jdesktop.swingx.decorator.SortController;
import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Paints a custom TableHeader for all Mouseable Tables.
 */
public class TableCellHeaderRenderer extends JXLabel implements TableCellRenderer {
    @Resource
    private Color topBorderColor;
    @Resource
    private Color bottomBorderColor;
    @Resource
    private Color topGradientColor;
    @Resource
    private Color bottomGradientColor;
    @Resource
    private Color leftBorderColor;
    @Resource
    private Color rightBorderColor;
    @Resource
    private Color fontColor;
    @Resource
    private Icon downIcon;
    @Resource
    private Icon upIcon;
    
    private final Border emptyBorder;

    private final Font font;
    
    private Icon sortIcon;
    
    public TableCellHeaderRenderer() {
        GuiUtils.assignResources(this);
        
        emptyBorder = BorderFactory.createEmptyBorder(0, 10, 0, 0);
        font = getFont().deriveFont(Font.BOLD, 11);

        setHorizontalTextPosition(JLabel.LEFT);
        setForeground(fontColor);
        setBackgroundPainter(new HeaderBackgroundPainter());
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        JXTable t = (JXTable) table;

        setText((String) value);
        setIcon(sortIcon);
        
        setPreferredSize(new Dimension(20, getPreferredSize().width));
        setBorder(emptyBorder);
        setFont(font);
        
        if(column >= 0) {
            // show the appropriate arrow if this column is sorted
            SortOrder order = getSortOrder(t, t.convertColumnIndexToModel(column));
            if(order == SortOrder.UNSORTED) { 
                setIcon(null);
            } else if(order == SortOrder.ASCENDING) {
                setIcon(upIcon);
            } else {
                setIcon(downIcon);
            }
        }
        
        return this;
    }
    
    /**
     * Returns the sort order associated with the specified JXTable and model
     * column index.  The sort order is meaningful only if the column is the 
     * first sort key column; otherwise, SortOrder.UNSORTED is returned.
     */
    private SortOrder getSortOrder(JXTable table, int modelColumn) {
        FilterPipeline filters = table.getFilters();
        if (filters == null) {
            return SortOrder.UNSORTED;
        }
        
        SortController sortController = filters.getSortController();
        if (sortController == null) {
            return SortOrder.UNSORTED;
        }
        
        List<? extends SortKey> sortKeys = sortController.getSortKeys();
        if (sortKeys == null) {
            return SortOrder.UNSORTED;
        }
        
        SortKey firstKey = SortKey.getFirstSortingKey(sortKeys);
        if ((firstKey != null) && (firstKey.getColumn() == modelColumn)) {
            return firstKey.getSortOrder();
        } else {
            return SortOrder.UNSORTED;
        }
    }
    
    // The following methods override the defaults for performance reasons
    public void validate() {}
    public void revalidate() {}
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}

    
    /**
     * Painter for the background of the header
     */
    private class HeaderBackgroundPainter extends AbstractPainter<JXLabel> {

        private RectanglePainter<JXLabel> painter;
        
        public HeaderBackgroundPainter() {
            painter = new RectanglePainter<JXLabel>();
            painter.setFillPaint(new GradientPaint(0,0, topGradientColor, 0, 1, bottomGradientColor, false));
            painter.setFillVertical(true);
            painter.setFillHorizontal(true);
            painter.setPaintStretched(true);
            painter.setBorderPaint(null);
        }
        
        @Override
        protected void doPaint(Graphics2D g, JXLabel object, int width, int height) {
            painter.paint(g, object, width, height);
            
            // paint the top border
            g.setColor(topBorderColor);
            g.drawLine(0, 0, width, 0);

            //paint the bottom border
            g.setColor(bottomBorderColor);
            g.drawLine(0, height-1, width, height-1);
            
            //paint the left border
            g.setColor(leftBorderColor);
            g.drawLine(0, 0, 0, height-2);

            //paint the bottom border
            g.setColor(rightBorderColor);
            g.drawLine(width-1, 0, width-1, height);
        }
    }
}
