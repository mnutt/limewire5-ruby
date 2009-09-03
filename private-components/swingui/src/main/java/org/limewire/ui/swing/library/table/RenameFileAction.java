package org.limewire.ui.swing.library.table;

import java.awt.Dimension;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Handles renaming a file. Creates a popup dialog that allows the
 * user to edit the filename.
 */
public class RenameFileAction extends AbstractAction {

    private final Provider<LibraryManager> libraryManager;
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
    private JDialog dialog;
    
    @Inject
    public RenameFileAction(Provider<LibraryManager> libraryManager,
            @LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems) {
        super(I18n.tr("Rename"));
        
        this.libraryManager = libraryManager;
        this.selectedLocalFileItems = selectedLocalFileItems;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        dialog = new LimeJDialog(GuiUtils.getMainFrame(), I18n.tr("Rename File"), ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(new FileRenamePanel());
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(GuiUtils.getMainFrame());
        dialog.setVisible(true);
    }
    
    /**
     * Closes the popup Dialog and disposes of any instance.
     */
    private void closeDialog() {
        if(dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
            dialog = null;
        }
    }
    
    /**
     * Notifies the library that the fileName has been changed.
     */
    private void updateFileNameInLibrary(File oldFile, File newFile) {
        libraryManager.get().getLibraryManagedList().fileRenamed(oldFile, newFile);
    }
    
    /**
     * Returns true if the text is a valid file name.
     */
    private boolean isValidFileName(String fileName) {
        return fileName != null && fileName.length() > 0;
    }
    
    private class FileRenamePanel extends JPanel {
        
        private final JTextField fileNameTextfield;
        private final JButton renameButton;
        private final JButton cancelButton;
        
        public FileRenamePanel() {
            super(new MigLayout("fill, gapy 10"));
            
            setMinimumSize(new Dimension(400, 300));
            
            fileNameTextfield = new JTextField(selectedLocalFileItems.get().get(0).getName());
            renameButton = new JButton(new RenameAction(fileNameTextfield));
            cancelButton = new JButton(new CancelAction());
            
            add(fileNameTextfield, "span 2, growx, wrap");
            add(renameButton, "skip 1, gapleft 130, split, alignx right");
            add(cancelButton, "gapleft 15, alignx right");
        }
    }
    
    /**
     * Closes the dialog and saves the fileName both to disk and updates
     * the fileName in the Library.
     */
    private class RenameAction extends AbstractAction {
        private final JTextField textField;
        
        public RenameAction(JTextField textField) {
            super(I18n.tr("Rename"));
            
            this.textField = textField;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String newFileName = textField.getText().trim();
            LocalFileItem oldFileItem = selectedLocalFileItems.get().get(0);
            // check the new file name is valid
            if(!isValidFileName(newFileName)) {
                textField.setText(oldFileItem.getName());
                return;
            }
            
            // check if the name hasn't changed, just return
            if(newFileName.equals(oldFileItem.getName())) {
                closeDialog();
                return;
            }

            File oldFile = oldFileItem.getFile();
            
            newFileName = newFileName + "." + FileUtils.getFileExtension(oldFile);
            File newFile = new File(oldFile.getParentFile(), newFileName);
            
            // try performing the file rename, if something goes wrong, revert textfield.
            if(FileUtils.forceRename(oldFile, newFile)) {
                updateFileNameInLibrary(oldFile, newFile);
                closeDialog();
            } else {
                textField.setText(oldFileItem.getName());
            }
        }
    }

    /**
     * Closes the dialog without changing the fileName.
     */
    private class CancelAction extends AbstractAction {
        public CancelAction() {
            super(I18n.tr("Cancel"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            closeDialog();
        }
    }
}
