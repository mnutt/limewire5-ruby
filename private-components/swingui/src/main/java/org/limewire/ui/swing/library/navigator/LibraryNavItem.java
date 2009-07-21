package org.limewire.ui.swing.library.navigator;

import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.ui.swing.util.I18n;


public class LibraryNavItem {

    public static enum NavType {
        LIBRARY, PUBLIC_SHARED, LIST
    }
    
    private final NavType navType;
    private final LocalFileList localFileList;
    private final SharedFileList sharedFileList; // null if it can't be typed to this.
    
    public LibraryNavItem(LocalFileList localFileList) {
        this.localFileList = localFileList;
        if(localFileList instanceof SharedFileList) {
            this.sharedFileList = (SharedFileList)localFileList;
            this.navType = sharedFileList.isPublic() ? NavType.PUBLIC_SHARED : NavType.LIST;
        } else {
            this.sharedFileList = null;
            this.navType = NavType.LIBRARY;
        }
    }
    
    public LocalFileList getLocalFileList() {
        return localFileList;
    }
    
    public int getId() {
        return sharedFileList != null ? sharedFileList.getId() : -1;
    }
    
    public String getDisplayText() {
        switch(navType) {
        case LIST:
            return sharedFileList.getCollectionName();
        case LIBRARY:
            return I18n.tr("Library");
        case PUBLIC_SHARED:
            return I18n.tr("Public Shared");
        default:
            throw new IllegalStateException("unknown type: " + navType);
        }
    }
    
    public boolean canRemove() {
        return navType == NavType.LIST;
    }
    
    public NavType getType() {
        return navType;
    }
    
    public boolean isShared() {        
        if (getType() == NavType.PUBLIC_SHARED){
            return true;
        }
        
        if(sharedFileList != null){
            return sharedFileList.getFriendIds().size() > 0;
        }
        
        return false;
    }
}
