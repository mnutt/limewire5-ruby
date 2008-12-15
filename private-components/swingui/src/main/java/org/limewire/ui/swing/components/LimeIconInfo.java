package org.limewire.ui.swing.components;

import java.awt.Image;
import java.io.File;
import java.net.URI;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

public class LimeIconInfo {    

    @Resource private Icon limeIcon;
    @Resource private String limeFrameIconLocation;
    
    public LimeIconInfo() {
        GuiUtils.assignResources(this);
    }
    
    public Icon getIcon() {
        return limeIcon;
    }
    
    public String getIconLocation() {
        return limeFrameIconLocation;
    }
    
    public Image getImage() {
        return ((ImageIcon)limeIcon).getImage();
    }
    
    public File getIconFile() {
        return new File(
           URI.create(ClassLoader.getSystemResource(limeFrameIconLocation).getFile()).getPath())
                .getAbsoluteFile();
    }

}
