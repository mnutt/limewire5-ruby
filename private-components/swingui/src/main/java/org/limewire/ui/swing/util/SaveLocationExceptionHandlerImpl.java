package org.limewire.ui.swing.util;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * A universal handler for SaveLocationException messages generated while
 * performing downloads.
 */
@Singleton
public class SaveLocationExceptionHandlerImpl implements SaveLocationExceptionHandler {
    private static final Log LOG = LogFactory.getLog(SaveLocationExceptionHandlerImpl.class);

    private final SaveLocationManager saveLocationManager;

    /**
     * Constructs a SaveLocationExceptionHandler with the specified
     * SaveLocationManager.
     */
    @Inject
    public SaveLocationExceptionHandlerImpl(SaveLocationManager saveLocationManager) {
        this.saveLocationManager = saveLocationManager;
    }

    /**
     * Handles the supplied SaveLocationException. The method may take one of
     * several actions: eat the exception, try downloading again using the
     * supplied <code>downloadAction</code>, or popup a dialog to try and save
     * the download in a new location.
     */
    public void handleSaveLocationException(final DownloadAction downLoadAction,
            final SaveLocationException sle, final boolean supportNewSaveDir) {

        // Create Runnable to execute task on UI thread. This is necessary
        // if the handler method has been invoked from a background thread.
        SwingUtils.invokeLater(new Runnable() {
            public void run() {
                handleException(downLoadAction, sle, supportNewSaveDir);
            }
        });
    }

    /**
     * Handles the specified SaveLocationException. The method may prompt the
     * user for input, and should be executed from the UI thread.
     */
    private void handleException(final DownloadAction downLoadAction,
            final SaveLocationException sle, final boolean supportNewSaveDir) {
        // TODO get rid of supportNewSaveDir variable, it looks like bit torrent
        // can support changing the file name if need be so there should be no need for it anymore

        if (sle.getErrorCode() == SaveLocationException.LocationCode.FILE_ALREADY_DOWNLOADING) {
            // ignore, just return because we are already downloading this file
            return;
        }

        //check to make sure this is a SaveLocationException we can handle
        if ((sle.getErrorCode() != SaveLocationException.LocationCode.FILE_ALREADY_EXISTS)
                && (sle.getErrorCode() != SaveLocationException.LocationCode.FILE_IS_ALREADY_DOWNLOADED_TO)) {
            // Create user message.
            showErrorMessage(sle);
            return;
        } else if(sle.getErrorCode() == SaveLocationException.LocationCode.FILE_IS_ALREADY_DOWNLOADED_TO && !supportNewSaveDir) {
            //prevents infinite loop case where for bit torrent files we can't change the save file at the moment
            showErrorMessage(sle);
            return;
        }

        //select a save file name
        File saveFile = null;
        if (supportNewSaveDir && SwingUiSettings.AUTO_RENAME_DUPLICATE_FILES.getValue()) {
            saveFile = getAutoSaveFile(sle);
        } else {
            if (supportNewSaveDir) {
                saveFile = FileChooser.getSaveAsFile(GuiUtils.getMainFrame(), I18n
                        .tr("Save File As..."), sle.getFile());
            } else {
                saveFile = sle.getFile();
            }
            
            if (saveFile == null) {
                return;
            }
        }
        
        //if save file already exists, prompt to overwrite
        //otherwise download using the supplied download action
        if (saveFile.exists()) {
            createOverwriteDialogue(saveFile, downLoadAction, sle, supportNewSaveDir);
        } else {
            download(downLoadAction, supportNewSaveDir, saveFile, false);
        }
    }

    private void showErrorMessage(final SaveLocationException sle) {
        String message = I18n.tr("Unable to download: {0}\nfile {1}", sle.getErrorCode(), sle
                .getFile());

        // Log exception and display user message.
        LOG.error(message, sle);
        FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), message, I18n
                .tr("Download"), JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Iterates through possible file names until an available one is found and
     * is returned.
     */
    private File getAutoSaveFile(final SaveLocationException sle) {
        File saveFile;
        saveFile = sle.getFile();
        int index = 1;
        String fileName = FileUtils.getFilenameNoExtension(saveFile.getName());
        String extension = FileUtils.getFileExtension(saveFile);
        while (saveFile.exists() || saveLocationManager.isSaveLocationTaken(saveFile)) {
            String newFileName = fileName + "(" + index + ")";
            if (extension.length() > 0) {
                newFileName += "." + extension;
            }
            saveFile = new File(saveFile.getParentFile(), newFileName);
            index++;
        }
        return saveFile;
    }

    /**
     * Downloads the given file using the supplied download action. And handles
     * any possible SaveLocationExceptions.
     */
    private void download(final DownloadAction downLoadAction, final boolean supportNewSaveDir,
            File saveFile, boolean overwrite) {
        try {
            downLoadAction.download(saveFile, overwrite);
        } catch (SaveLocationException e1) {
            handleSaveLocationException(downLoadAction, e1, supportNewSaveDir);
        }
    }

    private void createOverwriteDialogue(final File saveFile, final DownloadAction downLoadAction,
            final SaveLocationException sle, final boolean supportNewSaveDir) {

        final JDialog dialog = new LimeJDialog();
        dialog.setModalityType(ModalityType.APPLICATION_MODAL);

        final MultiLineLabel message = new MultiLineLabel(I18n
                .tr("File already exists. What do you want to do?"), 400);

        final JTextField filePathField = new JTextField(25);
        filePathField.setEnabled(false);
        filePathField.setText(saveFile.getAbsolutePath());

        JToggleButton overwriteButton = null;
        overwriteButton = new JToggleButton(I18n.tr("Overwrite"));
        overwriteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dialog.dispose();
                download(downLoadAction, supportNewSaveDir, saveFile, true);
            }
        });

        JButton cancelButton = new JButton(I18n.tr("Cancel"));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
                if (supportNewSaveDir) {
                    handleSaveLocationException(downLoadAction, sle, supportNewSaveDir);
                }
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("hidemode 3, gapy 10", "", ""));
        panel.add(message, "span 2, wrap");
        panel.add(filePathField, "grow x, push, wrap");
        panel.add(overwriteButton, "alignx right");
        panel.add(cancelButton);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(GuiUtils.getMainFrame());
        dialog.setVisible(true);
    }
}
