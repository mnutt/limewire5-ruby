package com.limegroup.gnutella.gui.library;

import java.io.File;

import javax.swing.Icon;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;

/**
 * Holder for files that are displayed in the Library Tree under the node 
 * Store->Special Purchased Files. 
 * 
 * These represent songs purchased from the LWS found in the shared directories folders.
 *
 */
public class LWSSpecialFilesHolder extends AbstractDirectoryHolder {

    @Override
    public String getName() {
        return I18n.tr
            ("Individual Store Files");
    }

    @Override
    public String getDescription() {
        return I18n.tr
            ("List of All Individual Purchased Files");
    }

    public File getDirectory() {
        return null;
    }
    
    @Override
    public boolean isEmpty() {
        return false; 
    }
    
    @Override
    public File[] getFiles() {
        return new File[0];
    }
    
    @Override
    public boolean accept(File file) {
        return false; 
    }
    
    @Override
    public Icon getIcon() {
        return GUIMediator.getThemeImage("multifile_small");
    }

    @Override
    public boolean isStoreNode(){
        return true;
    }
}
