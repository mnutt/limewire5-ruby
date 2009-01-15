package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.limewire.core.api.library.LibraryData;
import org.limewire.util.FileUtils;

public class LibraryManagerItemImpl implements LibraryManagerItem {

    private boolean fullName;
    private final File file;
    private final LibraryData libraryData;
    private final LibraryManagerItem parent;
    private final List<File> excludedChildren;
    private final boolean showExcludedChildren;
    
    private List<LibraryManagerItem> children;
    
    public LibraryManagerItemImpl(LibraryManagerItem parent, 
            LibraryData libraryData,
            File file,
            boolean showExcludedChildren) {
        this.showExcludedChildren = showExcludedChildren;
        this.parent = parent;
        this.libraryData = libraryData;
        this.file = FileUtils.canonicalize(file);
        this.excludedChildren = new ArrayList<File>();
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
                    if(!showExcludedChildren && libraryData.isDirectoryExcluded(folder)) {
                        excludedChildren.add(folder);
                    } else {
                        children.add(new LibraryManagerItemImpl(this, libraryData, folder, showExcludedChildren));
                    }
                }
            }
        }
        
        Collections.sort(children, new Orderer());
        return children;
    }
    
    public int addChild(LibraryManagerItem item) {
        excludedChildren.remove(item.getFile());
        int idx = Collections.binarySearch(getChildren(), item, new Orderer());
        if(idx >= 0) {
            throw new IllegalStateException("already contains: " + item + ", in: " + children);
        }
        idx = -(idx + 1);
        children.add(idx, item);
        assert item.getParent() == this;
        return idx;
    }

    public int removeChild(LibraryManagerItem item) {
        excludedChildren.add(item.getFile());
        int idx = getChildren().indexOf(item);
        assert idx != -1;
        children.remove(idx);
        return idx;
    }    
    
    @Override
    public Collection<? extends File> getExcludedChildren() {
        getChildren(); // calculate the exclusions...
        return Collections.unmodifiableList(excludedChildren);
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
