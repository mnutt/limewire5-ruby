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
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.settings.SharingSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.OverlayPopupPanel;
import org.limewire.ui.swing.components.PopupHeaderBar;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.mainframe.GlobalLayeredPane;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

class SharingWarningDialog extends OverlayPopupPanel {

    private final LibraryFileAdder libraryFileAdder;
    
    @Resource private Color border;
    @Resource private Color messageForeground;
    @Resource private Font messageFont;
    @Resource private Font alwaysCheckBoxFont;
    @Resource private Color alwaysCheckBoxForeground;    
    
    private final Action cancelAction = new AbstractAction(I18n.tr("Cancel")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            dispose();
        }
    };
        
    @Inject
    public SharingWarningDialog(
            @GlobalLayeredPane JLayeredPane layeredPane,
            LibraryMediator libraryMediator, LibraryFileAdder libraryFileAdder) {
        super(layeredPane);
        
        this.libraryFileAdder = libraryFileAdder;
        
        GuiUtils.assignResources(this);
        
        setLayout(new BorderLayout());
        
        PopupHeaderBar header = new PopupHeaderBar(I18n.tr("Share files?"), cancelAction);
        add(header, BorderLayout.NORTH);
    }

    public void initialize(final SharedFileList fileList, final List<File> files) {

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
        
        contentPanel.setLayout(new MigLayout("nogrid"));
        
        JLabel messageLabel = new JLabel("<html>"+getMessage(fileList, folder)+"</html>");
        messageLabel.setFont(messageFont);
        messageLabel.setForeground(messageForeground);
        
        contentPanel.add(messageLabel, "wrap");
        final JCheckBox warnMeCheckbox = new JCheckBox(I18n
                .tr("Warn me before adding folders to any shared list"), true);
        warnMeCheckbox.setFont(alwaysCheckBoxFont);
        warnMeCheckbox.setForeground(alwaysCheckBoxForeground);
        contentPanel.add(warnMeCheckbox, "wrap");
        
        
        contentPanel.add(new JButton(new AbstractAction(I18n.tr("Share")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SharingSettings.WARN_SHARING_FOLDER.setValue(warnMeCheckbox.isSelected());
                libraryFileAdder.addFilesInner(fileList, files);
                SharingWarningDialog.this.dispose();
            }
        }), "alignx right");
        contentPanel.add(new JButton(cancelAction));
        
        add(contentPanel, BorderLayout.CENTER);

        repaint();
        validate();
        
    }

    private static String getMessage(SharedFileList fileList, File folder) {
        if (fileList.isPublic()) {
            if (folder != null) {
                return I18n.tr("Share files in \"{0}\" and its subfolders <b>with the world</b>?", folder
                        .getName());
            } else {
                return I18n.tr("Share files in these folders and their subfolders <b>with the world</b>?");
            }
        } else {
            if (folder != null) {
                return I18n.tr("Share files in \"{0}\" and its subfolders with selected friends? ",
                        folder.getName());
            } else {
                return I18n
                        .tr("Share files in these folders and their subfolders with selected friends?");
            }
        }
    }
    
    @Override
    public void resize() {
        Rectangle parentBounds = getParent().getBounds();
        int w = 420;
        int h = 136;
        setBounds((int)parentBounds.getWidth()/2-w/2,
                (int)parentBounds.getHeight()/2-h/2,
                w, h);
    }
}
