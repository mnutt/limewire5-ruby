package com.limegroup.gnutella.gui.tables;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.Position;

import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.JMultilineToolTip;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.themes.ThemeSettings;
import com.limegroup.gnutella.util.DataUtils;

/**
 * A specialized JTable for use with special Lime functions.
 * 1) Allows the user to easily
 *    set a column as visible or invisible, rather than
 *    having to remove/add columns.
 *    It internally will remember where the column was and
 *    add/remove it as needed.
 * 2) It remembers which column is sorted and whether the sort
 *    is ascending or descending.
 *    For use with adding arrows to the tableHeader.
 * 3) Shows special tooltips for each row.
 * @author Sam Berlin
 */
public class LimeJTable extends JTable implements JSortTable {

    private static final int DEFAULT_ROW_HEIGHT = 16;
    
    /**
     * The columns that are currently hidden.
     */
    protected Map<Object, LimeTableColumn> _hiddenColumns =
        new HashMap<Object, LimeTableColumn>();
    
    /**
     * The index of the column that is currently pressed down.
     */
    protected int pressedColumnIndex = -1;
    
    /**
     * The array of tooltip data to display next.
     */
    private String[] tips;
    
    /**
     * The array to use when the tip is for extending a clipped name.
     */
    private final String[] CLIPPED_TIP = new String[1];
    
    /**
     * The last LimeTableColumn that was removed from this table.
     */
    private static LimeTableColumn _lastRemoved;
    
    /**
     * The preferences handler for the table columns.
     */
    protected ColumnPreferenceHandler columnPreferences;
    
    /**
     * The settings for this table.
     */
    protected TableSettings tableSettings;
    
    /**
     * Same as JTable().
     * The table MUST have setModel called with a DataLineModel in order
     * for this class to function properly.
     */
    public LimeJTable() {
        super();
        setToolTipText("");
        GUIUtils.fixInputMap(this);
        addFocusListener(FocusHandler.INSTANCE);
    }

    /**
     * Same as JTable(TableModel)
     */
    public LimeJTable(DataLineModel dm) {
        super(dm);
        setToolTipText("");
        GUIUtils.fixInputMap(this);
        addFocusListener(FocusHandler.INSTANCE);
    }
    
    /**
     * Overriden to not manage focus.
     * (Other it causes some problems in search results)
     */
    @Override
    @SuppressWarnings("deprecation")
    public boolean isManagingFocus() {
        return false;
    }
    
    /**
     * Sets the given row to be the only one selected.
     */
    public void setSelectedRow(int row) {
        clearSelection();
        addRowSelectionInterval(row, row);
    }
    
    /**
     * Override getSelectedRow to ensure that it exists in the table.
     * This is necessary because of bug 4730055.
     * See: http://developer.java.sun.com/developer/bugParade/bugs/4730055.html
     */
    @Override
    public int getSelectedRow() {
        int selected = super.getSelectedRow();
        if( selected >= dataModel.getRowCount() )
            return -1;
        else
            return selected;
    }
    
    /**
     * Overrided getSelectedRows to ensure that all selected rows exist in the
     * table. This is necessary because of bug 4730055.
     * See: http://developer.java.sun.com/developer/bugParade/bugs/4730055.html
     *
     * As a side effect, this implementation will return the rows in a sorted
     * order. (Lowest first)
     */
    @Override
    public int[] getSelectedRows() {
        int[] selected = super.getSelectedRows();
        if( selected == null || selected.length == 0)
            return selected;
        Arrays.sort(selected);
        int tableSize = dataModel.getRowCount();
        for(int i = 0; i < selected.length; i++) {
            // Short-circuit when we find an invalid value.
            if( selected[i] >= tableSize ) {
                int[] newData = new int[i];
                System.arraycopy(selected, 0, newData, 0, i);
                return newData;
            }
        }
        //Nothing was outside of the selection range.
        return selected;
    }
    
    /**
     * Ensures the selected row is visible.
     */
    public void ensureSelectionVisible() {
        ensureRowVisible(getSelectedRow());
    }
    
