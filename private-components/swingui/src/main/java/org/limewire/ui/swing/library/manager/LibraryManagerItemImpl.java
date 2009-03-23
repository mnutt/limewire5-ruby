package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.limewire.core.api.library.LibraryData;
import org.limewire.util.FileUtils;

public class LibraryManagerItemImpl implements LibraryManagerItem {

    private boolean fullName;
    private final File file;
    private final LibraryData libraryData;
    private final ExcludedFolderCollectionManager excludedFolders;
    private final LibraryManagerItem parent;
    
    private List<LibraryManagerItem> children;
    
    public LibraryManagerItemImpl(LibraryManagerItem parent, 
            LibraryData libraryData, ExcludedFolderCollectionManager excludedFolders,
            File file) {
        this.parent = parent;
        this.libraryData = libraryData;
        this.excludedFolders = excludedFolders;
        this.file = FileUtils.canonicalize(file);
    }
    
    @Override
    public void setShowFullName(boolean show) {
        this.fullName = show;
    }
    
    @Override
    public String toString() {
        return displayName();
    }
    
    @Override
    public LibraryManagerItem getParent() {
        return parent;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        } else if(obj instanceof LibraryManagerItem) {
            LibraryManagerItem o = (LibraryManagerItem)obj;
            return o.getFile().equals(file);
        } else {
            return false;
        }
    }
    
    @Override
    public File getFile() {
        return file;
    }
    
    @Override
    public String displayName() {
        if(fullName) {
            return file.getPath();
        } else if(file.getName() == null || file.getName().length() == 0) {
            return file.getPath();
        } else {
            return file.getName();
        }
    }

    @Override
    public List<LibraryManagerItem> getChildren() {
        if(children == null) {
            children = new ArrayList<LibraryManagerItem>();
            File[] folders = file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return libraryData.isDirectoryAllowed(file);
                }
            });
            
            if(folders != null) {
                for(File folder : folders) {
                    if(!excludedFolders.isExcluded(folder)) {
                        children.add(new LibraryManagerItemImpl(this, libraryData, excludedFolders, folder));
                    }
                }
            }
        }
        
        Collections.sort(children, new Orderer());
        return children;
    }
    
    public int addChild(LibraryManagerItem item) {
        int idx = Collections.binarySearch(getChildren(), item, new Orderer());
        if(idx >= 0) {
            throw new IllegalStateException("already contains: " + item + ", in: " + children);
        }
        idx = -(idx + 1);
        getChildren().add(idx, item);
        assert item.getParent() == this;
        return idx;
    }

    public int removeChild(LibraryManagerItem item) {
        int idx = getChildren().indexOf(item);
        assert idx != -1;
        getChildren().remove(idx);
        return idx;
    }    
    
    @Override
    public LibraryManagerItem getChildFor(File directory) {
        for(LibraryManagerItem child : getChildren()) {
            if(child.getFile().equals(directory)) {
                return child;
            }
        }
        return null;
    }    
    
    private static class Orderer implements Comparator<LibraryManagerItem> {
        @Override
        public int compare(LibraryManagerItem o1, LibraryManagerItem o2) {
            return o1.getFile().compareTo(o2.getFile());
        }
    }
}
