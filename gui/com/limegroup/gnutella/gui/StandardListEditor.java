package com.limegroup.gnutella.gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.limewire.i18n.I18nMarker;

/**
 * This reusable component is a list with buttons for adding and removing
 * elements.  The add button brings up a dialog window to retrieve
 * the element to add from the user.  The remove button is only enabled
 * when there is an item selected in the list.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class StandardListEditor {

	/**
	 * Constant handle to the main panel in the editor.
	 */
	private final JPanel MAIN_PANEL = new JPanel(new GridBagLayout());

	/**
	 * Constant handle to the underlying <tt>JList</tt> instance.
	 */
	private final JList LIST = new JList();
	
	/**
	 * Handle to the remove button to allow us to set its state.
	 */
	private final JButton REMOVE_BUTTON;

	/**
	 * Handle to the add button.
	 */
	private final JButton ADD_BUTTON;

	/**
	 * Default add action which delegates to the <code>addListener</code> if it is set.
	 */
	private AddAction _addAction;
	
	/**
	 * Handle to the remove action used for disabling it when no item in the list is selected.
	 */
	private RemoveAction _removeAction;
	
	/**
	 * Handle to the set add listener.
	 */
	private ActionListener _addListener = null;

	/**
	 * Member variable for whether or not the list data has changed
	 * since the last call to reset this value.
	 */
	private boolean _listChanged = false;
	
	/**
	 * Creates a <tt>StandardListEditor</tt> with the list of elements on
	 * the left and buttons for adding and removing elements on the
	 * right.  The key allows for a custom label for the text field
	 * in the dialog window that the "Add..." button pops up.  For example, 
	 * using <p>
	 *
	 * OPTIONS_AUTO_CONNECT_INPUT_FIELD_LABEL=Enter Host Address:<p>
	 *
	 * in the messages bundle will make "Enter Host Address: " appear 
	 * as the label for the text field in dialog window popped up by
	 * the "add" button.  
	 *
	 * @param INPUT_FIELD_KEY the key for the locale-specific label for the dialog
	 * window popped up by the "Add..." button
	 */
	public StandardListEditor(final String INPUT_FIELD_KEY) {
		this(I18nMarker.marktr("Add..."), I18nMarker.marktr("Remove"),INPUT_FIELD_KEY);
	}

	/**
	 * Creates a <tt>StandardListEditor</tt> with the default settings
	 * for the names of the add and remove buttons and that uses
	 * the specified <tt>ActionListener</tt> instance as the listener
	 * for the add button.
	 */
	public StandardListEditor(ActionListener listener) {
		this(I18nMarker.marktr("Add..."), I18nMarker.marktr("Remove"),"");
		setAddActionListener(listener);
	}

	/**
	 * More flexible constructor that allows the text for the add and
	 * remove buttons to be set and that allows a custom <tt>ActionListener</tt>
	 * for the add button.
	 *
	 * @param ADD_BUTTON_KEY the key for the locale-specific string to use for
	 *  the add button
	 * @param REMOVE_BUTTON_KEY the key for the locale-specific string to use 
	 *  for the remove button
	 * @param INPUT_FIELD_KEY the key for the locale-specific string to use
	 *  for the label in the generic text input component used by default
	 */
	public StandardListEditor(final String ADD_BUTTON_KEY, 
							  final String REMOVE_BUTTON_KEY,
							  final String INPUT_FIELD_KEY) {	

		_addAction = new AddAction(ADD_BUTTON_KEY, INPUT_FIELD_KEY);
		_removeAction = new RemoveAction(REMOVE_BUTTON_KEY);

		GUIUtils.bindKeyToAction(LIST,
				KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), _removeAction);
		
		LIST.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		LIST.addListSelectionListener(new ListEditorSelectionListener());
		JScrollPane scrollPane = new JScrollPane(LIST);
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.gridheight = GridBagConstraints.REMAINDER;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		MAIN_PANEL.add(scrollPane, gbc);
		
		gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.NORTHWEST;
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 3, ButtonRow.BUTTON_SEP, 0);
		
		ADD_BUTTON = new JButton(_addAction);
		MAIN_PANEL.add(ADD_BUTTON, gbc);
		
		REMOVE_BUTTON = new JButton(_removeAction);
		REMOVE_BUTTON.setEnabled(false);
		MAIN_PANEL.add(REMOVE_BUTTON, gbc);
	}

	/**
	 * Provides access to the wrapped <tt>Component</tt> of the 
	 * <tt>StandardListEditor</tt>.  This is the <tt>Component</tt>
	 * that other gui elements should add.
	 *
	 * @return the underlying <tt>Component</tt> that contains all 
	 *  of the elements fo the <tt>StandardListEditor</tt>
	 */
	public Component getComponent() {
		return MAIN_PANEL;
	}

	/**
	 * Adds the specified object to the end of the list.
	 *
	 * @param element the <tt>Object</tt> to add
	 */
	private void addElement(Object element) {
		DefaultListModel model = (DefaultListModel)LIST.getModel();
		model.addElement(element);
	}

	/**
	 * Adds the specified <tt>File</tt> instance to the end of the list.
	 *
	 * @param file the <tt>File</tt> to add
	 */
	public void addFile(File file) {
		addElement(file);
	}

	/**
	 * Adds the specified <tt>String</tt> instance to the end of the list.
	 *
	 * @param string the <tt>String</tt> to add
	 */
	public void addString(String string) {
		addElement(string);
	}

	/**
	 * Sets the data in the underlying <tt>ListModel</tt>.
	 *
	 * @param data the <tt>Vector</tt> containing data for the model
	 */
	public void setListData(Vector data) {
		DefaultListModel model = new DefaultListModel();
		for (int i=0; i<data.size(); i++)
			model.addElement(data.get(i));
		LIST.setModel(model);
	}

	/**
	 * Sets the data in the underlying <tt>ListModel</tt>.
	 *
	 * @param data array of strings containing the data for the model
	 */
	private void setListDataObjects(Object[] data) {
		DefaultListModel model = new DefaultListModel();
		for (int i=0; i<data.length; i++)
			model.addElement(data[i]);
		LIST.setModel(model);
	}

	/**
	 * Sets the data in the underlying <tt>ListModel</tt>.
	 *
	 * @param data array of <tt>File</tt> instances containing the data for the 
	 *  model
	 */
	public void setListData(File[] data) {
		setListDataObjects(data);
	}

	/**
	 * Sets the data in the underlying <tt>ListModel</tt>.
	 *
	 * @param data array of <tt>String</tt> instances containing the data for the 
	 *  model
	 */
	public void setListData(String[] data) {
		setListDataObjects(data);
	}
	
	/**
	 * Clears the selections in the list.
	 */
	public void clearSelection() {
		LIST.clearSelection();
	}

	/**
	 * Returns an array of the underlying data represented as strings
	 * by calling toString() on each element.
	 *
	 * @return the list data as an array of strings.
	 */
	public String[] getDataAsStringArray() {
		DefaultListModel model = (DefaultListModel)LIST.getModel();
		Object[] dataObjects = model.toArray();
		String[] dataStrings = new String[dataObjects.length];
		for(int i=0; i<dataObjects.length; i++) {
			dataStrings[i] = dataObjects[i].toString();
		}
		return dataStrings;
	}

	/**
	 * Returns an array of the underlying data represented as <tt>File</tt>
	 * instances.
	 *
	 * @return the list data as an array of <tt>File</tt> instances.
	 */
	public File[] getDataAsFileArray() {
		DefaultListModel model = (DefaultListModel)LIST.getModel();
		Object[] dataObjects = model.toArray();
		File[] dataFiles = new File[dataObjects.length];
		for(int i=0; i<dataObjects.length; i++) {
			dataFiles[i] = (File)dataObjects[i];
		}
		return dataFiles;
	}

	/**
	 * Returns an array of the underlying data represented as objects.
	 *
	 * @return the list data as an array of objects.
	 */
	public Object[] getDataAsObjectArray() {
		DefaultListModel model = (DefaultListModel)LIST.getModel();
		return model.toArray();
	}

	/**
	 * Sets the <tt>ActionListener</tt> to use for the add button.
	 *
	 * @param addAction the <tt>ActionListener</tt> to use for the add button
	 */
  	private void setAddActionListener(ActionListener addListener) {
  		this._addListener = addListener;
  	}

	/**
	 * Returns whether or not the list has changed since the last
	 * call to reset the list.  Note that this will not be 
	 * accurate if you use your own <tt>ActionListener</tt> for the
	 * add button.  In this case, this will return whether or not
	 * elements have been removed from the list.
	 *
	 * @return <tt>true</tt> if the list has changed since the last
	 *  call to reset the list, <tt>false</tt> otherwise
	 */
	public boolean getListChanged() {
		return _listChanged;
	}

	/**
	 * Resets the value for whether or not the list has changed to
	 * <tt>false</tt>.
	 */
	public void resetList() {
		_listChanged = false;
	}

	/**
	 * This class responds to a click of the add button and pops
	 * up a window for the user to enter a new element to add
	 * to the list.
	 */
	private class AddAction extends AbstractAction {
		
		private final String INPUT_FIELD_KEY;
		
		public AddAction(final String name, final String key) {
			putValue(Action.NAME, I18n.tr(name));
			putValue(Action.SHORT_DESCRIPTION, I18n.tr("Add New List Item"));
			INPUT_FIELD_KEY = key;
		}
		
		public void actionPerformed(ActionEvent e) {
			
			// delegate event if there is a special addListener
			if (_addListener != null) {
				_addListener.actionPerformed(e);
			}
			else {
			
			InputFieldDialog dialog = new InputFieldDialog(INPUT_FIELD_KEY);
			int returnCode = dialog.showDialog();

			if(returnCode == InputFieldDialog.TEXT_ENTERED) {
				_listChanged = true;
				addElement(dialog.getText());
			}
		}
	}
	}


	/**
	 * This class responds to a click of the remove button and removes
	 * the selected element from the list.
	 */
	private class RemoveAction extends AbstractAction {
		
		public RemoveAction(final String name)
		{
			 putValue(Action.NAME, I18n.tr(name));
			 putValue(Action.SHORT_DESCRIPTION, I18n.tr("Remove Selected Item"));
		}
		
		public void actionPerformed(ActionEvent e) {
			_listChanged = true;
			// return if nothing is selected
			if(LIST.isSelectionEmpty()) return;
			
			DefaultListModel model = (DefaultListModel)LIST.getModel();
			model.remove(LIST.getSelectedIndex());

			if(LIST.isSelectionEmpty()) {
				REMOVE_BUTTON.setEnabled(false);
			}
		}
	}

	/**
	 * This private class handles selection of items in the list.  It
	 * controls the state of the remove button as well, disabling it
	 * if nothing is selected.
	 */
	private class ListEditorSelectionListener implements ListSelectionListener {

		/**
		 * Implements the <tt>ListSelectionListener</tt> interface.  
		 * Responds to selections in the list.
		 */
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting())
				return;
			if (LIST.isSelectionEmpty()) {
				REMOVE_BUTTON.setEnabled(false);
			} else {
				REMOVE_BUTTON.setEnabled(true);
			}
		}
	}
}