    /**
     * Ensures the given row is visible.
     */
    public void ensureRowVisible(int row) {
        if(row != -1) {
            Rectangle cellRect = getCellRect(row, 0, false);
            Rectangle visibleRect = getVisibleRect();
            if( !visibleRect.intersects(cellRect) )
                scrollRectToVisible(cellRect);
        }
    }
    
    /**
     * Determines if the selected row is visible.
     */
    public boolean isSelectionVisible() {
        return isRowVisible(getSelectedRow());
    }
    
    /**
     * Determines if the given row is visible.
     */
    public boolean isRowVisible(int row) {
        if(row != -1) {
            Rectangle cellRect = getCellRect(row, 0, false);
            Rectangle visibleRect = getVisibleRect();
            return visibleRect.intersects(cellRect);
        } else
            return false;
    }            

    /**
     * Access the ColumnPreferenceHandler.
     */
    public ColumnPreferenceHandler getColumnPreferenceHandler() {
        return columnPreferences;
    }

    /**
     * Set the ColumnPreferenceHandler
     */
    public void setColumnPreferenceHandler(ColumnPreferenceHandler handl) {
        columnPreferences = handl;
    }
    
    /**
     * Access the TableSettings.
     */
    public TableSettings getTableSettings() {
        return tableSettings;
    }
    
    /**
     * Set the TableSettings.
     */
    public void setTableSettings(TableSettings settings) {
        tableSettings = settings;
    }

    /**
     * set the pressed header column.
     * @param col The MODEL index of the column
     */
    public void setPressedColumnIndex(int col) {
        pressedColumnIndex = col;
    }

    /**
     * get the pressed header column
     * @return the VIEW index of the pressed column.
     */
    public int getPressedColumnIndex() {
        return convertColumnIndexToView(pressedColumnIndex);
    }

    /**
     * @return the VIEW index of the sorted column.
     */
    public int getSortedColumnIndex() {
        return convertColumnIndexToView(
                ((DataLineModel)dataModel).getSortColumn()
               );
    }

    /**
     * accessor function
     */
    public boolean isSortedColumnAscending() { 
        return ((DataLineModel)dataModel).isSortAscending();
    }

    /**
     * Simple function that tucks away hidden columns for use later.
     * And it uses them later!
     */
    public void setColumnVisible(Object columnId, boolean visible)
        throws LastColumnException {
        if ( !visible ) {
            TableColumnModel model = getColumnModel();
            // don't allow the last column to be removed.
            if ( model.getColumnCount() == 1 ) throw new LastColumnException();
            LimeTableColumn column = (LimeTableColumn)model.getColumn( model.getColumnIndex(columnId) );
            _hiddenColumns.put( columnId, column );
            _lastRemoved = column;
            removeColumn(column);
        } else {
            TableColumn column = _hiddenColumns.get(columnId);
            _hiddenColumns.remove( columnId );
            addColumn(column);
        }
    }

    /**
     * Returns an iterator of the removed columns.
     */
    public Iterator<LimeTableColumn> getHiddenColumns() {
        return Collections.unmodifiableCollection(_hiddenColumns.values()).iterator();
    }

    /**
     * Returns the last removed column.
     */
    public LimeTableColumn getLastRemovedColumn() {
        return _lastRemoved;
    }

    /**
     * Determines whether or not a column is visible in this table.
     */
    public boolean isColumnVisible(Object columnId) {
        return !_hiddenColumns.containsKey(columnId);
    }
    
    /**
     * Determines if the given point is a selected row.
     */
    public boolean isPointSelected(Point p) {
        int row = rowAtPoint(p);
        int col = columnAtPoint(p);
        if(row == -1 || col == -1)
            return false;
        int sel[] = getSelectedRows();
        for(int i = 0; i < sel.length; i++)
            if(sel[i] == row)
                return true;
        return false;
    }
    
