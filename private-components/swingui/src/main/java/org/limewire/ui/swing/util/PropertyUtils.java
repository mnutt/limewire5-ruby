package org.limewire.ui.swing.util;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;

public class PropertyUtils {
    
    public static String getTitle(FileItem item){
        if(item.getProperty(FilePropertyKey.TITLE) == null){
            return item.getName();
        } else {
            return (String)item.getProperty(FilePropertyKey.TITLE); 
        }
    }
    
    public static String getToolTipText(Object o){
        if(o instanceof FileItem){
            return getTitle((FileItem)o);
        }
        return o.toString();
    }

}
