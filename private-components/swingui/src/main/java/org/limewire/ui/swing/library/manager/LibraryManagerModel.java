package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.TreePath;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.limewire.util.FileUtils;

public class LibraryManagerModel extends AbstractTreeTableModel {

    public static final int REMOVE_INDEX = 0;
    public static final int FOLDER_INDEX = 1;
    
    private static final int COLUMN_COUNT = 2;
    
    private ExcludedFolderCollectionManager excludedSubfolders;
    
    public LibraryManagerModel(RootLibraryManagerItem item, ExcludedFolderCollectionManager excludedSubfolders) {
        super(item);
        this.excludedSubfolders = excludedSubfolders;
    }
    
    @Override
    public int getHierarchicalColumn() {
        return FOLDER_INDEX;
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
     * Adds the child to the parent. This does not handle cleaning up the exclusion
     * list, only modifies what is shown in the UI. 
     */
    public void addChild(LibraryManagerItem child, LibraryManagerItem parent) {
        int idx = parent.addChild(child);
        modelSupport.fireChildAdded(new TreePath(getPathToRoot(parent)), idx, child);
    }
    
    /**
     * Removes the child from it's parent. This does not handle cleaning up
     * the exclusion list, only modifies what is shown in the UI. It is the
     * responsibility of the caller to cleanup any artifacts in the exclusion list.
     */
    public void removeChild(LibraryManagerItem item) {
        LibraryManagerItem parent = item.getParent();
        if (parent == null) {
            throw new IllegalStateException("node does not have a parent.");
        }

        int index = parent.removeChild(item);
        modelSupport.fireChildRemoved(new TreePath(getPathToRoot(parent)), index, item);
    }
    
    /**
     * Removes this item from the tree. If the item is a root, the item is not
     * added to the excluded folder list. If the item is not a root, the item
     * gets added to the exclusion list. All the children of this item
     * that are also excluded are removed from the exclusion list.
     */
    public void excludeChild(LibraryManagerItem item) {
        boolean isRootChild = getRootChildrenAsFiles().contains(item.getFile());
        removeChild(item);        
        if (!isRootChild) {
            //top level managed folders should not be added to exclude list
            excludeFolder(item.getFile());   
        }        
        unexcludeUnmanagedSubfolders(item.getFile());
    }
    
    /**
     * When adding a item. If any descendant of this item
     * is excluded, remove those children from the
     * exclusion list.
     */
    public void unexcludeAllSubfolders(File parent) {
        for(Iterator<File> iter = excludedSubfolders.getExcludedFolders().iterator(); iter.hasNext(); ) {
            File child = iter.next();
            if(FileUtils.isAncestor(parent, child))
                iter.remove();
        }
    }
    
    /**
     * Unexcludes all subfolders with no intermediate managed folder. When a folder
     * is removed from being managed, will remove all the children of that folder 
     * that are excluded unless that excluded folder is a child of a managed folder.
     * 
     * A(managed) |-B
     *            |-C (Excluded)
     *   
     *   Removing the node A will remove C from the exclusion list
     *   
     * A(managed) |-B
     *            |-C (Excluded)
     *              |- D (managed)
     *                 |- E (Excluded)
     *  
     *    Removing the node A will remove C from the exclusion list
     *    but will not remove E from the exclusion list since D is
     *    explicitly managed and D is the parent of E
     */    
    private void unexcludeUnmanagedSubfolders(File parent){
        for(Iterator<File> iter = excludedSubfolders.getExcludedFolders().iterator(); iter.hasNext(); ) {
            File child = iter.next();
            if(shouldRemoveFromExcludeList(child, parent))
                iter.remove();
        } 
    }
    
    /**Adds folder to list of excluded subfolders. */
    private void excludeFolder(File folder) { 
        excludedSubfolders.exclude(folder);     
    }
       
    /**
     * @return true if child is a subfolder of ancestor and there is no intermediate managed folder.  
     */
    private boolean shouldRemoveFromExcludeList(File child, File ancestor) {
        Collection<File> roots = getRootChildrenAsFiles();        
        
        child = child.getParentFile();        
        
        while (child != null) {
            if (child.equals(ancestor)) {
                return true;
            } else if (roots.contains(child)) {
                return false; // there is an intermediate managed folder
            }
            child = child.getParentFile();
        }
        
        return false;
    }
    
    @Override
    public RootLibraryManagerItem getRoot() {
        return (RootLibraryManagerItem)super.getRoot();
    }
    
    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    @Override
    public Object getValueAt(Object node, int column) {
        if(node instanceof LibraryManagerItem) {
            LibraryManagerItem item = (LibraryManagerItem) node;
            switch (column) {
                case REMOVE_INDEX: return item;
                case FOLDER_INDEX: return item;
            }
        }
        return null;
    }
    
    public String getColumnName(int column) {
        switch(column) {
            case REMOVE_INDEX: return null;
            case FOLDER_INDEX: return null;
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
    
    /**
     * Returns a list of all the root folders
     */
    public Collection<File> getRootChildrenAsFiles() {
        Collection<File> manageRecursively = new HashSet<File>();
        for(LibraryManagerItem child : getRoot().getChildren()) {
            manageRecursively.add(child.getFile());
        }
        return manageRecursively;
    }
    
    /**
     * Returns list of excluded directories
     */
    public Collection<File> getAllExcludedSubfolders() {
        return excludedSubfolders.getExcludedFolders();
    }

    public void setRootChildren(List<LibraryManagerItem> children) {
        List<LibraryManagerItem> oldChildren = new ArrayList<LibraryManagerItem>(getRoot().getChildren());
        for(LibraryManagerItem child : oldChildren) {
            removeChild(child);
        }
        for(LibraryManagerItem child : children) {
            addChild(child, getRoot());
        }
    }    

}
