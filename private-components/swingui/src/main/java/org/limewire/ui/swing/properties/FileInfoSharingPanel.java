package org.limewire.ui.swing.properties;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.YesNoCheckBoxDialog;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.settings.QuestionsHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class FileInfoSharingPanel implements FileInfoPanel {

    @Resource private Font headerFont;
    @Resource private Icon removeIcon;
    @Resource private Icon removeIconRollover;
    @Resource private Icon removeIconPressed;
    @Resource private Color backgroundColor;
    
    private final JPanel component;
    private final FileInfoType type;
    private final PropertiableFile propertiableFile;
    private final SharedFileListManager sharedFileListManager;
    
    public FileInfoSharingPanel(FileInfoType type, PropertiableFile propertiableFile, 
            SharedFileListManager sharedFileListManager) {
        this.type = type;
        this.propertiableFile = propertiableFile;
        this.sharedFileListManager = sharedFileListManager;
        
        GuiUtils.assignResources(this);
        
        component = new JPanel(new MigLayout("fillx"));
        
        init();
    }
    
    public JComponent getComponent() {
        return component;
    }
    
    @Override
    public boolean hasChanged() {
        return false;
    }

    @Override
    public void save() {
        //currently the lists are updated on click, not on save
    }
    
    @Override
    public void unregisterListeners() {
        //no listeners registered
    }
    
    private void init() {
        component.setOpaque(false);
        switch(type) {
        case LOCAL_FILE:
            if(propertiableFile instanceof LocalFileItem) {
                List<SharedFileList> sharedWithList = getSharedWithList((LocalFileItem)propertiableFile);
                if(sharedWithList.size() > 0) {
                    component.add(createHeaderLabel(I18n.tr("Sharing from these lists")), "span, wrap");

                    final JPanel listPanel = new JPanel(new MigLayout("fill, nogrid, gap 0, insets 0"));
                    listPanel.setBackground(backgroundColor);
                    
                    for(SharedFileList sharedFileList : sharedWithList) {
                        final SharedFileList shareList = sharedFileList;
                        final JLabel listNameLabel = new JLabel(sharedFileList.getCollectionName());
                        final JButton removeButton = new IconButton(removeIcon, removeIconRollover, removeIconPressed);
                        removeButton.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                
                                if(showConfirmation(I18n.tr("Remove {0} from list {1}?", propertiableFile.getFileName(), shareList.getCollectionName()))) {
                                    shareList.removeFile(((LocalFileItem)propertiableFile).getFile());
    
                                    listPanel.remove(listNameLabel);
                                    listPanel.remove(removeButton);
                                    //make sure the friend actually disappears
                                    component.revalidate();
                                }
                            }
                        });
                        
                        listPanel.add(removeButton);
                        listPanel.add(listNameLabel, "gapright 20, wrap");
                    }
                    JScrollPane scroll = new JScrollPane(listPanel);
                    scroll.setOpaque(false);
                    scroll.setBorder(BorderFactory.createEmptyBorder());
                    component.add(scroll, "grow, wrap");
                } else {
                    component.add(createHeaderLabel(I18n.tr("This file is not shared")), "span, wrap");
                }
            }
            break;
        }
    }
    
    /**
     * Returns list of file lists that are shared and contain this file.
     */
    private List<SharedFileList> getSharedWithList(LocalFileItem fileItem) {
        List<SharedFileList> sharedWith = new ArrayList<SharedFileList>();
        
        sharedFileListManager.getModel().getReadWriteLock().readLock().lock();
        try {
            for(SharedFileList sharedFileList : sharedFileListManager.getModel()) {
                if(sharedFileList.contains(fileItem.getFile()) && sharedFileList.getFriendIds().size() > 0)
                    sharedWith.add(sharedFileList);
            }
        } finally {
            sharedFileListManager.getModel().getReadWriteLock().readLock().unlock();
        }
        return sharedWith;
    }
    
    private JLabel createHeaderLabel(String text) { 
        JLabel label = new JLabel(text);
        label.setFont(headerFont);
        return label;
    }
    
    private boolean showConfirmation(String message) {
        if (!QuestionsHandler.CONFIRM_REMOVE_FILE_INFO_SHARING.getValue()) {
            // no need to confirm here
            return true;
        }

        final YesNoCheckBoxDialog yesNoCheckBoxDialog = new YesNoCheckBoxDialog(I18n.tr("Remove File"), message, I18n
                .tr("Don't ask me again"), !QuestionsHandler.CONFIRM_REMOVE_FILE_INFO_SHARING.getValue(),
                I18n.tr("Yes"), I18n.tr("No"));
        yesNoCheckBoxDialog.setLocationRelativeTo(GuiUtils.getMainFrame());
        yesNoCheckBoxDialog.setVisible(true);

        QuestionsHandler.CONFIRM_REMOVE_FILE_INFO_SHARING.setValue(!yesNoCheckBoxDialog.isCheckBoxSelected());
        
        return yesNoCheckBoxDialog.isConfirmed();
    }
}
