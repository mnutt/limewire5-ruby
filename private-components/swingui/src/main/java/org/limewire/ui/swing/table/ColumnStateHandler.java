package org.limewire.ui.swing.table;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.TableColumnExt;
import org.limewire.ui.swing.settings.TablesHandler;

/**
 * Saves the state of columns in a given table. 
 */
public class ColumnStateHandler implements TableColumnModelListener, MouseListener, PropertyChangeListener {

    private final JXTable table;
    private final VisibleTableFormat format;
    
    
    private boolean columnMoved = false;
    
    public ColumnStateHandler(JXTable table, VisibleTableFormat format) {
        this.table = table;
        this.format = format;
        startListening();
        for(int i = 0; i < format.getColumnCount(); i++) {
            table.getColumnExt(i).addPropertyChangeListener(this);
        }
    }
    
    private void startListening() {
        table.getTableHeader().addMouseListener(this);
        table.getColumnModel().addColumnModelListener(this);
//        for(int i = 0; i < format.getColumnCount(); i++) {
//            table.getColumnExt(i).addPropertyChangeListener(this);
//        }
    }
    
    private void stopListening() {
        table.getTableHeader().removeMouseListener(this);
        table.getColumnModel().removeColumnModelListener(this);
//        for(int i = 0; i < format.getColumnCount(); i++) {
//            table.getColumnExt(i).removePropertyChangeListener(this);
//        }
    }
    
    public void removeListeners() {
        stopListening();
        for(int i = 0; i < format.getColumnCount(); i++) {
            table.getColumnExt(format.getColumnName(i)).removePropertyChangeListener(this);
        }
    }
    
    @Override
    public void columnAdded(TableColumnModelEvent e) {}

    @Override
    public void columnMarginChanged(ChangeEvent e) {}


    @Override
    public void columnMoved(TableColumnModelEvent e) {
        if( e.getFromIndex() == e.getToIndex() || !table.isShowing()) return;
        
        // wait till after the mouse was released to save new column ordering
        columnMoved = true;
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) {}

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {
        //if columns were moved, save any changes after the moving stopped
        if(columnMoved) {
            columnMoved = false;

            saveColumnOrder();
        }
    }
    
    /**
     * Saves the current column ordering to disk if a column is not in its default
     * index. Ignores hidden columns and saves the current ordering of visible
     * columns.
     */
    private void saveColumnOrder() {
        for(int i = 0; i < table.getColumnCount(); i++) {
            TableColumn column = table.getColumn(i);
            ColumnStateInfo info = format.getColumnInfo(column.getModelIndex());
            if(info.getPreferredViewIndex() != i) {
                info.setPreferredViewIndex(i);
                setOrder(info, i);
            }
        }
    }
    
    public void revertToDefault() {
//        stopListening();

        //TODO: revert to defaults

//        startListening();
    }
    
    public void setupColumnWidths() {
        for(int i = 0; i < format.getColumnCount(); i++) {
            table.getColumn(i).setPreferredWidth(format.getInitialWidth(i));
        }
    }
    
    public void setupColumnOrder() {
        stopListening();

        for(int i = 0; i < table.getColumnCount(); i++) {
            TableColumnExt column = table.getColumnExt(i); 
            ColumnStateInfo info = format.getColumnInfo(column.getModelIndex());
            if(i != info.getPreferredViewIndex()) {
                if(info.getPreferredViewIndex() >= 0 && info.getPreferredViewIndex() < table.getColumnCount()) {
                    table.getColumnModel().moveColumn(i, info.getPreferredViewIndex());
                }
            }
        }

        startListening();
    }
    
    public void setupColumnVisibility() {
        stopListening();

        for(int i = format.getColumnCount()-1; i >= 0; i--) {
            TableColumnExt column = table.getColumnExt(i);
            column.setVisible(format.isVisibleAtStartup(column.getModelIndex()));
        }

        startListening();
    }
    
    private void setVisibility(ColumnStateInfo info, boolean isVisible) {
        TablesHandler.getVisibility(info.getId(), info.isDefaultlyShown()).setValue(isVisible);
    }
    
    private void setOrder(ColumnStateInfo column, int order) {
        TablesHandler.getOrder(column.getId(), column.getModelIndex()).setValue(order);
    }
    
    private void setWidth(ColumnStateInfo column, int width) {
        TablesHandler.getWidth(column.getId(), column.getDefaultWidth()).setValue(width);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {        
        //width of column changed
        if(evt.getPropertyName().equals("width") && table.isShowing()) {
            ColumnStateInfo info = format.getColumnInfo(((TableColumnExt)evt.getSource()).getModelIndex());
            setWidth(info,(Integer) evt.getNewValue() );
        }
        
        //visibility changed
        if(evt.getPropertyName().equals("visible") && table.isShowing()) {
            ColumnStateInfo info = format.getColumnInfo(((TableColumnExt)evt.getSource()).getModelIndex());
            setVisibility(info, Boolean.TRUE.equals(evt.getNewValue()));
            
            //column visibility changed, so check the ordering
            saveColumnOrder();
        }
    }
}
