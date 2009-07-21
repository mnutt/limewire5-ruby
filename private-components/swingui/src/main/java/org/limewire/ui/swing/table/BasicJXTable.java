package org.limewire.ui.swing.table;

import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXTable;

/**
 * A JXTable with some default functionality removed.
 * Specifically, this removes the 'Find' feature from the actionMap
 * and changes the 'enter' key to not move down.
 */
public class BasicJXTable extends JXTable {
    
    public BasicJXTable() {
        super();
        initialize();
    }

    public BasicJXTable(int numRows, int numColumns) {
        super(numRows, numColumns);
        initialize();
    }

    public BasicJXTable(Object[][] rowData, Object[] columnNames) {
        super(rowData, columnNames);
        initialize();
    }

    public BasicJXTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
        initialize();
    }

    public BasicJXTable(TableModel dm, TableColumnModel cm) {
        super(dm, cm);
        initialize();
    }

    public BasicJXTable(TableModel dm) {
        super(dm);
        initialize();
    }

    public BasicJXTable(Vector rowData, Vector columnNames) {
        super(rowData, columnNames);
        initialize();
    }

    private void initialize() {
        getActionMap().remove("find");
        //Default java behavior for the enter key is the same as the down arrow.  We don't want this.
        setEnterKeyAction(null);
    }

    /**
     * @param action the action that occurs when the user presses the enter key on the table
     */
    public void setEnterKeyAction(Action action){
        getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), "launchAction");
        getActionMap().put("launchAction", action);
    }
}
