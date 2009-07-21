package org.limewire.ui.swing.components;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

public class PopupCloseButton extends IconButton {

    @Resource private Icon closeIcon;
    @Resource private Icon closeIconRollover;
    @Resource private Icon closeIconPressed;
    
    public PopupCloseButton() {
        GuiUtils.assignResources(this);
        setIcon(closeIcon);
        setRolloverIcon(closeIconRollover);
        setPressedIcon(closeIconPressed);
        setOpaque(false);
    }
}
