package com.limegroup.gnutella.gui.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.Action;

import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.FileChooserHandler;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MessageService;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;

/**
 * Opens a file chooser dialog centered on {@link
 * MessageService#getParentComponent()} and adds the selected file to the
 * specially shared files if it is not being shared already.
 */
public class ShareFileSpeciallyAction extends AbstractAction {

    public ShareFileSpeciallyAction() {
        putValue(Action.NAME, I18n.tr("Share New File..."));
        putValue(Action.SHORT_DESCRIPTION, "Opens a Dialog and Lets You Choose a File to Share");
    }

    public void actionPerformed(ActionEvent e) {
        final List<File> toShare = FileChooserHandler.getMultiInputFile(MessageService
                .getParentComponent(), I18nMarker.marktr("Share New File..."), I18nMarker
                .marktr("Share"), null);
        if (toShare != null) {
            BackgroundExecutorService.schedule(new Runnable() {
                public void run() {
                    for (File f : toShare) {
//                        GuiCoreMediator.getFileManager().addFileEventListener(new Listener(f));
                        GuiCoreMediator.getFileManager().getGnutellaFileList().add(f);
                    }
                }
            });
        }
    }

//    private static class Listener implements EventListener<FileManagerEvent> {
//
//        private final File listenForFile;
//
//        public Listener(File file) {
//            this.listenForFile = file;
//        }
//
//        public void handleEvent(final FileManagerEvent fev) {
//            // only act on events regarding this.listenForFile
//            if (fev.getNewFile() == null || fev.getNewFile().equals(listenForFile))
//                return;
//
//            GUIMediator.safeInvokeLater(new Runnable() {
//                public void run() {
//                    switch (fev.getType()) {
//                    case FILE_ALREADY_ADDED:
//                        GUIMediator.showError(I18n.tr("The file \"{0}\" is already shared.", fev.getNewFile()));
//                        break;
//                    case ADD_FAILED_FILE:
//                        GUIMediator.showError(I18n
//                                .tr("LimeWire was unable to share the file \"{0}\".", fev.getNewFile()));
//                        break;
//                    }
//                }
//            });
//            //remove this listener once we heard back about our file
//            fev.getSource().removeFileEventListener(this);
//        }
//    }

}
