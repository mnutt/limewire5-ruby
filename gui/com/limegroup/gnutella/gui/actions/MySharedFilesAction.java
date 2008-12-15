package com.limegroup.gnutella.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.search.SearchMediator;

/**
 * Displays the My Shared Files window when invoked.
 */
public class MySharedFilesAction extends AbstractAction {
    
    /**
     * Creates an action that links to My Shared Files with given text.
     */
    public MySharedFilesAction(String title) {
        super(title);
    }
    
    /**
     * Creates an action that links to My Shared Files with text that reflects the number
     *  of files in a users library by registering a FileManager listener.
     */
    public MySharedFilesAction() {
        super(createTitle());
        
        // TODO: change to polling
//        GuiCoreMediator.getFileManager().addFileEventListener(new EventListener<FileManagerEvent>() {
//            public void handleEvent(FileManagerEvent evt) {
//                switch (evt.getType()) {
//                    case ADD_FILE:
//                    if(!evt.getFileManager().getGnutellaSharedFileList().contains(evt.getNewFileDesc()) &&
//                            !evt.getFileManager().getIncompleteFileList().contains(evt.getNewFileDesc()))
//                        break;
//                    case REMOVE_FILE:
//                        GUIMediator.safeInvokeLater(new Runnable() {
//                            public void run() {
//                                putValue(Action.NAME, createTitle());
//                            }
//                        });
//                        break;
//                }
//            }
//        });
    }

    public void actionPerformed(ActionEvent e) {
        SearchMediator.showMyFiles(I18n.tr("My Shared Files"));
    }

    private static String createTitle() {
        return I18n.tr("View My {0} Shared Files", GuiCoreMediator.getFileManager().getGnutellaFileList().size());
    }
}
