package org.limewire.ui.swing.table;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.search.RowPresevationListener;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventTableModel;

/**
 * This class is a subclass of JTable that adds features from Glazed Lists.
 */
public class ConfigurableTable<E> extends MouseableTable implements RowPresevationListener {
    private final Log LOG = LogFactory.getLog(getClass());
    private EventList<E> eventList;
    private EventTableModel<E> tableModel;
    private VisibleTableFormat<E> tableFormat;
    private E selectedRow;

    public ConfigurableTable(boolean showHeaders) {
        if (showHeaders) {
            // Set up table headers to have a context menu
            // that configures which columns are visible.
            final JTableHeader header = getTableHeader();
            header.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if(SwingUtilities.isRightMouseButton(e)){
                        showHeaderPopupMenu(e.getPoint());
                    }
                }
            });
        } else {
            // Remove the table header.
            setTableHeader(null);
        }
        setShowHorizontalLines(false);
    }

    public EventList<E> getEventList() {
        return eventList;
    }
    
    /**
     * Displays the popup menu on the table header
     */
    public void showHeaderPopupMenu(Point p) {
        JPopupMenu menu = createHeaderColumnMenu();
        menu.show(getTableHeader(), p.x, p.y);
    }
    
    /**
     * Lazily creates the popup menu to display
     * @return
     */
    public JPopupMenu createHeaderColumnMenu() {
        return new TableColumnSelector(this, tableFormat).getPopupMenu();
    }

    /**
     * Sets the width of a given column.
     * @param columnIndex the column index
     * @param width the width
     */
    public void setColumnWidth(int columnIndex, int width) {
        getColumnModel().getColumn(columnIndex).setPreferredWidth(width);
    }
    
    public void initColumnVisibility() {
        for(int i = tableFormat.getColumnCount() -1; i >= 0; i--) {
            getColumnExt(i).setVisible(tableFormat.isColumnHiddenAtStartup(i));
        }
    }

    /**
     * Sets the EventList table model from which
     * the data to be displayed is obtained.
     * @param eventList the EventList
     */
    public void setEventList(EventList<E> eventList) {
        this.eventList = eventList;
    }

    /**
     * Set the object that controls the columns displayed in this table.
     * @param tableFormat the TableFormat
     */
    public void setTableFormat(VisibleTableFormat<E> tableFormat) {
        if (tableFormat == null) {
            throw new IllegalArgumentException("tableFormat can't be null");
        }

        this.tableFormat = tableFormat;
        tableModel = new EventTableModel<E>(eventList, tableFormat);
        setModel(tableModel);
    }
    
    public EventTableModel<E> getEventTableModel() {
        return tableModel;
    }

    /**
     * This is overridden to prevent a java.lang.IllegalStateException
     * that occurs because a reverse mapping function must be specified
     * to support this operation.
     * @param aValue the new value
     * @param row the row
     * @param column the column
     */
    @Override
    public void setValueAt(Object aValue, int row, int column) {
    }

    @Override
    public void setDefaultEditor(Class clazz, TableCellEditor editor) {
        super.setDefaultEditor(clazz, editor);
        if (clazz != String.class) return;
        LOG.debugf("ConfigurableTable: editor for {0} is now {1}", clazz.getName() + editor.getClass().getName());
    }

    @Override
    public void setDefaultRenderer(Class clazz, TableCellRenderer renderer) {
        super.setDefaultRenderer(clazz, renderer);
        if (clazz != String.class) return;
        LOG.debugf("ConfigurableTable: renderer for {0} is now {1}", clazz.getName(), renderer.getClass().getName());
    }

    @Override
    public void preserveRowSelection() {
        selectedRow = null;
        if (isVisible()) {
            int preservedSelectedRow = getSelectedRow();
            if (preservedSelectedRow > -1) {
                selectedRow = eventList.get(preservedSelectedRow);
            } else {
                selectedRow = null;
            }
        }
    }

    @Override
    public void restoreRowSelection() {
        if (isVisible() && selectedRow != null) {
            int rowSelection = eventList.indexOf(selectedRow);
            if (rowSelection > -1) {
                setRowSelectionInterval(rowSelection, rowSelection);
            }
        }
        selectedRow = null;
    }
}