    /**
     * Processes the given mouse event.
     */
    @Override
    public void processMouseEvent(MouseEvent e) {
        try {
            int reselectIndex = -1;
            if (OSUtils.isAnyMac() && e.isControlDown()) {
                TableModel model = getModel();
                if (model != null) {
                    int index = rowAtPoint(e.getPoint());
                    if (isRowSelected(index)) {
                        reselectIndex = index;
                    }
                }
            }
            
            super.processMouseEvent(e);
            
            if (reselectIndex != -1) {
                getSelectionModel().addSelectionInterval(reselectIndex, reselectIndex);
            }
            
            // deselect rows if 
            if (e.getID() == MouseEvent.MOUSE_CLICKED 
                    && SwingUtilities.isLeftMouseButton(e)
                    && !e.isPopupTrigger()) {
                
                TableModel model = getModel();
                if (model != null) {
                    int index = rowAtPoint(e.getPoint());
                    if (index < 0 || index >= model.getRowCount()) {
                        clearSelection();
                    }
                }
            }
        } catch(ArrayIndexOutOfBoundsException aioobe) {
            // A bug in Java 1.3 causes an AIOOBE from PopupMenus.
            // Normally we would ignore this, since it has nothing
            // to do with LimeWire -- but because we insert ourselves
            // into the call-chain here, we must manually ignore the error.
            String msg = aioobe.getMessage();
            if(msg != null &&
               msg.indexOf("at javax.swing.MenuSelectionManager.processMouseEvent") != -1)
                return; // ignore;
            
            throw aioobe;
        }
    }

    /**
     * Sets the internal tooltip text for use with the next
     * createToolTip.
     */
    @Override
    public String getToolTipText(MouseEvent e) {
        Point p = e.getPoint();
        int row = rowAtPoint(p);
        int col = columnAtPoint(p);
        int colModel = convertColumnIndexToModel(col);
        DataLineModel dlm = (DataLineModel)dataModel;
        boolean isClippable = col > -1 && row > -1 ?
                          dlm.isClippable(colModel) : false;
        boolean forceTooltip = col > -1 && row > -1 ?
                          dlm.isTooltipRequired(row, col) : false;
        
        // If the user doesn't want tooltips, only display
        // them if the column is too small (and data is clipped)
        if (!tableSettings.DISPLAY_TOOLTIPS.getValue() && !forceTooltip) {
            if (isClippable)
                return clippedToolTip(row, col, colModel);
            else
                return null;
        }
        
        if ( row > -1 ) {
            //set the internal tips for later use with createToolTip
            tips = dlm.getToolTipArray(row, colModel);
            // NOTE: the below return triggers the tooltip manager to
            // create a tooltip.
            // If it is null, one won't be created.
            // If two different rows return the same tip, the manager
            // won't be triggered to create a 'new' tip.
            // Rather than return the actual row#, which could stay the same
            // if sorting is enabled & the DataLine moves,
            // return the string representation
            // of the dataline, so if the row moves out from under the mouse,
            // the tooltip will auto change when the mouse
            // moves around the new DataLine (same row)
            if (tips == null) {
                // if we're over a column, see if we can display a clipped tool tip.
                if (isClippable)
                    return clippedToolTip(row, col, colModel);
                else
                    return null;
            } else
                return dlm.get(row).toString() + col;
        }
        tips = DataUtils.EMPTY_STRING_ARRAY;
        return null;
    }
    
    /**
     * Displays a tooltip for clipped data, if possible.
     *
     * @param row the row of the data
     * @param col the VIEW index of the column
     * @param colModel the MODEL index of the column
     */
    private String clippedToolTip(int row, int col, int colModel) {
        TableColumn tc = getColumnModel().getColumn(col);
        int columnWidth = tc.getWidth();
        int dataWidth = getDataWidth(row, colModel);
        if (columnWidth < dataWidth) {
            tips = CLIPPED_TIP;
            return ((DataLineModel)dataModel).get(row).toString() + col;
        } else {
            tips = DataUtils.EMPTY_STRING_ARRAY;
            return null;
        }
    }
    
