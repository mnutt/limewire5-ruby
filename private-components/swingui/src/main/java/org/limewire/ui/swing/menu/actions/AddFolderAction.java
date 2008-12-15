/**
 * 
 */
package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class AddFolderAction extends AbstractAction {
    private final LibraryManager libraryManager;

    public AddFolderAction(String name, LibraryManager libraryManager) {
        super(name);
        this.libraryManager = libraryManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<File> folders = FileChooser.getInput(GuiUtils.getMainFrame(), I18n.tr("Add Folder(s)"), I18n
                .tr("Add Folder(s)"), FileChooser.getLastInputDirectory(),
                JFileChooser.DIRECTORIES_ONLY, JFileChooser.APPROVE_OPTION, true,
                new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory();
                    }

                    @Override
                    public String getDescription() {
                        return I18n.tr("All Folders");
                    }
                });

        if (folders != null) {
            for (File folder : folders) {
                libraryManager.getLibraryManagedList().addFolder(folder);
            }
        }

    }
}