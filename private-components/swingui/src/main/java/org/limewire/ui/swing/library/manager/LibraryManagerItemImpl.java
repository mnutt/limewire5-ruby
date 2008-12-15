package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.library.LibraryData;
import org.limewire.util.FileUtils;

public class LibraryManagerItemImpl implements LibraryManagerItem {

    private final boolean fullName;
    private final File file;
    private final LibraryData libraryData;
    private final LibraryManagerItem parent;
    
    private boolean isScanned;
    private List<LibraryManagerItem> children;
    
    public LibraryManagerItemImpl(LibraryManagerItem parent, 
            LibraryData libraryData,
            File file,
            boolean isScanned,
            boolean fullName) {
        this.parent = parent;
        this.libraryData = libraryData;
        this.file = canonicalize(file);
        this.isScanned = isScanned;
        this.fullName = fullName;
    }
    
    @Override
    public LibraryManagerItem getParent() {
        return parent;
    }
    
    private File canonicalize(File file) {
        try {
            return FileUtils.getCanonicalFile(file);
        } catch(IOException iox) {
            return file;
        }
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
            File[] files = file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return libraryData.isDirectoryAllowed(file);
                }
            });
            if(files != null) {
                for(File file : files) {
                    children.add(new LibraryManagerItemImpl(this, libraryData, file, !libraryData.isDirectoryExcluded(file), false));
                }
            }
        }
        
        return children;
    }

    @Override
    public boolean isScanned() {
        return isScanned;
    }

    @Override
    public void setScanned(boolean value) {
        isScanned = value;
        setScanChildren(value);
    }
    
    private void setScanChildren(boolean value) {
        for(LibraryManagerItem item : getChildren()) {
            item.setScanned(value);
        }
    }
}