    /**
     * Gets the width of the data in the specified row/column.
     *
     * @param row the row of the data
     * @param col the MODEL index of the column
     */
    private int getDataWidth(int row, int col) {
        DataLineModel dlm = (DataLineModel)dataModel;
        DataLine dl = dlm.get(row);
        Object data = dl.getValueAt(col);
        String info;
        if( data != null && (info = data.toString()) != null ) {
            if (data instanceof Icon && info.startsWith("file:"))
                return -1;
            
            CLIPPED_TIP[0] = info;
            TableCellRenderer tcr = getDefaultRenderer(dlm.getColumnClass(col));
            JComponent renderer = (JComponent)tcr.getTableCellRendererComponent(
                                        this, data, false, false, row, col);
            try {
                FontMetrics fm = renderer.getFontMetrics(renderer.getFont());
                return fm.stringWidth(info) + 3;
            } catch (NullPointerException npe) {
                return -1;
            }
        } else {
            return -1;
        }
    }

    /**
     *@return The JToolTip returned is actually a JMultilineToolTip
     */
    @Override
    public JToolTip createToolTip() {
        JMultilineToolTip ret = JMultilineToolTip.instance();
        ret.setToolTipArray( tips );
        tips = DataUtils.EMPTY_STRING_ARRAY;
        return ret;
    }

    /**
     * Overrides JTable's default implementation in order to add
     * LimeTableColumn columns.
     */
    @Override
    public void createDefaultColumnsFromModel() {
        DataLineModel dlm = (DataLineModel)dataModel;
        if (dlm != null) {
            // Remove any current columns
            TableColumnModel cm = getColumnModel();
            while (cm.getColumnCount() > 0) {
                cm.removeColumn(cm.getColumn(0));
            }
        
            // Create new columns from the data model info
            for (int i = 0; i < dlm.getColumnCount(); i++) {
                TableColumn newColumn = dlm.getTableColumn(i);
                addColumn(newColumn);
            }
        }
    }
    
    /**
     * Returns the color that a specific row will be.
     */
    public Color getBackgroundForRow(int row) {
        if(row % 2 == 0 || !tableSettings.ROWSTRIPE.getValue())
            return getBackground();
        else
            return ThemeFileHandler.TABLE_ALTERNATE_COLOR.getValue();
    }
        
    
    /**
     * This overrides JTable.prepareRenderer so that we can stripe the
     * rows as needed.
     */    
    @Override
    public Component prepareRenderer(TableCellRenderer renderer,
      int row, int column) {
        if(renderer == null)
            throw new IllegalStateException("null renderer, row: " + row +
                ", column: " + column + ", id: " + tableSettings.getID() +
                ", columnId: " + getColumnModel().getColumn(column));
        
        Object value = getValueAt(row, column);
    	boolean isSelected = isCellSelected(row, column);
    	boolean rowIsAnchor = selectionModel.getAnchorSelectionIndex() == row;
    	boolean colIsAnchor = columnModel.getSelectionModel().getAnchorSelectionIndex() == column;
    	boolean hasFocus = rowIsAnchor && colIsAnchor && hasFocus();
    
    	Component r = renderer.getTableCellRendererComponent(this, value,
    	                                              isSelected, hasFocus,
    	                                              row, column);
	                      
        Color  odd = getEvenRowColor(row);
        Color even = getOddRowColor(row);
        
        if ( isSelected ) {
            // do nothing if selected.
        } else if (hasFocus && isCellEditable(row, column)) {
            // do nothing if we're focused & editting.
        } else if (even.equals(odd)) {
            // do nothing if backgrounds are the same.
        } else if (!tableSettings.ROWSTRIPE.getValue()) {
            // if the renderer's background isn't already the normal one,
            // change it.  (needed for real-time changing of the option)
            if( r != null && !r.equals(even) )
                r.setBackground(even);
        } else if ( row % 2 != 0 ) {
            r.setBackground(odd);
        } else {
            r.setBackground(even);
        }
        
        return r;
    }
    
