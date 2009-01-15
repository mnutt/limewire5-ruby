package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.limewire.util.FileUtils;

public class NoChildrenLibraryManagerItem implements LibraryManagerItem {

    private final File file;
    private final LibraryManagerItem parent;
    private boolean showFullName;
    
    public NoChildrenLibraryManagerItem(LibraryManagerItem parent, File file) {
        this.parent = parent;
        this.file = FileUtils.canonicalize(file);
    }
    
    @Override
    public String toString() {
        return displayName();
    }
    
    @Override
    public int addChild(LibraryManagerItem child) {
        throw new IllegalStateException("no children");
    }

    @Override
    public String displayName() {
        if(showFullName) {
            return file.getPath();
        } else {
            return file.getName();
        }
    }

    @Override
    public LibraryManagerItem getChildFor(File directory) {
        throw new IllegalStateException("no children");
    }

    @Override
    public List<LibraryManagerItem> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public Collection<? extends File> getExcludedChildren() {
        return Collections.emptyList();
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public LibraryManagerItem getParent() {
        return parent;
    }

    @Override
    public int removeChild(LibraryManagerItem item) {
        throw new IllegalStateException("no children");
    }

    @Override
    public void setShowFullName(boolean show) {
        this.showFullName = show;
    }

}
