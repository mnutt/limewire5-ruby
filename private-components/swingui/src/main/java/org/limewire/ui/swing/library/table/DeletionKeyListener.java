package org.limewire.ui.swing.library.table;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class DeletionKeyListener implements KeyListener {
    
    private final LibraryManager libraryManager;
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;    
    
    @Inject
    public DeletionKeyListener(LibraryManager libraryManager, 
            @LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems) {
        this.libraryManager = libraryManager;
        this.selectedLocalFileItems = selectedLocalFileItems;
    }
    
    @Override
    public void keyPressed(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_DELETE) {

            List<LocalFileItem> selectedItems = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
                                
            String title = null;
            String message = null;
            String deleteText = null;
            String removeText = I18n.tr("Remove from Library");
            String cancelText = I18n.tr("Cancel");
            
            if (OSUtils.isWindows() && OSUtils.supportsTrash()) {
                title = I18n.trn("Move File to the Recycle Bin or Remove from Library", "Move Files to the Recycle Bin or Remove from Library",
                        selectedItems.size());
                message = I18n.trn("Do you want to move this file to the Recycle Bin or just remove it from the Library?", 
                        "Do you want to move this file to the Recycle Bin or just remove it from the Library?", selectedItems.size());
                deleteText = I18n.tr("Move to Recycle Bin");
            }
            else if (OSUtils.isMacOSX() && OSUtils.supportsTrash()) {
                title = I18n.trn("Move File to the Trash or Remove from Library", "Move Files to the Trash or Remove from Library",
                        selectedItems.size());
                message = I18n.trn("Do you want to move this file to the Trash or just remove it from the Library?", 
                        "Do you want to move this file to the Trash or just remove it from the Library?", selectedItems.size());
                deleteText = I18n.tr("Move to Trash");
            }
            else {
                title = I18n.trn("Delete File or Remove from Library", "Delete Files or Remove from Library",
                        selectedItems.size());
                message = I18n.trn("Do you want to delete this file from disk or just remove it from the Library?",
                        "Do you want to delete this file from disk or just remove it from the Library?", selectedItems.size());
                deleteText = I18n.tr("Delete from Disk");
            }
            
            Object[] options = new Object[] {removeText, deleteText, cancelText};
            
            int confirmation = FocusJOptionPane.showOptionDialog(null, 
                    message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                    options, cancelText);
            
            if (confirmation < 0 || options[confirmation] == cancelText) {
                return;
            }
            
            if (options[confirmation] == deleteText) {
                DeleteAction.deleteSelectedItems(libraryManager, selectedItems);
            } 
            else if (options[confirmation] == removeText) {
                RemoveFromLibraryAction.removeFromLibrary(libraryManager, selectedItems);
            }
        }
    }
    @Override
    public void keyReleased(KeyEvent e) {
    }
    @Override
    public void keyTyped(KeyEvent e) {
    }
}