    /**
     * Returns the next list element that starts with 
     * a prefix.
     *
     * @param prefix the string to test for a match
     * @param startIndex the index for starting the search
     * @param bias the search direction, either 
     * Position.Bias.Forward or Position.Bias.Backward.
     * @return the index of the next list element that
     * starts with the prefix; otherwise -1
     * @exception IllegalArgumentException if prefix is null
     * or startIndex is out of bounds
     */
    public int getNextMatch(String prefix, int startIndex, Position.Bias bias) {
        DataLineModel model = (DataLineModel)dataModel;
        int max = model.getRowCount();
        if (prefix == null)
            throw new IllegalArgumentException();
        if (startIndex < 0 || startIndex >= max)
            throw new IllegalArgumentException();
        prefix = prefix.toUpperCase();
        
        // start search from the next element after the selected element
        int increment = (bias == Position.Bias.Forward) ? 1 : -1;
        int index = startIndex;
        int typeAheadColumn = model.getTypeAheadColumn();
        if(typeAheadColumn >= 0 && typeAheadColumn < model.getColumnCount()) {
            do {
                Object o = model.getValueAt(index, typeAheadColumn);
                
                if (o != null) {
                    String string;
                    if (o instanceof String)
                        string = ((String)o).toUpperCase();
                    else {
                        string = o.toString();
                        if (string != null)
                            string = string.toUpperCase();
                    }
                    
                    if (string != null && string.startsWith(prefix))
                        return index;
                }
                index = (index + increment + max) % max;
            } while (index != startIndex);
        }
        return -1;
    }
    
    /*
     * Stretch JTable to JViewport height so that the space
     * underneath the rows fires mouse events as well
     */
    @Override
    public boolean getScrollableTracksViewportHeight() {
        Component parent = getParent();
        if (parent instanceof javax.swing.JViewport)
            return parent.getHeight() > getPreferredSize().height;
        return super.getScrollableTracksViewportHeight();
    }
    
    /**
     * Paints the table & a focused row border.
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        
        int focusedRow = getFocusedRow(true);
        if(focusedRow != -1 && focusedRow < getRowCount() ) {
            Border rowBorder = UIManager.getBorder("Table.focusRowHighlightBorder");
            if(rowBorder != null) {
                Rectangle rect = getCellRect(focusedRow, 0, true);
                rect.width = getWidth();
                rowBorder.paintBorder(this, g, rect.x, rect.y, rect.width, rect.height);
            }
        }
    }
    
    /**
     * Returns the default Color for an even row.  This <b>can</b> be overridden.
     * 
     * @param row the row in which we're interested
     * @return the default Color for an even row
     */
    protected Color getEvenRowColor(int row) {
        return ThemeFileHandler.TABLE_BACKGROUND_COLOR.getValue();
    }

    /**
     * Returns the default Color for an even row.  This <b>can</b> be overridden.
     * 
     * @param row the row in which we're interested
     * @return the default Color for an even row
     */
    protected Color getOddRowColor(int row) {
        return ThemeFileHandler.TABLE_ALTERNATE_COLOR.getValue();
    }     
    
    /**
     * Repaints the focused row if one was focused.
     */
    private void repaintFocusedRow() {
        int focusedRow = getFocusedRow(false);
        if(focusedRow != -1 && focusedRow < getRowCount()) {
            Rectangle rect = getCellRect(focusedRow, 0, true);
            rect.width = getWidth();
            repaint(rect);
        }
    }   
    
    /**
     * Gets the focused row.
     */
    private int getFocusedRow(boolean requireFocus) {
        if(!requireFocus || hasFocus())
            return selectionModel.getAnchorSelectionIndex();
        else
            return -1;
    }
    
    /**
     * Updates the row height appropriately
     */
    private void updateRowHeight() {
        int increment = ThemeSettings.FONT_SIZE_INCREMENT.getValue();
        if (increment != 0) {
            FontMetrics fm = getFontMetrics(getFont());
            setRowHeight(Math.max(fm.getHeight() + 1, DEFAULT_ROW_HEIGHT));
        }
    }
    
    /**
     * Overridden to update the table row size if necessary.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        updateRowHeight();
    }
    
    /**
     * Handler for repainting focus for all tables.
     */
    private static class FocusHandler implements FocusListener {
        private static final FocusListener INSTANCE = new FocusHandler();
        
        public void focusGained(FocusEvent e) {
            LimeJTable t = (LimeJTable)e.getSource();
            t.repaintFocusedRow();
        }
        
        public void focusLost(FocusEvent e) {
            LimeJTable t = (LimeJTable)e.getSource();
            t.repaintFocusedRow();
        }
    }
}
