package org.limewire.ui.swing.library;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.MagnetLinkFactory;

public class MagnetLinkCopier {
    
    public void copyLinkToClipBoard(FileItem fileItem, MagnetLinkFactory magnetFactory){
        StringSelection sel = new StringSelection(magnetFactory.createMagnetLink(fileItem));
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
    }
}
