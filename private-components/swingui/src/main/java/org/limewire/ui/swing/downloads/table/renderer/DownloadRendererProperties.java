package org.limewire.ui.swing.downloads.table.renderer;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JComponent;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

class DownloadRendererProperties {

    @Resource
    private Color labelColor;

    @Resource
    private Font font;

    public DownloadRendererProperties() {
        GuiUtils.assignResources(this);
    }
    
    public void decorateComponent(JComponent component){
        component.setForeground(labelColor);
        component.setFont(font);
    }

    public Font getFont() {
        return font;
    }

}
