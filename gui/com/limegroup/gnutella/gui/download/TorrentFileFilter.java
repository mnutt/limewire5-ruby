
package com.limegroup.gnutella.gui.download;

import java.io.File;
import java.util.Locale;

import javax.swing.filechooser.FileFilter;

public class TorrentFileFilter extends FileFilter {
    public static final TorrentFileFilter INSTANCE = new TorrentFileFilter();
    
       /* (non-Javadoc)
        * @see java.io.FileFilter#accept(java.io.File)
        */
       @Override
    public boolean accept(File file) {
               return file.isDirectory() || file.getName().toLowerCase(Locale.US).endsWith(".torrent");
       }
    
    /* (non-Javadoc)
     * @see javax.swing.filechooser.FileFilter#getDescription()
     */
    @Override
    public String getDescription() {
        // TODO i18nize
        return "Torrents";
    }
}