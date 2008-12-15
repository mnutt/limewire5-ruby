package com.limegroup.gnutella.gui.library;

import java.io.File;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.library.LibraryUtils;

public class TorrentMetaFileDirectoryHolder extends AbstractDirectoryHolder{
    
    private String name;
    private String desc;
    
    public TorrentMetaFileDirectoryHolder() {
        this.name = I18n.tr
                ("List of .torrent files");
        this.desc = I18n.tr
                (".torrent files");
    }

    public File getDirectory() {
        return LibraryUtils.APPLICATION_SPECIAL_SHARE;
    }
    
    @Override
    public boolean accept(File pathname) {
        return super.accept(pathname) && pathname.getName().endsWith(".torrent");
    }

    @Override
    public String getDescription() {
        return name;
    }

    @Override
    public String getName() {
        return desc;
    }
    
    

    //TODO get torrent icon
    /*public Icon getIcon() {
        
    }*/

}
