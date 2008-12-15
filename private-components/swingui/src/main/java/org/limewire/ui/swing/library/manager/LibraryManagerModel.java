package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.tree.TreePath;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.limewire.ui.swing.util.I18n;

public class LibraryManagerModel extends AbstractTreeTableModel {

    public static final int FOLDER = 0;
    public static final int DONT_SCAN_INDEX = 1;
    public static final int SCAN_INDEX = 2;
    
    public LibraryManagerModel(RootLibraryManagerItem item) {
        super(item);
    }
    
    /**
     * Gets the path from the root to the specified node.
     * 
     * @param aNode
     *            the node to query
     * @return an array of {@code LibraryManagerItem}s, where
     *         {@code arr[0].equals(getRoot())} and
     *         {@code arr[arr.length - 1].equals(aNode)}, or an empty array if
     *         the node is not found.
     * @throws NullPointerException
     *             if {@code aNode} is {@code null}
     */
    public LibraryManagerItem[] getPathToRoot(LibraryManagerItem item) {
        List<LibraryManagerItem> path = new ArrayList<LibraryManagerItem>();
        LibraryManagerItem node = item;

        while (node != root) {
            path.add(0, node);

            node = node.getParent();
        }

        if (node == root) {
            path.add(0, node);
        }

        return path.toArray(new LibraryManagerItem[0]);
    }
    
    /**
     * Invoked this to insert newChild at location index in parents children.
     * This will then message nodesWereInserted to create the appropriate event.
     * This is the preferred way to add children as it will create the
     * appropriate event.
     */
    public void addChildToRoot(LibraryManagerItem item) {
        int idx = getRoot().addChild(item);
        modelSupport.fireChildAdded(new TreePath(getPathToRoot(getRoot())), idx, item);
    }
    
    public void removeChildFromRoot(LibraryManagerItem item) {
        int idx = getRoot().removeChild(item);
        modelSupport.fireChildRemoved(new TreePath(getPathToRoot(getRoot())), idx, item);
    }
    
    
    @Override
    public RootLibraryManagerItem getRoot() {
        return (RootLibraryManagerItem)super.getRoot();
    }
    
    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(Object node, int column) {
        if(node instanceof LibraryManagerItem) {
            LibraryManagerItem item = (LibraryManagerItem) node;
            switch (column) {
                case DONT_SCAN_INDEX: return item;
                case SCAN_INDEX: return item;
                case FOLDER: return item.displayName();
            }
        }
        return null;
    }
    
    public String getColumnName(int column) {
        switch(column) {
            case DONT_SCAN_INDEX: return I18n.tr("Don't Scan");
            case SCAN_INDEX: return I18n.tr("Scan");
            case FOLDER: return I18n.tr("Folder");
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public LibraryManagerItem getChild(Object parent, int index) {
        if(parent instanceof LibraryManagerItem) {
            List<LibraryManagerItem> items = ((LibraryManagerItem)parent).getChildren();
            if(index < 0 || index >= items.size())
                return null;
            return items.get(index);
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if(parent instanceof LibraryManagerItem) {
            LibraryManagerItem item = (LibraryManagerItem) parent;
            return item.getChildren().size();
        }
        return 0;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if(parent instanceof LibraryManagerItem && child instanceof LibraryManagerItem) {
            LibraryManagerItem childItem = (LibraryManagerItem) child;
            LibraryManagerItem parentItem = (LibraryManagerItem) parent;
            
            for(int i = 0; i < parentItem.getChildren().size(); i++) {
                LibraryManagerItem item = parentItem.getChildren().get(i);
                if(item.equals(childItem))
                    return i;
            }
        }
        
        return -1;
    }
    
    public Collection<File> getManagedDirectories() {
        Collection<File> manageRecursively = new HashSet<File>();
        List<LibraryManagerItem> children = getRoot().getChildren();
        for(LibraryManagerItem child : children) {
            if(child.isScanned()) {
                manageRecursively.add(child.getFile());
            }
        }
        return manageRecursively;
    }
    
    public Collection<File> getExcludedDirectories() {
        Collection<File> excludes = new HashSet<File>();
        List<LibraryManagerItem> children = getRoot().getChildren();
        for(LibraryManagerItem child : children) {
            if(child.isScanned()) {
                addExclusions(child, excludes);
            }
        }
        return excludes;
    }
        
    private void addExclusions(LibraryManagerItem item, Collection<File> excludes) {
        if(!item.isScanned()) {
            excludes.add(item.getFile());
        } else {
            for(LibraryManagerItem child : item.getChildren()) {
                addExclusions(child, excludes);
            }
        }
    }    
}
