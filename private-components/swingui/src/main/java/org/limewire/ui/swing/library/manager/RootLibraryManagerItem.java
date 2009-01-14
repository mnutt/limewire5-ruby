package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RootLibraryManagerItem implements LibraryManagerItem {

    private final List<LibraryManagerItem> children;
    private final Collection<File> defaultFiles;
    
    public RootLibraryManagerItem(Collection<File> defaultFiles) {
        this.children = new ArrayList<LibraryManagerItem>();
        this.defaultFiles = defaultFiles;
    }
    
    @Override
    public LibraryManagerItem getParent() {
        return null;
    }
    
    @Override
    public File getFile() {
        return null;
    }
    
    @Override
    public String displayName() {
        return "root";
    }
    
    @Override
    public void setShowFullName(boolean show) {
    }

    @Override
    public List<LibraryManagerItem> getChildren() {
        return children;
    }
    
    public int addChild(LibraryManagerItem item) {
        int idx = Collections.binarySearch(children, item, new Orderer());
        if(idx >= 0) {
            throw new IllegalStateException("already contains: " + item + ", in: " + children);
        }
        idx = -(idx + 1);
        children.add(idx, item);
        assert item.getParent() == this;
        if(defaultFiles.contains(item.getFile())) {
            item.setShowFullName(false);
        } else {
            item.setShowFullName(true);
        }
        return idx;
    }

    public int removeChild(LibraryManagerItem item) {
        int idx = children.indexOf(item);
        assert idx != -1;
        children.remove(idx);
        return idx;
    }
    
    @Override
    public Collection<? extends File> getExcludedChildren() {
        return Collections.emptyList();
    }
    
    @Override
    public LibraryManagerItem getChildFor(File directory) {
        for(LibraryManagerItem child : children) {
            if(child.getFile().equals(directory)) {
                return child;
            }
        }
        return null;
    }
    
    private class Orderer implements Comparator<LibraryManagerItem> {
        @Override
        public int compare(LibraryManagerItem o1, LibraryManagerItem o2) {
            boolean oneDefault = defaultFiles.contains(o1.getFile());
            boolean twoDefault = defaultFiles.contains(o2.getFile());
            if(oneDefault == twoDefault) {
                return o1.getFile().compareTo(o2.getFile());
            } else if(oneDefault) {
                return -1;
            } else {
                return 1;
            }
        }
    }

}
