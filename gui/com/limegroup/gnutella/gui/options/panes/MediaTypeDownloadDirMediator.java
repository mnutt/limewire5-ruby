package com.limegroup.gnutella.gui.options.panes;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Iterator;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.limewire.util.MediaType;

import com.limegroup.gnutella.gui.FileChooserHandler;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.PaddedPanel;
import com.limegroup.gnutella.gui.options.OptionsMediator;
import com.limegroup.gnutella.gui.search.NamedMediaType;
import com.limegroup.gnutella.gui.tables.AbstractTableMediator;
import com.limegroup.gnutella.gui.tables.LimeJTable;
import com.limegroup.gnutella.gui.tables.TableSettings;

/**
 * Class which handles the construction of the download directories per
 * mediatype table and its associated actions.
 */
class MediaTypeDownloadDirMediator extends AbstractTableMediator<MediaTypeDownloadDirModel, MediaTypeDownloadDirDataLine, NamedMediaType> {
	/**
	 * The PaneItem on which the media type is displayed.
	 */
	private final JTextField saveField;

	/**
	 * Handle to the browse action which can be retrieved through
	 * {@link #getBrowseDirectoryAction()}.
	 */
	private BrowseDirectoryAction browseAction;

	/**
	 * Handle to the reset action which can be retrieve through the
	 * {@link #getResetDirectoryAction()}.
	 */
	private ResetDirectoryAction resetAction;
	
	/**
	 * Workaround to allow reinstantiation without getting duplicate settings keys.
	 */
	private static TableSettings cachedSettings = null;
	
	/**
	 * @param item
	 * @param id
	 */
	public MediaTypeDownloadDirMediator(JTextField field) {
		super("MEDIA_TYPE_DOWNLOAD_DIR_TABLE");
		saveField = field;
		saveField.getDocument().addDocumentListener(new DocumentHandler());
	}

	/**
	 * Returns true if the settings in the table have changed and they need to
	 * be saved.
	 */
	public boolean isDirty() {
	    for( int i = 0; i < DATA_MODEL.getRowCount(); i++) {
	        if(DATA_MODEL.get(i).isDirty())
	            return true;
	    }
	    return false;
	}

	/**
	 * Overriden to allow reinstantiation of this class.
	 */
	@Override
    protected void buildSettings() {
		if (cachedSettings == null) {
			super.buildSettings();
			cachedSettings = SETTINGS;
		}
		SETTINGS = cachedSettings;
	}

	@Override
    protected void setupConstants() {
		MAIN_PANEL = new PaddedPanel(0);
		DATA_MODEL = new MediaTypeDownloadDirModel();
		TABLE = new LimeJTable(DATA_MODEL);
		TABLE.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// construct actions here to avoid null pointer exceptions in
		// handleNoSelection()
		browseAction = new BrowseDirectoryAction();
		resetAction = new ResetDirectoryAction();
	}
	
	/**
	 * Save options for each mediatype in the table.
	 */
	public boolean applyOptions(Set<? super File> newDirs) {
		for (int i = 0; i < DATA_MODEL.getRowCount(); i++) {
			MediaTypeDownloadDirDataLine dl = DATA_MODEL.get(i);
			dl.saveDirectory(newDirs);
		}
		
		return false;
	}
	
	/** 
	 * Adds save dirs to the given option.
	 */
	void addSaveDirs(Set<File> set) {
		for (int i = 0; i < DATA_MODEL.getRowCount(); i++) {
			String dir = DATA_MODEL.get(i).getDirectory();
			if(dir != null)
			    set.add(new File(dir));
		}
	}

	/**
	 * Fill the table with the mediatypes and the directory settings.
	 */
	public void initOptions() {
		DATA_MODEL.clear();
		for (Iterator i = NamedMediaType.getAllNamedMediaTypes().iterator(); i.hasNext();) {
			NamedMediaType nm = (NamedMediaType) i.next();
			if (!nm.getMediaType().getSchema().equals(MediaType.SCHEMA_ANY_TYPE))
				DATA_MODEL.add(nm);
		}
		//  sort alphabetically, so sort twice
		DATA_MODEL.sort(0);
		DATA_MODEL.sort(0);
		updateModel();
	}

