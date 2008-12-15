package com.limegroup.gnutella.gui.search;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.URLLabel;
import com.limegroup.gnutella.gui.actions.AbstractAction;
import com.limegroup.gnutella.gui.actions.LimeAction;
import com.limegroup.gnutella.gui.options.OptionsConstructor;
import com.limegroup.gnutella.gui.options.OptionsMediator;
import com.limegroup.gnutella.gui.tables.DataLine;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileList;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.LibraryUtils;

/**
 * Shows the files being shared by the users LimeWire in a search results like window
 * 
 */
public class MySharedFilesResultPanel extends ResultPanel {
    

    protected static final String MY_SHARED_FILES_TABLE = "MY_SHARED_FILES_TABLE";
    
    private JLabel filesLabel;

//	private final EventListener<FileManagerEvent> listener;
    
    
    
    /**
     *  Constructs a result panel showing your shared files. 
     */
    MySharedFilesResultPanel(String title, FileManager fileManager) {
        super(title, MY_SHARED_FILES_TABLE);

        this.TABLE.setDragEnabled(false);
        this.TABLE.setTransferHandler(null);
        // this.TABLE.setEnabled(false);
        BUTTON_ROW.setButtonsEnabled(false);

        SOUTH_PANEL.setVisible(false);
        
        FileList fileList = fileManager.getGnutellaFileList();
        fileList.getReadLock().lock();
        try {
            for(FileDesc fd : fileList) {
                if (!shouldDisplayAddedFile(fd))
                    continue;
                                
                addFile(fd);
            }
        } finally {
            fileList.getReadLock().unlock();
        }
        
//        listener = createUpdateListener();
    }

    public Action getUnshareAction(int num) {
        return new UnshareFileAction(num);
    }

    /**
     * @return false if this is an incomplete file and should not be added.
     */
    private boolean shouldDisplayAddedFile(FileDesc fd) {
        if(GuiCoreMediator.getFileManager().getIncompleteFileList().contains(fd))
            return false;
        
        if (LibraryUtils.isForcedShare(fd)) {
            return false;
        }
        
        return true;
        
    }
    
    private void addFile(FileDesc fd) {
        add(new SharedSearchResult(fd, GuiCoreMediator.getCreationTimeCache(),
                GuiCoreMediator.getNetworkManager()));
    }
    
//    private void removeFile(FileDesc fd) {
//        for ( int i=0 ; i<DATA_MODEL.getRowCount() ; i++ ) {
//            DataLine<SearchResult> line =  getLine(i);
//            SearchResult sr = line.getInitializeObject();
//
//            if (((SharedSearchResult) sr).getFileDesc().equals(fd)) {
//                this.remove(sr);
//            }
//        }
//    }
    
    private class UnshareFileAction extends AbstractAction {

        public UnshareFileAction(int num) {
            putValue(Action.NAME, I18n.trn("Stop Sharing File", "Stop Sharing Files", num));
        }

        public void actionPerformed(ActionEvent e) {
            final int[] sel = TABLE.getSelectedRows();
            final FileDesc[] files = new FileDesc[sel.length];

            for (int i = 0; i < sel.length; i++) {
                DataLine<SearchResult> line =  getLine(sel[i]);
                SearchResult sr = line.getInitializeObject();

                FileDesc fd = ((SharedSearchResult) sr).getFileDesc();
                files[i] = fd;
            }

            BackgroundExecutorService.schedule(new Runnable() {
                public void run() {
                    for (FileDesc fd : files) {
                        GuiCoreMediator.getFileManager().getGnutellaFileList().remove(fd);
                    }      
                }
            });
            
            // remove rows from the model seperately from the FM
            //  the FM uses synchronization for removals and can cause 
            //  unnessary repaints in the table when multiple rows are
            //  removed at the same time
            for (int i = files.length-1; i >= 0 ; i--) {
                DATA_MODEL.remove(sel[i]);
            }      
            refreshNumFiles();
        }
    }    
    
