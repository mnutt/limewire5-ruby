package org.limewire.ui.swing.warnings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HorizonalCheckBoxListPanel;
import org.limewire.ui.swing.components.OverlayPopupPanel;
import org.limewire.ui.swing.components.PopupHeaderBar;
import org.limewire.ui.swing.friends.FriendRequestPanel;
import org.limewire.ui.swing.mainframe.GlobalLayeredPane;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

class LibraryWarningDialog extends OverlayPopupPanel {

    private final LibraryManager libraryManager;
    private final LibraryFileAdder libraryFileAdder;

    @Resource private Color border;
    @Resource private Color messageForeground;
    @Resource private Font messageFont;
    @Resource private Font categoriesCheckBoxesFont;
    @Resource private Color categoriesCheckBoxesForeground;
    @Resource private Font alwaysCheckBoxFont;
    @Resource private Color alwaysCheckBoxForeground;    
    
    private final Action cancelAction = new AbstractAction(I18n.tr("Cancel")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    };
    
    @Inject
    public LibraryWarningDialog(
            @GlobalLayeredPane JLayeredPane layeredPane,
            FriendRequestPanel friendRequestPanel,
            LibraryManager libraryManager, LibraryFileAdder libraryFileAdder) {
        
        super(layeredPane);
        
        this.libraryManager = libraryManager;
        this.libraryFileAdder = libraryFileAdder;
        
        GuiUtils.assignResources(this);
        
        setLayout(new BorderLayout());
        
        PopupHeaderBar header = new PopupHeaderBar(I18n.tr("Choose Categories"), cancelAction);
        add(header, BorderLayout.NORTH);
    }

    public void initialize(final LocalFileList fileList, final List<File> files) {

        int directoryCount = 0;
        File folder = null;
        for (File file : files) {
            if (file.isDirectory()) {
                directoryCount++;
                folder = file;
                if (directoryCount > 1) {
                    // short circuit just need to know if there is more than 1
                    // null folder when more than 1 folder.
                    folder = null;
                    break;
                }
            }
        }

        JPanel contentPanel = new JPanel(new MigLayout("nogrid, gap 10, fill, insets 14 8 14 8, align center"));
        contentPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, border));
        
        final HorizonalCheckBoxListPanel<Category> categories = new HorizonalCheckBoxListPanel<Category>(
                Category.getCategoriesInOrder());
        categories.setSelected(libraryManager.getLibraryData().getManagedCategories());
        categories.setForeground(categoriesCheckBoxesForeground);
        categories.setFont(categoriesCheckBoxesFont);
        
        JLabel messageLabel = new JLabel("<html>"+getMessage(fileList, folder)+"</html>");
        messageLabel.setFont(messageFont);
        messageLabel.setForeground(messageForeground);
        
        contentPanel.add(messageLabel, "wrap");
        contentPanel.add(categories, "wrap");
        final JCheckBox alwaysAsk = new JCheckBox(
                I18n
                        .tr("Always choose these kind of files when I add a folder to the Library or a List"),
                false);
        alwaysAsk.setFont(alwaysCheckBoxFont);
        alwaysAsk.setForeground(alwaysCheckBoxForeground);
        contentPanel.add(alwaysAsk, "wrap");
        contentPanel.add(new JButton(new AbstractAction(getButtonName(fileList)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                LibrarySettings.ASK_ABOUT_FOLDER_DROP_CATEGORIES.set(!alwaysAsk.isSelected());
                libraryManager.getLibraryData().setCategoriesToIncludeWhenAddingFolders(
                        categories.getSelected());
                libraryFileAdder.addFilesInner(fileList, files);
                LibraryWarningDialog.this.dispose();
            }
        }), "alignx right");
        contentPanel.add(new JButton(cancelAction));
        
        add(contentPanel, BorderLayout.CENTER);
        
        repaint();
        validate();
    }

    private static String getButtonName(LocalFileList fileList) {
        SharedFileList sharedFileList = null;
        if (fileList instanceof SharedFileList) {
            sharedFileList = (SharedFileList) fileList;
        }

        LibraryFileList libraryFileList = null;
        if (fileList instanceof LibraryFileList) {
            libraryFileList = (LibraryFileList) fileList;
        }

        if (libraryFileList != null || sharedFileList != null && !sharedFileList.isPublic()
                && sharedFileList.getFriendIds().size() == 0) {
            return I18n.tr("Add");
        } else {
            return I18n.tr("Share");
        }
    }

    private static String getMessage(LocalFileList fileList, File folder) {
        SharedFileList sharedFileList = null;
        if (fileList instanceof SharedFileList) {
            sharedFileList = (SharedFileList) fileList;
        }

        LibraryFileList libraryFileList = null;
        if (fileList instanceof LibraryFileList) {
            libraryFileList = (LibraryFileList) fileList;
        }

        if (libraryFileList != null || sharedFileList != null && !sharedFileList.isPublic()
                && sharedFileList.getFriendIds().size() == 0) {
            if (folder != null) {
                return I18n.tr(
                        "What kind of files do you want to add from \"{0}\" and its subfolders?",
                        folder.getName());
            } else {
                return I18n
                        .tr("What kind of files do you want to add from these folders and their subfolders?");
            }
        } else if (sharedFileList != null && sharedFileList.isPublic()) {
            if (folder != null) {
                return I18n
                        .tr(
                                "What kind of files do you want to <b>share with the world</b> from \"{0}\" and its subfolders?",
                                folder.getName());
            } else {
                return I18n
                        .tr("What kind of files do you want to <b>share with the world</b> from these folders and their subfolders?");
            }
        } else {
            if (folder != null) {
                return I18n
                        .tr(
                                "What kind of files do you want to share with selected friends from \"{0}\" and its subfolders?",
                                folder.getName());
            } else {
                return I18n
                        .tr("What kind of files do you want to share with selected friends from these folders and their subfolders?");
            }
        }
    }

    @Override
    public void resize() {
        Rectangle parentBounds = getParent().getBounds();
        int w = 520;
        int h = 180;
        setBounds((int)parentBounds.getWidth()/2-w/2,
                (int)parentBounds.getHeight()/2-h/2,
                w, h);
    }
}
