/**
 * 
 */
package com.limegroup.gnutella.gui.logging;

import javax.swing.Icon;

import com.limegroup.gnutella.gui.GUIMediator;

public enum LogType {
    UPLOAD("upload_generic"),
    DOWNLOAD("download_generic"),
    BROWSE_HOST("browse_host_generic");
    
    private final String icon;
    
    LogType(String icon) {
        this.icon = icon;
    }
    
    public Icon getIcon() {
        return GUIMediator.getThemeImage(icon);
    }
}