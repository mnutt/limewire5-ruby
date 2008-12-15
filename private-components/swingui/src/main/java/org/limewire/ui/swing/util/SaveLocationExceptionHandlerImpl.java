package org.limewire.ui.swing.util;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.util.FileUtils;
import org.limewire.util.NotImplementedException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SaveLocationExceptionHandlerImpl implements SaveLocationExceptionHandler {

    private final SaveLocationManager saveLocationManager;
    
    @Inject
    public SaveLocationExceptionHandlerImpl(SaveLocationManager saveLocationManager) {
        this.saveLocationManager = saveLocationManager;
    }
    
    public void handleSaveLocationException(final DownloadAction downLoadAction,
            final SaveLocationException sle, final boolean supportNewSaveDir) {

        if (sle.getErrorCode() == SaveLocationException.LocationCode.FILE_ALREADY_DOWNLOADING) {
            // ignore, just return
            return;
        }

        if (sle.getErrorCode() != SaveLocationException.LocationCode.FILE_ALREADY_EXISTS
                && sle.getErrorCode() != SaveLocationException.LocationCode.FILE_IS_ALREADY_DOWNLOADED_TO) {
            // TODO better user feedback
            throw new NotImplementedException(sle);
        }

        File saveFile = null;

        if (supportNewSaveDir && DownloadSettings.AUTO_RENAME_DUPLICATE_FILES.getValue()) {
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
        } else {
            if (supportNewSaveDir) {
                saveFile = FileChooser.getSaveAsFile(GuiUtils.getMainFrame(), I18n.tr("Save File As..."), sle
                        .getFile());
            } else {
                saveFile = sle.getFile();
            }

            if (saveFile == null) {
                return;
            }
        }

        if (saveFile.exists()) {
            createOverwriteDialogue(saveFile, downLoadAction, sle, supportNewSaveDir);
        } else {
            download(downLoadAction, supportNewSaveDir, saveFile, false);
        }
    }

    private void download(final DownloadAction downLoadAction, final boolean supportNewSaveDir, File saveFile, boolean overwrite) {
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