	public Action getBrowseDirectoryAction() {
		return browseAction;
	}

	public Action getResetDirectoryAction() {
		return resetAction;
	}

	/**
	 * Overriden to disable removal of rows.
	 */
	@Override
    public void removeSelection() { }

	/*
	 * @see com.limegroup.gnutella.gui.tables.AbstractTableMediator#updateSplashScreen()
	 */
	@Override
    protected void updateSplashScreen() { }

	/*
	 * @see com.limegroup.gnutella.gui.tables.AbstractTableMediator#createPopupMenu()
	 */
	@Override
    protected JPopupMenu createPopupMenu() {
		JPopupMenu menu = new JPopupMenu();
		menu.add(new JMenuItem(browseAction));
		menu.add(new JMenuItem(resetAction));
		return menu;
	}

	/**
	 * Overridden to not popup a menu on the table header.
	 */
	@Override
    public void handleHeaderPopupMenu(Point p) { }
	
	/*
	 * @see com.limegroup.gnutella.gui.tables.ComponentMediator#handleActionKey()
	 */
	public void handleActionKey() { }

	/**
	 * Enable the browse and reset action.
	 */
	public void handleSelection(int row) {
		browseAction.setEnabled(true);
		resetAction.setEnabled(true);
	}

	/**
	 * Disable the browse and reset action.
	 */
	public void handleNoSelection() {
		browseAction.setEnabled(false);
		resetAction.setEnabled(false);
	}

	/**
	 * Opens a directory chooser dialog and sets the selected directory for the
	 * selected mediatype row.
	 */
	private class BrowseDirectoryAction extends AbstractAction {
		public BrowseDirectoryAction() {
			putValue(Action.NAME, I18n
					.tr("Browse..."));
			putValue(Action.SHORT_DESCRIPTION, I18n
					.tr("Open Dialog to Select a Folder"));
		}

		/*
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent e) {
			MediaTypeDownloadDirDataLine line = DATA_MODEL.get(TABLE.getSelectedRow());

			if (line == null) {
				throw new IllegalArgumentException(
						"browse action was not correctly disabled");
			}

			File dir = FileChooserHandler
					.getInputDirectory(OptionsMediator.instance().getMainOptionsComponent(),
							new File(line.getVisibleDirectoryString()));

			if (dir != null)
                line.setDirectory(dir);
		}
	}

	/**
	 * Resets the selected mediatype row so that its download directory is
	 * always the default download directory.
	 */
	private class ResetDirectoryAction extends AbstractAction {

		public ResetDirectoryAction() {
			putValue(Action.NAME, I18n
					.tr("Reset"));
			putValue(Action.SHORT_DESCRIPTION, I18n
					.tr("Reset to Default Download Folder"));
		}

		public void actionPerformed(ActionEvent e) {
            MediaTypeDownloadDirDataLine mediaLine =
                DATA_MODEL.get(TABLE.getSelectedRow());

			if (mediaLine == null) {
				throw new IllegalArgumentException(
						"reset action was not correctly disabled");
			}
			mediaLine.reset();
			MediaTypeDownloadDirMediator.this.DATA_MODEL.fireTableDataChanged();
		}
	}

	/**
	 * Listens for document changes of the {@link SaveDirPaneItem#_saveField}
	 * and updates the table model accordingly.
	 */
	private class DocumentHandler implements DocumentListener {

		public void insertUpdate(DocumentEvent e) {
			MediaTypeDownloadDirMediator.this.updateModel();
		}

		public void removeUpdate(DocumentEvent e) {
			MediaTypeDownloadDirMediator.this.updateModel();
		}

		public void changedUpdate(DocumentEvent e) {
		}
	}

	/**
	 * Notify all mediatype rows of a possible change of the default download
	 * directory.
	 */
	private void updateModel() {
		String text = saveField.getText();
		for (int i = 0; i < TABLE.getRowCount(); i++) {
			MediaTypeDownloadDirDataLine line = DATA_MODEL.get(i);
			line.setDefaultDir(text);
		}
		DATA_MODEL.fireTableDataChanged();
	}
}
