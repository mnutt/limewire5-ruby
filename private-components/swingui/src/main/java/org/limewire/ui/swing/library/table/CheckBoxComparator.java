/**
 * 
 */
package org.limewire.ui.swing.library.table;

import java.util.Comparator;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;

class CheckBoxComparator implements Comparator<LocalFileItem> {
    private final LocalFileList fileList;
    
    public CheckBoxComparator(LocalFileList fileList) {
        this.fileList = fileList;
    }
    
    @Override
    public int compare(LocalFileItem o1, LocalFileItem o2) {
        boolean isShared1 = fileList.contains(o1.getFile());
        boolean isShared2 = fileList.contains(o2.getFile());

        if(isShared1 && isShared2) {
            return 0;
        } else if(isShared1 && !isShared2) {
            return 1;
        } else if(!isShared1 && isShared2){
            return -1;
        } else if(o1.isShareable() && o2.isShareable()){
            return 0;
        } else if(o1.isShareable() && !o2.isShareable()){
            return 1;
        } else
            return -1;
    }
}