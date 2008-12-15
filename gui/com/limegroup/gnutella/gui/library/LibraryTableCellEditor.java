package com.limegroup.gnutella.gui.library;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import org.limewire.util.VersionUtils;

import com.limegroup.gnutella.gui.LimeTextField;
import com.limegroup.gnutella.gui.tables.LimeJTable;

/**
 * An editor for a table cell in the library.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class LibraryTableCellEditor implements TableCellEditor {

	private JTextField _textField;
	private LibraryTableMediator _libraryTable;
	private CellEditorListener _cellEditorListener;
	private int lastRow = -1;
    
	/**
	 * An event to use when signalling that the editing was
	 * programatically started.
	 */
	static final EventObject EVENT = new EventObject(new Object());

	/**
	 * Constructs a new <tt>LibraryTableCellEditor</tt> instance.
	 * 
	 * @param libraryTable a reference to the <tt>LibraryTable</tt> instance 
	 * that this is a cell editor for
	 */
	LibraryTableCellEditor(LibraryTableMediator libraryTable) {
		_libraryTable = libraryTable;
	}

    /**
	 * returns the Object value (a String) of the text field
	 * cell editor.
	 * implements javax.swing.CellEditor.
	 */
    public Object getCellEditorValue() {
		String newName = _textField.getText();
		return _libraryTable.handleNameChange(newName);
    }

    /** 
	 * returns a value determining whether or not the 
	 * cell is editable.
	 * implements javax.swing.CellEditor.
	 *
	 * @param anEvent  An <code>EventObject</code> instance containing 
	 *                 information about the event
	 *
	 * @return <code>true</code> if the event is a mouse event, the mouse has
	 *         been clicked once, and the cell is editable
	 */
    public boolean isCellEditable(EventObject event) {
		if (event instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) event;
            LimeJTable table = (LimeJTable) _libraryTable.getTable();

            // java6+ passes all mouse clicks to both the table and the cell
            if (VersionUtils.isJava16OrAbove()) {
                if (table.getSelectedRow() == lastRow) 
                    return (me.getClickCount() < 2);
                else {
                    lastRow = table.getSelectedRow();
                    return false;
                }
            }
            // java5 passes first mouse click to the table only and all other
            // mouse
            // clicks in the row to the table and cell
            else 
                return (me.getClickCount() < 2);
        } else 
            return event == EVENT;
    }
    
    /**
	 * returns a boolean determining whether or not the
	 * cell should be selected.
	 * implements javax.swing.CellEditor.
	 *
	 * @param anEvent  An <code>EventObject</code> instance containing 
	 *                 information about the event
	 *
	 * @return Always returns <code>true</code>, as in this case the cell 
	 *         should always be selectable
	 */
    public boolean shouldSelectCell(EventObject anEvent) { 
		return true; 
    }

    /**
	 * stops cell editing and returns a boolean indicating
	 * that the editing has stopped.
	 * implements javax.swing.CellEditor.
	 *
	 * @return Always returns <code>true</code>, as in this case cell editing
	 *         should always be stopped
	 */
    public boolean stopCellEditing() {
		fireEditingStopped();
    	return true;
    }

    /**
	 * cancels the editing of the current cell.
	 * implements javax.swing.CellEditor.
	 */
    public void cancelCellEditing() {
		fireEditingCanceled();
    }

    /**
	 * Sets the CellEditorListener to be the passed in listener. 
	 * implements javax.swing.CellEditor.
	 *
	 * @param listener The <code>CellEditorListener</code> to set as this 
	 *                 instance's listener.
	 */
    public void addCellEditorListener(CellEditorListener listener) {
		_cellEditorListener = listener;
    }

    /**
	 * sets the CellEditorListener to null.
	 * implements javax.swing.CellEditor.
	 *
	 * @param listener Not used at all in this implementation of CellEditor
	 */
    public void removeCellEditorListener(CellEditorListener listener) {
		_cellEditorListener = null;
    }

    /**
     * notify all listeners that have registered interest for
     * notification on this event type.  The event instance 
     * is lazily created using the parameters passed into 
     * the fire method.
     */
    void fireEditingStopped() {
		if(_cellEditorListener != null)
			_cellEditorListener.editingStopped(new ChangeEvent(this));
    }
	
    /**
     * notify all listeners that have registered interest for
     * notification on this event type.  The event instance 
     * is lazily created using the parameters passed into 
     * the fire method.
     */
    void fireEditingCanceled() {
		if(_cellEditorListener != null)
			_cellEditorListener.editingCanceled(new ChangeEvent(this));
    }

	/** 
	 * method implementing the TableCellEditor interface that
	 * returns the text field editing component.
	 */
    public Component getTableCellEditorComponent(JTable table, Object value,
												 boolean isSelected,
												 int row, int column) {
        JTextField field = new LimeTextField();
        field.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fireEditingStopped();
    		}
    	});

        field.setText(value != null ? value.toString() : "");
		field.selectAll();
		field.setCaretPosition(0);
		field.requestFocus();
        _textField = field;
		return field;
    }
}

