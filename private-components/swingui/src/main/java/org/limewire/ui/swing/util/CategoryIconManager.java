package org.limewire.ui.swing.util;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.inject.LazySingleton;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class CategoryIconManager {
    
    private Icon audioIcon;
    private Icon imageIcon;
    private Icon videoIcon;
    private Icon documentIcon;
    private Icon programIcon;
    private Icon otherIcon;
    
    @Resource
    private Icon smallAudioIcon;
    @Resource
    private Icon smallImageIcon;
    @Resource
    private Icon smallVideoIcon;
    @Resource
    private Icon smallDocumentIcon;
    @Resource
    private Icon smallProgramIconWinVista;
    @Resource
    private Icon smallProgramIconWinXP;
    @Resource
    private Icon smallProgramIconOSX;
    @Resource
    private Icon smallProgramIconLinux;
    @Resource
    private Icon smallOtherIcon;
    
    private final Provider<IconManager> iconManager;
    
    @Inject
    CategoryIconManager(Provider<IconManager> iconManager) {
        this.iconManager = iconManager;
        
        GuiUtils.assignResources(this);
        
        audioIcon = smallAudioIcon;
        imageIcon = smallImageIcon;
        videoIcon = smallVideoIcon;
        documentIcon = smallDocumentIcon;
        otherIcon = smallOtherIcon;
            
        if (OSUtils.isMacOSX()) {
            programIcon = smallProgramIconOSX;
        } else if (OSUtils.isWindowsVista()) {
            programIcon = smallProgramIconWinVista;
        } else if (OSUtils.isWindowsXP()) {
            programIcon = smallProgramIconWinXP;
        } else {
            programIcon = smallProgramIconLinux;
        }
    }
    
    /**
     * Returns the LimeWire-specific icons for the given category.
     */
    public Icon getIcon(Category category) {
        switch (category) {
        case AUDIO:
            
            return audioIcon;
            
        case DOCUMENT:
            
            return documentIcon;
            
        case IMAGE:
            
            return imageIcon;
            
        case VIDEO:
            
            return videoIcon;
            
        case PROGRAM:
            
            return programIcon;
            
        default:
            
            return otherIcon;
            
        }
    }
    
    /**
     * Returns the local MIME type for files of category DOCUMENT and OTHER (if it can). For all other
     * types it returns the LimeWire icon for the file category.
     */
    public Icon getIcon(PropertiableFile file) {
        switch(file.getCategory()) {
        case DOCUMENT:
        case OTHER:
            return iconManager.get().getIconForPropertiableFile(file);
        default:
            return getIcon(file.getCategory());
        }
    }
}