    /**
     * Creates the specialized SearchResultMenu for right-click popups.
     *
     * Upgraded access from protected to public for SearchResultDisplayer.
     */
    @Override
    public JPopupMenu createPopupMenu() {
        TableLine[] lines = getAllSelectedLines();
        if(lines.length == 0)
            return null;
        return (new SearchResultMenu(this)).addToMenu(new JPopupMenu(), lines, true, false);
    }
    
    /**
     * Setup the data model 
     */
    @Override
    protected void setupDataModel() {
        DATA_MODEL = new NoGroupTableRowFilter(FILTER);
    }
    /**
     * Sets the appropriate buttons to be disabled.
     */
    @Override
    public void handleNoSelection() {
       BUTTON_ROW.setButtonsEnabled(false);
    }
    
    /**
     * Sets the appropriate buttons to be enabled.
     */
    @Override
    public void handleSelection(int i)  { 
        BUTTON_ROW.setButtonsEnabled(false);
    }
    
    @Override
    public void handleActionKey() {
    }
    

    
    @Override
    protected void setupMainPanel() {
        MAIN_PANEL.add(createMyFilesInfoPanel());
                
        setupMainPanelBase();
    }

    private void refreshNumFiles() {
        String info = I18n.tr(
                "You are sharing {0} files. You can control which files LimeWire shares.",
                GuiCoreMediator.getFileManager().getGnutellaFileList().size());

        filesLabel.setText("<html><font color=\"#7B5100\"><b>" + info + "</b></font></html>");
        
        
    }
    
    private JComponent createMyFilesInfoPanel() {
        JPanel panel = createWarningDitherPanel();

        
        String configure = I18n.tr("Configure");
        String library = I18n.tr("Library");
        
        panel.setLayout((new FlowLayout(FlowLayout.LEFT, 3, 3)));
        panel.add(new JLabel(GUIMediator.getThemeImage("warn-triangle")));
        
        filesLabel = new JLabel();

        refreshNumFiles();
        
        panel.add(filesLabel);
        
        panel.add(Box.createHorizontalStrut(2));
        Action configureAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                OptionsMediator optionsMediator = OptionsMediator.instance();
                optionsMediator.setOptionsVisible(true, OptionsConstructor.SHARED_KEY);
            }
        };
        configureAction.putValue(Action.NAME, "<b>" + configure + "</b>");
        configureAction.putValue(LimeAction.COLOR, new Color(0xAC,0x71,0x00));
        panel.add(new URLLabel(configureAction));
        
        panel.add(Box.createHorizontalStrut(2));
        Action libraryAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                GUIMediator.instance().getMainFrame().setSelectedTab(GUIMediator.Tabs.LIBRARY);
            }
        };
        libraryAction.putValue(Action.NAME, "<b>" + library + "</b>");
        libraryAction.putValue(LimeAction.COLOR, new Color(0xAC,0x71,0x00));
     
        panel.add(new URLLabel(libraryAction));

        Dimension ps = panel.getPreferredSize();
        ps.width = Short.MAX_VALUE;
        panel.setMaximumSize(ps);

        return panel;
    }

    @Override
    public void cleanup() {
//        GuiCoreMediator.getFileManager().removeFileEventListener(listener);
    }
    
//    private EventListener<FileManagerEvent> createUpdateListener() {
//        
//        EventListener<FileManagerEvent> listener = new EventListener<FileManagerEvent>() {
//            public void handleEvent(final FileManagerEvent evt) {
//                switch (evt.getType()) {
//                    case ADD_FILE:
//                    case REMOVE_FILE:
//                        GUIMediator.safeInvokeLater(new Runnable() {
//                            public void run() {
//                                if (evt.getType() == FileManagerEvent.Type.ADD_FILE) {
//                                    if (shouldDisplayAddedFile(evt.getNewFileDesc()) &&
//                                            evt.getFileManager().getGnutellaSharedFileList().contains(evt.getNewFileDesc()))
//                                        addFile(evt.getNewFileDesc());     
//                                } else {
//                                    removeFile(evt.getNewFileDesc());
//                                }
//                               
//                                refreshNumFiles();
//                            }
//                        });
//                    }
//                }
//        };
//        
//        GuiCoreMediator.getFileManager().addFileEventListener(listener);
//        
//        return listener;
//    }
}
