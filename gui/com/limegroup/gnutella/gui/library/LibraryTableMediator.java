package com.limegroup.gnutella.gui.library;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.MouseInputListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.limewire.collection.Tuple;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SaveLocationException.LocationCode;
import org.limewire.core.settings.QuestionsHandler;
import org.limewire.io.NetworkUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.CantResumeException;
import com.limegroup.gnutella.downloader.IncompleteFileManager;
import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.CheckBoxList;
import com.limegroup.gnutella.gui.CheckBoxListPanel;
import com.limegroup.gnutella.gui.FileDescProvider;
import com.limegroup.gnutella.gui.FileDetailsProvider;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.IconManager;
import com.limegroup.gnutella.gui.LicenseWindow;
import com.limegroup.gnutella.gui.MessageService;
import com.limegroup.gnutella.gui.MultiLineLabel;
import com.limegroup.gnutella.gui.actions.ActionUtils;
import com.limegroup.gnutella.gui.actions.BitziLookupAction;
import com.limegroup.gnutella.gui.actions.CopyMagnetLinkToClipboardAction;
import com.limegroup.gnutella.gui.actions.LimeAction;
import com.limegroup.gnutella.gui.actions.SearchAction;
import com.limegroup.gnutella.gui.library.RecursiveSharingDialog.State;
import com.limegroup.gnutella.gui.playlist.PlaylistMediator;
import com.limegroup.gnutella.gui.tables.AbstractTableMediator;
import com.limegroup.gnutella.gui.tables.LimeJTable;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;
import com.limegroup.gnutella.gui.util.CoreExceptionHandler;
import com.limegroup.gnutella.gui.util.GUILauncher;
import com.limegroup.gnutella.gui.util.GUILauncher.LaunchableProvider;
import com.limegroup.gnutella.gui.xml.editor.CCPublishWizard;
import com.limegroup.gnutella.gui.xml.editor.MetaEditor;
import com.limegroup.gnutella.gui.xml.editor.XmlTypeEditor;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.LibraryUtils;
import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.licenses.VerificationListener;
import com.limegroup.gnutella.util.EncodingUtils;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * This class wraps the JTable that displays files in the library,
 * controlling access to the table and the various table properties.
 * It is the Mediator to the Table part of the Library display.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class LibraryTableMediator extends AbstractTableMediator<LibraryTableModel, LibraryTableDataLine, File>
	implements VerificationListener, FileDetailsProvider {

//    private static final Log LOG = LogFactory.getLog(LibraryTableMediator.class);
	
	/**
     * Variables so the PopupMenu & ButtonRow can have the same listeners
     */
    public static Action LAUNCH_ACTION;
    public static Action ENQUEUE_ACTION;
	public static Action DELETE_ACTION;
    public static Action ANNOTATE_ACTION;
    public static Action RESUME_ACTION;
    
    public static Action RENAME_ACTION;
	
	public static Action SHARE_ACTION;
	public static Action UNSHARE_ACTION;
	public static Action SHARE_FOLDER_ACTION;
	public static Action UNSHARE_FOLDER_ACTION;

	private Action PUBLISH_ACTION;
    private Action EDIT_LICENSE_ACTION;
    private Action VIEW_LICENSE_ACTION;

    private Action BITZI_LOOKUP_ACTION;
    private Action MAGNET_LOOKUP_ACTION;
	private Action COPY_MAGNET_TO_CLIPBOARD_ACTION;

    /**
     * Whether or not the incomplete directory is selected.
     */
    private boolean _isIncomplete;

	/**
	 * Annotation can be turned on once XML is set up.
	 */
	private boolean _annotateEnabled = false;
	
    /**
     * instance, for singelton access
     */
    private static LibraryTableMediator _instance = new LibraryTableMediator();

    public static LibraryTableMediator instance() { return _instance; }

    /**
     * Build some extra listeners
     */
    @Override
    protected void buildListeners() {
        super.buildListeners();

        LAUNCH_ACTION = new LaunchAction();
        ENQUEUE_ACTION = new EnqueueAction();
		DELETE_ACTION = new RemoveAction();
        ANNOTATE_ACTION = new AnnotateAction();
        RESUME_ACTION = new ResumeAction();
        
        RENAME_ACTION = new RenameAction();
		
		SHARE_ACTION = new ShareFileAction();
		UNSHARE_ACTION = new UnshareFileAction();

        PUBLISH_ACTION = new PublishAction();
        EDIT_LICENSE_ACTION = new EditLicenseAction();
        VIEW_LICENSE_ACTION = new ViewLicenseAction();
        
        BITZI_LOOKUP_ACTION = new BitziLookupAction(this);
        MAGNET_LOOKUP_ACTION = new MagnetLookupAction();
        
		COPY_MAGNET_TO_CLIPBOARD_ACTION = new CopyMagnetLinkToClipboardAction(this);

		SHARE_FOLDER_ACTION = new ShareFolderAction();
		UNSHARE_FOLDER_ACTION = new UnshareFolderAction();
    }

    /**
     * Set up the constants
     */
    @Override
    protected void setupConstants() {
		MAIN_PANEL = null;
		DATA_MODEL = new LibraryTableModel();
		TABLE = new LimeJTable(DATA_MODEL);
		DATA_MODEL.setTable(TABLE);
		Action[] aa = new Action[] { 
				LAUNCH_ACTION,
				ENQUEUE_ACTION,
				DELETE_ACTION,
				ANNOTATE_ACTION,
				PUBLISH_ACTION,
				RESUME_ACTION
		};
		
		BUTTON_ROW = new ButtonRow(aa, ButtonRow.X_AXIS, ButtonRow.NO_GLUE);
    }

    // inherit doc comment
    @Override
    protected JPopupMenu createPopupMenu() {
		if (TABLE.getSelectionModel().isSelectionEmpty())
			return null;
        
        JPopupMenu menu = new JPopupMenu();
        
		menu.add(new JMenuItem(LAUNCH_ACTION));
		menu.add(new JMenuItem(ENQUEUE_ACTION));
		menu.addSeparator();
		menu.add(new JMenuItem(RESUME_ACTION));
		menu.addSeparator();
		menu.add(new JMenuItem(DELETE_ACTION));
		menu.add(new JMenuItem(RENAME_ACTION));
		menu.addSeparator();
		
        int[] rows = TABLE.getSelectedRows();
		boolean dirSelected = false;
		boolean fileSelected = false;
		boolean torrentSelected = false;
		for (int i = 0; i < rows.length; i++) {
			File f = DATA_MODEL.get(rows[i]).getFile();
			if (f.isDirectory()) {
				dirSelected = true;
				if (IncompleteFileManager.isTorrentFolder(f))
					torrentSelected = true;
			} else
				fileSelected = true;
			
			if (dirSelected && fileSelected)
				break;
		}
		if (dirSelected) {
	        if (GUIMediator.isPlaylistVisible())
	            ENQUEUE_ACTION.setEnabled(false);
	        DELETE_ACTION.setEnabled(torrentSelected);
	        RENAME_ACTION.setEnabled(false);
			if (fileSelected) {
				JMenu sharingMenu = new JMenu(I18n.tr("Sharing"));
				sharingMenu.add(new JMenuItem(SHARE_ACTION));
				sharingMenu.add(new JMenuItem(UNSHARE_ACTION));
				sharingMenu.add(new JMenuItem(ANNOTATE_ACTION));
				sharingMenu.addSeparator();
				sharingMenu.add(new JMenuItem(SHARE_FOLDER_ACTION));
				sharingMenu.add(new JMenuItem(UNSHARE_FOLDER_ACTION));
				menu.add(sharingMenu);
			} else { 
				menu.add(new JMenuItem(SHARE_FOLDER_ACTION));
				menu.add(new JMenuItem(UNSHARE_FOLDER_ACTION));
			}
		} else {
	        if (GUIMediator.isPlaylistVisible() && PlaylistMediator.isPlayableFile(DATA_MODEL.getFile(rows[0])) )
	            ENQUEUE_ACTION.setEnabled(true);
	        DELETE_ACTION.setEnabled(true);
	        // only allow single selection for renames
	        RENAME_ACTION.setEnabled(LibraryMediator.isRenameEnabled() && rows.length == 1);
			menu.add(new JMenuItem(SHARE_ACTION));
			menu.add(new JMenuItem(UNSHARE_ACTION));
			menu.add(new JMenuItem(ANNOTATE_ACTION));
		}
		menu.addSeparator();
		
        LibraryTableDataLine line = DATA_MODEL.get(rows[0]);
        menu.add(createLicenseMenu(line));
		menu.add(createSearchSubMenu(line));
		menu.add(createAdvancedMenu(line));

		return menu;
    }

	private JMenu createLicenseMenu(LibraryTableDataLine dl) {
		JMenu menu = new JMenu(I18n.tr("License"));
		if (dl != null) {
			menu.add(new JMenuItem(PUBLISH_ACTION));
			menu.add(new JMenuItem(EDIT_LICENSE_ACTION));
			menu.add(new JMenuItem(VIEW_LICENSE_ACTION));
			
			menu.setEnabled(PUBLISH_ACTION.isEnabled() 
					|| EDIT_LICENSE_ACTION.isEnabled() 
					|| VIEW_LICENSE_ACTION.isEnabled()); 
		} else {
            menu.setEnabled(false);
		}

		return menu;
	}

	private JMenu createAdvancedMenu(LibraryTableDataLine dl) {
		JMenu menu = new JMenu(I18n.tr("Advanced"));
		if (dl != null) {
			menu.add(new JMenuItem(BITZI_LOOKUP_ACTION));
			menu.add(new JMenuItem(MAGNET_LOOKUP_ACTION));
			menu.add(new JMenuItem(COPY_MAGNET_TO_CLIPBOARD_ACTION));
			File file = getFile(TABLE.getSelectedRow());
			menu.setEnabled(GuiCoreMediator.getFileManager().getGnutellaFileList().contains(file));
		}
		
        if (menu.getItemCount() == 0)
            menu.setEnabled(false);

		return menu;
	}
	

	private JMenu createSearchSubMenu(LibraryTableDataLine dl) {
		JMenu menu = new JMenu(I18n.tr("Search"));
        
        if(dl != null) {
            File f = dl.getInitializeObject();
    		String keywords = QueryUtils.createQueryString(f.getName());
            if (keywords.length() > 2)
    			menu.add(new JMenuItem(new SearchAction(keywords)));
    		
    		LimeXMLDocument doc = dl.getXMLDocument();
    		if(doc != null) {
                Action[] actions = ActionUtils.createSearchActions(doc);
        		for (int i = 0; i < actions.length; i++)
        			menu.add(new JMenuItem(actions[i]));
            }
        }
        
        if(menu.getItemCount() == 0)
            menu.setEnabled(false);
            
        return menu;
	}

	/**
     * Upgrade getScrolledTablePane to public access.
     */
    @Override
    public JComponent getScrolledTablePane() {
        return super.getScrolledTablePane();
    }

    /* Don't display anything for this.  The LibraryMediator will do it. */
	@Override
    protected void updateSplashScreen() {}

    /**
     * Note: This is set up for this to work.
     * Polling is not needed though, because updates
     * already generate update events.
     */
    private LibraryTableMediator() {
        super("LIBRARY_TABLE");
        //GUIMediator.addRefreshListener(this);
        ThemeMediator.addThemeObserver(this);
    }
    
    /**
     * Sets up drag & drop for the table.
     */
    @Override
    protected void setupDragAndDrop() {
    	TABLE.setDragEnabled(true);
    	TABLE.setTransferHandler(new LibraryTableTransferHandler());
    }

	/**
	 * there is no actual component that holds all of this table.
	 * The LibraryMediator is real the holder.
	 */
	@Override
    public JComponent getComponent() {
		return null;
	}
	
    /**
     * Sets the default editors.
     */
    @Override
    protected void setDefaultEditors() {
        TableColumnModel model = TABLE.getColumnModel();
        TableColumn tc = model.getColumn(LibraryTableDataLine.NAME_IDX);
        tc.setCellEditor(new LibraryTableCellEditor(this));
    }


	/**
	 * Cancels all editing of fields in the tree and table.
	 */
	void cancelEditing() {
		if(TABLE.isEditing()) {
			TableCellEditor editor = TABLE.getCellEditor();
			editor.cancelCellEditing();
		}	    
	}

	/**
	 * Adds the mouse listeners to the wrapped <tt>JTable</tt>.
	 *
	 * @param listener the <tt>MouseInputListener</tt> that handles mouse events
	 *                 for the library
	 */
	void addMouseInputListener(final MouseInputListener listener) {
        TABLE.addMouseListener(listener);
        TABLE.addMouseMotionListener(listener);
	}

	/**
	 * Allows annotation once XML is set up
	 *
	 * @param enabled whether or not annotation is allowed
	 */
	public void setAnnotateEnabled(boolean enabled) {
		_annotateEnabled = enabled;
		
	    LibraryTableDataLine.setXMLEnabled(enabled);
	    DATA_MODEL.refresh();
		
	    handleSelection(-1);
	}

	/**
	 * Notification that the incomplete directory is selected (or not)
	 *
	 * @param enabled whether or not incomplete is showing
	 */
	void setIncompleteSelected(boolean enabled) {
		if (enabled == _isIncomplete)
			return;
	    _isIncomplete = enabled;
	    //  enable/disable the resume buttons if we're not incomplete
	    if (!enabled) {
			RESUME_ACTION.setEnabled(false);
	    } else if (!TABLE.getSelectionModel().isSelectionEmpty()) {
			RESUME_ACTION.setEnabled(true);
	    }
	}
	
	/**
	 * Updates the Table based on the selection of the given table.
	 * Perform lookups to remove any store files from the shared folder
     * view and to only display store files in the store view
	 */
    void updateTableFiles(DirectoryHolder dirHolder) {
		if (dirHolder == null)
			return;
		clearTable();
		setIncompleteSelected(LibraryMediator.incompleteDirectoryIsSelected());
		forceResort();
    }
	
	/**
	 * Handles events created by the FileManager.  Adds or removes rows from
	 * the table as necessary. 
	 */
//    void handleFileManagerEvent(final FileManagerEvent evt, DirectoryHolder holder) {
//		//  Need to update table only if one of the files in evt
//		//  is contained in the current directory.
//		if (evt == null || holder == null)
//			return;
//			
//        if(LOG.isDebugEnabled())
//            LOG.debug("Handling event: " + evt);
//        switch(evt.getType()) {
//        case REMOVE_FILE:
//            File f = evt.getNewFile();
//            if(holder.accept(f)) {
//                DATA_MODEL.reinitialize(f);
//                handleSelection(-1);
//            } else if(DATA_MODEL.contains(f)) {
//                DATA_MODEL.remove(f);
//                handleSelection(-1);
//            }
//            break;
//        case ADD_FILE:
//            if(holder.accept(evt.getNewFile())) {
//                add(evt.getNewFile());
//                handleSelection(-1);
//            }
//            break;
//        case CHANGE_FILE:
//            DATA_MODEL.reinitialize(evt.getOldFile());
//            handleSelection(-1);
//            break;
//        case RENAME_FILE:
//            File old = evt.getOldFile();
//            File now = evt.getNewFile();
//            if (holder.accept(now)) {
//                if(DATA_MODEL.contains(old)) {
//                    DATA_MODEL.reinitialize(old, now);
//                    handleSelection(-1);
//                } else {
//                    DATA_MODEL.add(now);
//                }
//            } else {
//                DATA_MODEL.remove(old);
//            }
//            break;
//        case ADD_FOLDER:
//			if (holder.accept(evt.getOldFile())) {
//			    add(evt.getOldFile());
//	            handleSelection(-1);
//			}
//			break;
//        case REMOVE_FOLDER:
//            f = evt.getNewFile();
//            if(holder.accept(f)) {
//                DATA_MODEL.reinitialize(f);
//                handleSelection(-1);
//            } else if(DATA_MODEL.contains(f)) {
//                DATA_MODEL.remove(f);
//                handleSelection(-1);
//            }
//            break;
//        }
//    }

	/**
	 * Returns the <tt>File</tt> stored at the specified row in the list.
	 *
	 * @param row the row of the desired <tt>File</tt> instance in the
	 *            list
	 *
	 * @return a <tt>File</tt> instance associated with the specified row
	 *         in the table
	 */
    File getFile(int row) {
		return DATA_MODEL.getFile(row);
    }
	
	/**
	 * Returns the file desc object for the given row or <code>null</code> if
	 * there is none.
	 * @param row
	 * @return
	 */
	private FileDesc getFileDesc(int row) {
		return DATA_MODEL.getFileDesc(row);
	}
	
	/**
	 * Implements the {@link FileDescProvider} interface by returning all the
	 * selected filedescs.
	 */
	public FileDetails[] getFileDetails() {
		int[] sel = TABLE.getSelectedRows();
		List<FileDetails> files = new ArrayList<FileDetails>(sel.length);
		for (int i = 0; i < sel.length; i++) {
			FileDesc desc = getFileDesc(sel[i]);
			if (desc != null) {
			    //DPINJ: Fix!
				files.add(GuiCoreMediator.getLocalFileDetailsFactory().create(desc));
			}
		}
		if (files.isEmpty()) {
			return new FileDetails[0];
		}
		return files.toArray(new FileDetails[0]);
	}
    
    /**
	 * Accessor for the table that this class wraps.
	 *
	 * @return The <tt>JTable</tt> instance used by the library.
	 */
    JTable getTable() {
        return TABLE;
    }

    ButtonRow getButtonRow() {
        return BUTTON_ROW;
    }
    
    LibraryTableDataLine[] getSelectedLibraryLines() {
    	int[] selected = TABLE.getSelectedRows();
        LibraryTableDataLine[] lines = new LibraryTableDataLine[selected.length];
        for(int i = 0; i < selected.length; i++)
            lines[i] = DATA_MODEL.get(selected[i]);
        return lines;
    }

    /**
     * Accessor for the <tt>ListSelectionModel</tt> for the wrapped
     * <tt>JTable</tt> instance.
     */
    ListSelectionModel getSelectionModel() {
            return TABLE.getSelectionModel();
    }
    
    /**
     * 
     */
    private boolean isSupportedFormat(FileDesc[] fds) {
        boolean audio = false;
        boolean video = false;
        boolean program = false;
        boolean document = false;
        boolean image = false;
        
        for(int i = 0; i < fds.length; i++) {
            String name = fds[i].getFileName();
            
            if (MediaType.getAudioMediaType().matches(name)
                    && !video && !program && !document && !image) {
                audio = true;
            } else if (MediaType.getVideoMediaType().matches(name)
                    && !audio && !program && !document && !image) {
                video = true;
            } else if (MediaType.getProgramMediaType().matches(name)
                    && !audio && !video && !document && !image) {
                program = true;
            } else if (MediaType.getDocumentMediaType().matches(name)
                    && !audio && !video && !program && !image) {
                document = true;
            } else if (MediaType.getImageMediaType().matches(name)
                    && !audio && !video && !program && !document) {
                image = true;
            } else {
                return false;
            }
        }
        
        return true;
    }

    /**
     * shows the user a meta-data for the file(if any) and allow the user
     * to edit it.
     * 
     * @param publish true to edit the license MetaData, false otherwise
     */
    void editMeta(boolean publish){        
        int[] rows = TABLE.getSelectedRows();
        List<FileDesc> fileDescs = new ArrayList<FileDesc>(rows.length);
        for(int i = 0; i < rows.length; i++) {
            FileDesc fd = DATA_MODEL.getFileDesc(rows[i]);
            if (fd != null) {
                fileDescs.add(fd);
            }
        }
        
        if (fileDescs.isEmpty()) {
            return;
        }

        FileDesc[] fds = fileDescs.toArray(new FileDesc[0]);
        String name = fds[0].getFile().getName();
        
        Frame mainFrame = GUIMediator.getAppFrame();
        if (isSupportedFormat(fds)) {
            try {
            	if (publish) {
                    FileDesc fd = fds[0];               
                    LimeXMLDocument doc = fd.getXMLDocument(LimeXMLNames.AUDIO_SCHEMA);
                    LimeXMLSchemaRepository rep = GuiCoreMediator.getLimeXMLSchemaRepository();
                    LimeXMLSchema schema = rep.getSchema(LimeXMLNames.AUDIO_SCHEMA);
                    if(schema == null)
                        throw new IllegalStateException("no audio schema!");
                    
            		CCPublishWizard wizard = new CCPublishWizard(fd, doc, schema, mainFrame);
            		wizard.launchWizard();
            	} else {
            		MetaEditor metaEditor = new MetaEditor(mainFrame, fds, name);
            		metaEditor.setLocationRelativeTo(mainFrame);
            		metaEditor.setVisible(true);
            	}
            	
            	return;
            } catch(IllegalStateException failed) {
                if(publish) {
                    GUIMediator.showError(I18n.tr("LimeWire cannot publish this file because it was unable to find a schema for audio files."));
                    return;
                }   
            }
        } else { // have the user choose a xml type for the file
        	XmlTypeEditor metaEditor = new XmlTypeEditor(mainFrame, fds, name);
        	metaEditor.setLocationRelativeTo(mainFrame);
            metaEditor.setVisible(true);
        }
    }
    
    /**
     * Programatically starts a rename of the selected item.
     */
    void startRename() {
        int row = TABLE.getSelectedRow();
        if(row == -1)
            return;
        int viewIdx = TABLE.convertColumnIndexToView(LibraryTableDataLine.NAME_IDX);
        TABLE.editCellAt(row, viewIdx, LibraryTableCellEditor.EVENT);
    }
    
    /**
     * Shows the license window.
     */
    void showLicenseWindow() {
        LibraryTableDataLine ldl = DATA_MODEL.get(TABLE.getSelectedRow());
        if(ldl == null)
            return;
        FileDesc fd = ldl.getFileDesc();
        License license = fd.getLicense();
        URN urn = fd.getSHA1Urn();
        LimeXMLDocument doc = ldl.getXMLDocument();
        LicenseWindow window = LicenseWindow.create(license, urn, doc, this);
        GUIUtils.centerOnScreen(window);
        window.setVisible(true);
    }
    

    public void licenseVerified(License license) {
        DATA_MODEL.refresh();
    }

    /**
     * Prepare a detail page of magnet link info for selected files 
     * in the library.
     */
    void doMagnetLookup() {
        doMagnetCommand("/magcmd/detail?");
    }
	
    /**
     * Fire a local lookup with file/magnet details
     */
    void doMagnetCommand(String cmd) {
        // get the selected files.  Build up a url to display details.
        int[] rows = TABLE.getSelectedRows();
        int k = rows.length;
        if(k == 0)
            return;

        boolean haveValidMagnet = false;

        int    count     = 0;
        int    port      = GuiCoreMediator.getLocalAcceptor().getPort();
        int    eport     = GuiCoreMediator.getAcceptor().getPort(true);
        byte[] eaddr     = GuiCoreMediator.getAcceptor().getAddress(true);
        String lookupUrl = "http://localhost:"+port+
          cmd+
          "addr="+NetworkUtils.ip2string(eaddr)+":"+eport;
        for(int i=0; i<k; i++) {
            FileDesc fd = DATA_MODEL.getFileDesc(rows[i]);
            if (fd==null) {
                // Only report valid files
                continue;
            }
            URN urn = fd.getSHA1Urn();
			if(urn == null) {
                // Only report valid sha1s
                continue;
            }
            String urnStr = urn.toString();
            int hashstart = 1 + urnStr.indexOf(":", 4);
             
            String sha1 = urnStr.substring(hashstart);
            lookupUrl +=
              "&n"+count+"="+EncodingUtils.encode(fd.getFileName())+
              "&u"+count+"="+sha1;
            count++;
            haveValidMagnet = true;
        }
        
        if (haveValidMagnet) {
            GUIMediator.openURL(lookupUrl);
        }
    }

    /**
     * Returns the options offered to the user when removing files.
     * 
     * Depending on the platform these can be a subset of 
     * MOVE_TO_TRASH, DELETE, CANCEL.
     */
    private static Object[] createRemoveOptions() {
        if (OSUtils.supportsTrash()) {
            String trashLabel = OSUtils.isWindows() ? I18n.tr("Move to Recycle Bin")
                    : I18n.tr("Move to Trash");
            return new Object[] {
                    trashLabel, 
                    I18n.tr("Delete"),
                    I18n.tr("Cancel") 
            };
        }
        else {
            return new Object[] {
                    I18n.tr("Delete"),
                    I18n.tr("Cancel") 
            };
        }
    }
    
    /**
     * Override the default removal so we can actually stop sharing
     * and delete the file.
	 * Deletes the selected rows in the table.
	 * CAUTION: THIS WILL DELETE THE FILE FROM THE DISK.
	 */
	@Override
    public void removeSelection() {
	    int[] rows = TABLE.getSelectedRows();
        if (rows.length == 0)
            return;

        if (TABLE.isEditing()) {
            TableCellEditor editor = TABLE.getCellEditor();
            editor.cancelCellEditing();
        }

        List<Tuple<File, FileDesc>> files = new ArrayList<Tuple<File, FileDesc>>(rows.length);
        
        // sort row indices and go backwards so list indices don't change when
        // removing the files from the model list
        Arrays.sort(rows);
        for (int i = rows.length - 1; i >= 0; i--) {
            File file = DATA_MODEL.getFile(rows[i]);
            FileDesc fd = DATA_MODEL.getFileDesc(rows[i]);
            files.add(new Tuple<File, FileDesc>(file, fd));
        }
        
        CheckBoxListPanel<Tuple<File, FileDesc>> listPanel =
            new CheckBoxListPanel<Tuple<File, FileDesc>>(files, new TupleTextProvider(), true);
        listPanel.getList().setVisibleRowCount(4);
        
        // display list of files that should be deleted
        Object[] message = new Object[] {
                new MultiLineLabel(I18n.tr("Are you sure you want to delete the selected file(s), thus removing it from your computer?"), 400),
                Box.createVerticalStrut(ButtonRow.BUTTON_SEP),
                listPanel,
                Box.createVerticalStrut(ButtonRow.BUTTON_SEP)
        };
        
        // get platform dependent options which are displayed as buttons in the dialog
        Object[] removeOptions = createRemoveOptions();
        
        int option = JOptionPane.showOptionDialog(MessageService.getParentComponent(),
                message,
                I18n.tr("Message"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                removeOptions,
                removeOptions[0] /* default option */);
        
        if (option == removeOptions.length - 1 /* "cancel" option index */ 
                || option == JOptionPane.CLOSED_OPTION) {
            return;
        }
        
        // remove still selected files
        List<Tuple<File, FileDesc>> selected = listPanel.getSelectedElements();
        List<String> undeletedFileNames = new ArrayList<String>();
        FileManager fileManager = GuiCoreMediator.getFileManager();
        IncompleteFileManager incompleteManager = GuiCoreMediator.getDownloadManager().getIncompleteFileManager();

        for (Tuple<File, FileDesc> tuple : selected) {
            File file = tuple.getFirst();
            FileDesc fd = tuple.getSecond();
            if (_isIncomplete && hasActiveDownloader(file)) {
                undeletedFileNames.add(getCompleteFileName(file));
                continue;
            }
            
            if(fd == null || fileManager.getIncompleteFileList().contains(fd))
                incompleteManager.removeEntry(file);
            else 
                fileManager.getManagedFileList().remove(file);
            
            if (fd != null) { 
                GuiCoreMediator.getUploadManager().killUploadsForFileDesc(fd);
            }
            GuiCoreMediator.getTorrentManager().killTorrentForFile(file);

            // removeOptions > 2 => OS offers trash options
            boolean removed = FileUtils.delete(file, removeOptions.length > 2 && option == 0 /* "move to trash" option index */);
            if (removed) {
                DATA_MODEL.remove(DATA_MODEL.getRow(file));
            }
            else {
                undeletedFileNames.add(getCompleteFileName(file));
            }
        }

        
		clearSelection();		
		
		if (undeletedFileNames.isEmpty()) {
			return;
		}
		
		// display list of files that could not be deleted
		message = new Object[] {
				new MultiLineLabel(I18n.tr("The following files could not be deleted. They may be in use by another application or are currently being downloaded to."), 400),
				Box.createVerticalStrut(ButtonRow.BUTTON_SEP),
				new JScrollPane(createFileList(undeletedFileNames))
		};
	
		JOptionPane.showMessageDialog(MessageService.getParentComponent(), 
				message,
				I18n.tr("Error"),
				JOptionPane.ERROR_MESSAGE);
    }
	
	/**
	 * Creates a JList of files and sets and makes it non-selectable. 
	 */
	private static JList createFileList(List<String> fileNames) {
	    JList fileList = new JList(fileNames.toArray());
        fileList.setVisibleRowCount(5);
        fileList.setCellRenderer(new FileNameListCellRenderer());
        fileList.setSelectionForeground(fileList.getForeground());
        fileList.setSelectionBackground(fileList.getBackground());
        fileList.setFocusable(false);
        return fileList;
	}
	
	/**
	 * Returns the human readable file name for incomplete files or
	 * just the regular file name otherwise. 
	 */
	private String getCompleteFileName(File file) {
		if (_isIncomplete) {
			try { 
				return IncompleteFileManager.getCompletedName(file);
			}
			catch (IllegalArgumentException iae) {
			}
		}
		return file.getName();
	}
	
	private boolean hasActiveDownloader(File incompleteFile) {
		return GuiCoreMediator.getDownloadManager().getDownloaderForIncompleteFile(incompleteFile) != null;
    }
    
	/**
	 * Handles a name change of one of the files displayed.
	 *
	 * @param newName The new name of the file
	 *
	 * @return A <tt>String</tt> that is the name of the file
	 *         after this method is called. This is the new name if
	 *         the name change succeeded, and the old name otherwise.
	 */
	String handleNameChange(String newName) {
		int row = TABLE.getEditingRow();
		LibraryTableModel ltm = DATA_MODEL;
		
		File oldFile = ltm.getFile(row);
		String parent = oldFile.getParent();
		String nameWithExtension = newName + "." + ltm.getType(row);
		File newFile = new File(parent, nameWithExtension);
        if (!ltm.getName(row).equals(newName)) {
            if (oldFile.renameTo(newFile)) {
//                GuiCoreMediator.getFileManager().fileRenamed(oldFile, newFile);
                // Ideally, renameFileIfShared should immediately send RENAME or REMOVE
                // callbacks. But, if it doesn't, it should atleast have immediately
                // internally removed the file from being shared. So, we immediately
                // do a reinitialize on the oldFile to mark it as being not shared.
                DATA_MODEL.reinitialize(oldFile);
                return newName;
            }

            // notify the user that renaming failed
            GUIMediator.showError(I18n.tr("Unable to rename the file \'{0}\'. It may be in use by another application.", ltm.getName(row)));
			return ltm.getName(row);
		}
		return newName; 
	}

    public void handleActionKey() {
        int[] rows = TABLE.getSelectedRows();
		LibraryTableModel ltm = DATA_MODEL;
		File file;
		for (int i = 0; i < rows.length; i++) {
			file = ltm.getFile(rows[i]);
			// if it's a directory try to select it in the library tree
			// if it could be selected return
			if (file.isDirectory() 
				&& LibraryMediator.setSelectedDirectory(file))
				return;
		}
		launch();
    }

    /**
     * Resume incomplete downloads
     */    
    void resumeIncomplete() {        
        //For each selected row...
        int[] rows = TABLE.getSelectedRows();
        boolean startedDownload=false;
        List<Exception> errors = new ArrayList<Exception>();
        for (int i=0; i<rows.length; i++) {
            //...try to download the incomplete
            File incomplete = DATA_MODEL.getFile(rows[i]);
            try {
                GuiCoreMediator.getDownloadServices().download(incomplete);
                startedDownload=true;
            } catch (SaveLocationException e) { 
                // we must cache errors to display later so we don't wait
                // while the table might change in the background.
                errors.add(e);
            } catch(CantResumeException e) {
                errors.add(e);
            }
        }
        
        // traverse back through the errors and show them.
        for(int i = 0; i < errors.size(); i++) {
            Exception e = errors.get(i);
            if(e instanceof SaveLocationException) {
				SaveLocationException sle = (SaveLocationException)e;
				if (sle.getErrorCode() == LocationCode.FILE_ALREADY_DOWNLOADING) {
					GUIMediator.showError(I18n.tr("You are already downloading this file to \"{0}\".", sle.getFile()),
					        QuestionsHandler.ALREADY_DOWNLOADING);
				}
				else {
					String msg = CoreExceptionHandler.getSaveLocationErrorString(sle);
					GUIMediator.showError(msg);
				}
            } else if ( e instanceof CantResumeException ) {
                GUIMediator.showError(I18n.tr("The file \"{0}\" is not a valid incomplete file and cannot be resumed.", 
                        ((CantResumeException)e).getFilename()),
                        QuestionsHandler.CANT_RESUME);
            }
        }       

        //Switch to download tab (if we actually started anything).
        if (startedDownload)
            switchToDownloadTab();
    }

    private void switchToDownloadTab() {
        GUIMediator.instance().setWindow(GUIMediator.Tabs.SEARCH);
    }

    /**
	 * Launches the associated applications for each selected file
	 * in the library if it can.
	 */
    void launch() {
    	int[] rows = TABLE.getSelectedRows();
        if (rows.length == 0) {
       	 return;
        }
    	LaunchableProvider[] providers = new LaunchableProvider[rows.length];
    	if (_isIncomplete) {
    		for (int i = 0; i < rows.length; i++) {
				providers[i] = new IncompleteProvider(DATA_MODEL.getFile(rows[i]));
			}
    	}
    	else {
    		for (int i = 0; i < rows.length; i++) {
				providers[i] = new CompleteProvider(DATA_MODEL.getFile(rows[i]));
			}
    	}
    	GUILauncher.launch(providers);
    }

	/**
	 * Handles the selection rows in the library window,
	 * enabling or disabling buttons and chat menu items depending on
	 * the values in the selected rows.
	 * 
	 * @param row the index of the first row that is selected
	 */
	public void handleSelection(int row) {
		int[] sel = TABLE.getSelectedRows();
		if (sel.length == 0) {
			handleNoSelection();
			return;
		} 
		
		LibraryTableDataLine selectedLine = DATA_MODEL.get(sel[0]);
		File selectedFile = getFile(sel[0]);
		boolean firstShared = GuiCoreMediator.getFileManager().getGnutellaFileList().contains(selectedFile);
		
		//  always turn on Launch, Delete, Magnet Lookup, Bitzi Lookup
		LAUNCH_ACTION.setEnabled(true);
		DELETE_ACTION.setEnabled(true);
		
		//  turn on Enqueue if play list is visible and a selected item is playable
		if (GUIMediator.isPlaylistVisible()) {
			boolean found = false;
			for (int i = 0; i < sel.length; i++)
	            if (PlaylistMediator.isPlayableFile(DATA_MODEL.getFile(sel[i]))) {
					found = true;
					break;
	            }
			ENQUEUE_ACTION.setEnabled(found);
        } else
			ENQUEUE_ACTION.setEnabled(false);

		//  turn on Describe for complete files
		//  turn on Publish / Edit for single selected complete files
		if (!_isIncomplete && _annotateEnabled) {
			ANNOTATE_ACTION.setEnabled(firstShared);
			
			boolean canPublish = (sel.length == 1 && firstShared && LimeXMLUtils.isFilePublishable(selectedFile.getName()));
			PUBLISH_ACTION.setEnabled(canPublish && !selectedLine.isLicensed());
			EDIT_LICENSE_ACTION.setEnabled(canPublish && selectedLine.isLicensed());
		} else {
			ANNOTATE_ACTION.setEnabled(false);
			PUBLISH_ACTION.setEnabled(false);
			EDIT_LICENSE_ACTION.setEnabled(false);
		}
		// only allow annotations if 1 line is selected
		ANNOTATE_ACTION.setEnabled(sel.length == 1);
		
		VIEW_LICENSE_ACTION.setEnabled(sel.length == 1 && selectedLine.isLicensed());
		
		//  turn on Resume button if Incomplete folder is currently selected
		RESUME_ACTION.setEnabled(_isIncomplete);
		
		RENAME_ACTION.setEnabled(LibraryMediator.isRenameEnabled() && sel.length == 1);
		 
		//  enable Share File action when any selected file is not shared
		boolean shareAllowed = false;
		boolean unshareAllowed = false;
		boolean shareFolderAllowed = false;
		boolean unshareFolderAllowed = false;
		boolean foundDir = false;
		for (int i = 0; i < sel.length; i++) {
			File file = getFile(sel[i]);
			if (file.isDirectory()) {
				
				// disable annotate action when a directory is part of the selection
				ANNOTATE_ACTION.setEnabled(false);
				
				// turn off launching for incomplete torrents
				boolean isTorrent = false;
				if (_isIncomplete) {
					isTorrent = IncompleteFileManager.isTorrentFolder(file);
					if (isTorrent)
						LAUNCH_ACTION.setEnabled(false); 
				}
				
				//  turn off delete (only once) if non-torrent directory found
				if (!foundDir && !isTorrent){
					DELETE_ACTION.setEnabled(false);
					foundDir = true;
				}
//				if (!GuiCoreMediator.getFileManager().isFolderShared(file))
					shareFolderAllowed = true;
//				else
					unshareFolderAllowed = true;
			} else {
				if (!GuiCoreMediator.getFileManager().getGnutellaFileList().contains(file)) {
					if (!LibraryUtils.isFileManagable(file) || _isIncomplete)
						continue;
					shareAllowed = true;
				} else {
					unshareAllowed = true;
				}
				
				if (shareAllowed && unshareAllowed && shareFolderAllowed && unshareFolderAllowed)
					break;
			}
		}
		SHARE_ACTION.setEnabled(shareAllowed);
		UNSHARE_ACTION.setEnabled(unshareAllowed);
		SHARE_FOLDER_ACTION.setEnabled(shareFolderAllowed);
		UNSHARE_FOLDER_ACTION.setEnabled(unshareFolderAllowed);
		
		//  enable / disable advanced items if file shared / not shared
		MAGNET_LOOKUP_ACTION.setEnabled(firstShared);
		BITZI_LOOKUP_ACTION.setEnabled(firstShared);

		COPY_MAGNET_TO_CLIPBOARD_ACTION.setEnabled(!_isIncomplete && getFileDesc(sel[0]) != null);
	}

	/**
	 * Handles the deselection of all rows in the library table,
	 * disabling all necessary buttons and menu items.
	 */
	public void handleNoSelection() {
		LAUNCH_ACTION.setEnabled(false);
		ENQUEUE_ACTION.setEnabled(false);
		DELETE_ACTION.setEnabled(false);
		ANNOTATE_ACTION.setEnabled(false);
		RESUME_ACTION.setEnabled(false);
		
		RENAME_ACTION.setEnabled(false);
		
		SHARE_ACTION.setEnabled(false);
		UNSHARE_ACTION.setEnabled(false);
		SHARE_FOLDER_ACTION.setEnabled(false);
		UNSHARE_FOLDER_ACTION.setEnabled(false);

		PUBLISH_ACTION.setEnabled(false);
		EDIT_LICENSE_ACTION.setEnabled(false);
		VIEW_LICENSE_ACTION.setEnabled(false);

		COPY_MAGNET_TO_CLIPBOARD_ACTION.setEnabled(false);
		MAGNET_LOOKUP_ACTION.setEnabled(false);
		BITZI_LOOKUP_ACTION.setEnabled(false);
	}

	/**
	 * Refreshes the enabledness of the Enqueue button based
	 * on the player enabling state. 
	 */
	public void setPlayerEnabled(boolean value) {
		handleSelection(TABLE.getSelectedRow());
	}

	public boolean setFileSelected(File file) {
	    int i = DATA_MODEL.getRow(file);
	    if (i != -1) {
	        TABLE.setSelectedRow(i);
	        TABLE.ensureSelectionVisible();
	        return true;
	    }
	    return false;
	}

    ///////////////////////////////////////////////////////
    //  ACTIONS
    ///////////////////////////////////////////////////////

    private final class LaunchAction extends AbstractAction {
		
		public LaunchAction () {
			putValue(Action.NAME, I18n.tr
					("Launch"));
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Launch Selected Files"));
			putValue(LimeAction.ICON_NAME, "LIBRARY_LAUNCH");
		}
		
        public void actionPerformed(ActionEvent ae) {
			launch();
        }
    }
	
    private final class EnqueueAction extends AbstractAction {
		
		public EnqueueAction () {
			putValue(Action.NAME, I18n.tr
					("Enqueue"));
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Add Selected Files to the Playlist"));
			putValue(LimeAction.ICON_NAME, "LIBRARY_TO_PLAYLIST");
		}
		
        public void actionPerformed(ActionEvent ae) {
			//get the selected file. If there are more than 1 we add all
			int[] rows = TABLE.getSelectedRows();
            List<File> files = new ArrayList<File>();
			for (int i = 0; i < rows.length; i++) {
				int index = rows[i]; // current index to add
				File file = DATA_MODEL.getFile(index);
				if (GUIMediator.isPlaylistVisible() && PlaylistMediator.isPlayableFile(file))
                    files.add(file);
			}
            LibraryMediator.instance().addFilesToPlayList(files);
        }
    }

    private final class RemoveAction extends AbstractAction {
		
		public RemoveAction () {
			putValue(Action.NAME, I18n.tr
					("Delete"));
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Delete Selected Files"));
			putValue(LimeAction.ICON_NAME, "LIBRARY_DELETE");
		}
		
        public void actionPerformed(ActionEvent ae) {
            REMOVE_LISTENER.actionPerformed(ae);
		}
    }
	
    private final class AnnotateAction extends AbstractAction {
		
		public AnnotateAction() {
			putValue(Action.NAME, 
					 I18n.tr("Describe..."));
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Add Description to Selected File"));
			putValue(LimeAction.ICON_NAME, "LIBRARY_ANNOTATE");
		}
		
    	public void actionPerformed(ActionEvent ae) {
    		editMeta(false);
    	}
    }
    
    private final class PublishAction extends AbstractAction {
    	
    	public PublishAction() {
			putValue(Action.NAME, 
					 I18n.tr("Publish..."));
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Publish under Creative Commons License"));
			putValue(LimeAction.ICON_NAME, "LIBRARY_PUBLISH");
    	}

		public void actionPerformed(ActionEvent e) {
			editMeta(true);
		}
    }

    private final class EditLicenseAction extends AbstractAction {
    	
    	public EditLicenseAction() {
			putValue(Action.NAME, 
					 I18n.tr("Edit License..."));
    	}

		public void actionPerformed(ActionEvent e) {
			editMeta(true);
		}
    }
    private final class ResumeAction extends AbstractAction {
		
		public ResumeAction () {
			putValue(Action.NAME, I18n.tr
					("Resume"));
			putValue(Action.SHORT_DESCRIPTION,
					 I18n.tr("Continue Downloading Selected Incomplete File"));			
			putValue(LimeAction.ICON_NAME, "LIBRARY_RESUME");
		}
		
        public void actionPerformed(ActionEvent ae) {
            resumeIncomplete();
		}
    }
	
    private final class RenameAction extends AbstractAction {
		
		public RenameAction () {
			putValue(Action.NAME, I18n.tr
					("Rename"));
			//  "LIBRARY_RENAME"   ???
			//  "LIBRARY_RENAME_BUTTON_TIP"   ???			
		}
		
        public void actionPerformed(ActionEvent ae) {
			startRename();
		}
    }

	private class ShareFileAction extends AbstractAction {
		
		public ShareFileAction() {
			putValue(Action.NAME, I18n.tr
					 ("Share File"));
		}
		
		public void actionPerformed(ActionEvent e) {
			int[] sel = TABLE.getSelectedRows();
			final File[] files = new File[sel.length];
			for (int i = 0; i < sel.length; i++) {
				files[i] = getFile(sel[i]);
			}
			
			BackgroundExecutorService.schedule(new Runnable() {
			    public void run() {
        			for (int i = 0; i < files.length; i++) {
						File file = files[i];
						if (file == null || file.isDirectory())
							continue;
						GuiCoreMediator.getFileManager().getGnutellaFileList().add(file);
        			}
                }
            });
		}
	}
	
	private class UnshareFileAction extends AbstractAction {
		
		public UnshareFileAction() {
			putValue(Action.NAME, I18n.tr
					 ("Stop Sharing File"));
		}
		
		public void actionPerformed(ActionEvent e) {
			int[] sel = TABLE.getSelectedRows();
			final File[] files = new File[sel.length];
			for (int i = sel.length - 1; i >= 0; i--) {
				files[i] = getFile(sel[i]);
			}
			
			BackgroundExecutorService.schedule(new Runnable() {
			    public void run() {
        			for (int i = 0; i < files.length; i++) {
						File file = files[i];
						if (file == null || file.isDirectory())
							continue;
						GuiCoreMediator.getFileManager().getGnutellaFileList().remove(file);
        			}
                }
            });
		}
	}
	
	private class ShareFolderAction extends AbstractAction {
		
		public ShareFolderAction() {
			putValue(Action.NAME, I18n.tr
					 ("Share Folder"));
		}
		
		public void actionPerformed(ActionEvent e) {
			int[] sel = TABLE.getSelectedRows();
			List<File> files = new ArrayList<File>(sel.length);
			for (int i = 0; i < sel.length; i++) {
				File file = getFile(sel[i]);
				if (file != null && file.isDirectory()) {
					files.add(file);
				}
			}

			final RecursiveSharingDialog dialog = 
				new RecursiveSharingDialog(GUIMediator.getAppFrame(), files.toArray(new File[0]));
			if (dialog.showChooseDialog(MessageService.getParentComponent()) == State.OK) {
//				BackgroundExecutorService.schedule(new Runnable() {
//				    public void run() {
//				        GuiCoreMediator.getFileManager().addSharedFolders(dialog.getRootsToShare(),
//				        		dialog.getFoldersToExclude());
//		            }
//		        });
			}
		}
	}
	
	private class UnshareFolderAction extends AbstractAction {
		
		public UnshareFolderAction() {
			putValue(Action.NAME, I18n.tr
					 ("Stop Sharing Folder"));
		}
		
		public void actionPerformed(ActionEvent e) {
			int[] sel = TABLE.getSelectedRows();
			final File[] files = new File[sel.length];
			for (int i = sel.length - 1; i >= 0; i--) {
				files[i] = getFile(sel[i]);
			}
			
			BackgroundExecutorService.schedule(new Runnable() {
			    public void run() {
        			for (int i = 0; i < files.length; i++) {
						File file = files[i];
						if (file == null || !file.isDirectory())
							continue;
//        				GuiCoreMediator.getFileManager().removeSharedFolder(file);
        			}
                }
            });
		}
	}
	
	private final class MagnetLookupAction extends AbstractAction {
		
		public MagnetLookupAction() {
			putValue(Action.NAME, I18n.tr
					("Show Magnet Details"));
		}
		
        public void actionPerformed(ActionEvent e) {
            doMagnetLookup();
        }
    }

	private class ViewLicenseAction extends AbstractAction {

		public ViewLicenseAction() {
			putValue(Action.NAME, I18n.tr
					("View License"));
		}
		
		public void actionPerformed(ActionEvent e) {
			showLicenseWindow();
		}
	}
	
	/**
	 * Sets an icon based on the filename extension. 
	 */
	private static class FileNameListCellRenderer extends DefaultListCellRenderer {
		
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected,
					cellHasFocus);
			String extension = FileUtils.getFileExtension(value.toString());
			if (!extension.isEmpty()) {
				setIcon(IconManager.instance().getIconForExtension(extension));
			}
			return this;
		}
	}
	
	
	
	/**
	 * Renders the file part of the Tuple<File, FileDesc> in CheckBoxList<Tuple<File, FileDesc>>.
	 */
	private class TupleTextProvider implements CheckBoxList.TextProvider<Tuple<File, FileDesc>> {
            
        public Icon getIcon(Tuple<File, FileDesc> obj) {
            String extension = FileUtils.getFileExtension(obj.getFirst());
            if (!extension.isEmpty()) {
                return IconManager.instance().getIconForExtension(extension);
            }
            return null;
        }
        public String getText(Tuple<File, FileDesc> obj) {
            return getCompleteFileName(obj.getFirst());
        }
        
        public String getToolTipText(Tuple<File, FileDesc> obj) {
            return obj.getFirst().getAbsolutePath();
        }
        
    }
	
	private static class IncompleteProvider implements LaunchableProvider {

		private final File incompleteFile;
		
		public IncompleteProvider(File incompleteFile) {
			this.incompleteFile = incompleteFile;
		}
		
		public Downloader getDownloader() {
			return GuiCoreMediator.getDownloadManager().getDownloaderForIncompleteFile(incompleteFile);
		}

		public File getFile() {
			return incompleteFile;
		}
	}
	
	private static class CompleteProvider extends IncompleteProvider {
		
		public CompleteProvider(File file) {
			super(file);
		}
		
		@Override
        public Downloader getDownloader() {
			return null;
		}
	}
}
