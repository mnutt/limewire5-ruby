/*
 *  XNap Commons
 *
 *  Copyright (C) 2005  Felix Berger
 *  Copyright (C) 2005  Steffen Pingel
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *  
 *  Adapted version from the XNap Commons project.
 */
package com.limegroup.gnutella.gui.trees;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

import org.limewire.collection.FixedsizeForgetfulHashMap;

/**
 * Taken from the XNap Commons project and slightly adapted to use File objects
 * instead of FileNodes.
 * 
 * Represents a partial view on the file system. Has one root node which is of
 * type {@link String} and can have an arbitrary number of File nodes
 * underneath.
 * 
 * A {@link FileFilter} can be set to control what files will be shown in the
 * tree.
 */
public class FileTreeModel extends AbstractTreeModel {

    private List<File> subRoots;

    private Hashtable<String, List<File>> subChildren = new Hashtable<String, List<File>>();

    private File cachedDir;

    private boolean cacheSorted;

    private File[] cache;

    private Comparator<File> comparator = DEFAULT_COMPARATOR;

    private FileFilter filter = new DefaultFilter();

    private boolean sort = true;
    
    public static final Comparator<File> DEFAULT_COMPARATOR = new FileComparator();
    
    private FixedsizeForgetfulHashMap<File, Integer> cachedChildCountMap = new FixedsizeForgetfulHashMap<File, Integer>(500);

    public FileTreeModel(String root, File... roots) {
        super(root);

        if (roots != null) {
            subRoots = new ArrayList<File>(roots.length);
            for (int i = 0; i < roots.length; i++) {
                addSubRoot(roots[i]);
            }
        } else {
            subRoots = new ArrayList<File>();
        }
    }

    @Override
    public boolean isLeaf(Object node) {
        return getChildCount(node) == 0;
    }

    @Override
    public int getChildCount(Object node) {
        if (node instanceof File && ((File) node).canRead()) {
            File file = (File)node;
            Integer cached = cachedChildCountMap.get(file);
            if(cached == null) {
               cached = getSubDirs(file, false).length;
               cachedChildCountMap.put(file, cached);
            }
            return cached;               
        } else if (root.getClass().isInstance(node)) {
            if (node.equals(root)) {
                return subRoots.size();
            }
            return subChildren.get(node).size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof File) {
            File[] children = getSubDirs((File) parent, sort);

            if (index >= children.length) {
                return null;
            }
            return children[index];
        } else if (root.getClass().isInstance(parent)) {
            if (parent.equals(root) && index < subRoots.size())
                return subRoots.get(index);

            List<File> v = subChildren.get(parent);
            return (index < v.size()) ? v.get(index) : null;
        } else {
            return null;
        }
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof File) {
            File[] children = getSubDirs((File) parent, sort);

            if (children == null)
                return -1;

            for (int i = 0; i < children.length; i++) {
                if (children[i] == child)
                    return i;
            }
        } else if (root.getClass().isInstance(parent)) {
            if (parent.equals(root)) {
                return subRoots.indexOf(child);
            }
            return ((List) subChildren.get(parent)).indexOf(child);
        }

        return -1;
    }

    /**
     * Guaranteed to not return null.
     * 
     * @param f
     * @param doSort
     * @return
     */
    private File[] getSubDirs(File f, boolean doSort) {
        if (f == cachedDir && cacheSorted == doSort)
            return cache;

        File[] children = f.listFiles(filter);
        if (children == null) {
            cache = new File[0];
        } else {
            cache = children;
            if (doSort) {
                Arrays.sort(cache, comparator);
            }
        }
        cachedDir = f;
        cacheSorted = doSort;
        return cache;
    }

    public void addSubRoot(File f) {
        if (subRoots.contains(f))
            return;

        subRoots.add(f);

        Object[] path = { root };
        int[] indices = { subRoots.size() - 1 };
        Object[] children = { f };
        fireTreeNodesInserted(new TreeModelEvent(this, path, indices, children));
    }

    public boolean isSubRoot(File file) {
        return subRoots.contains(file);
    }
    
    public void removeSubRoot(File f) {
        int index = subRoots.indexOf(f);
        if (index != -1) {
            subRoots.remove(index);
            fireTreeNodesRemoved(new TreeModelEvent(this, new Object[] { root }, 
                    new int[] { index }, new Object[] { f }));
        }
    }

    public void removeSubRoots() {
        /* remove respective Lists in hash tree */
        for (int i = 0; i < subRoots.size(); i++)
            subChildren.remove(subRoots.get(i));

        subRoots.clear();

        Object[] path = { root };
        fireTreeStructureChanged(new TreeModelEvent(this, path));
    }

    @Override
    public void valueForPathChanged(TreePath path, Object value) {
        fireTreeNodesChanged(new TreeModelEvent(this, path));
    }

    // TODO: this looks like it'll never do anything, because
    // subRoots is a List<File>, so it can't contain a String.
    //  ???
    public void removeChildrenOfSubRoot(String s) {
        if (!subRoots.contains(s))
            return;

        subChildren.get(s).clear();

        Object[] path = { root, s };
        fireTreeStructureChanged(new TreeModelEvent(this, path));
    }

    public static class FileComparator implements Comparator<File> {

        public int compare(File o1, File o2) {
            return o1.getAbsolutePath().compareToIgnoreCase(
                    o2.getAbsolutePath());
        }
    }

    /**
     * Sets a file filter that is used for listing directories in the tree.
     * <p>
     * The default filter excludes hidden files and files that are not
     * directories.
     * <p>
     * 
     * @param filter can be <code>null</code>
     */
    public void setFileFilter(FileFilter filter) {
        this.filter = filter;
    }

    /**
     * Returns the currently set file filter.
     * 
     * @return can be <code>null</code>
     */
    public FileFilter getFileFilter() {
        return filter;
    }

    private class DefaultFilter implements FileFilter {
        public boolean accept(File file) {
            return file.isDirectory() && !file.isHidden();
        }
    }

    /**
     * Sets whether the listed files of a directory should be sorted.
     * <p>
     * Fires
     * {@link TreeModelListener#treeStructureChanged(javax.swing.event.TreeModelEvent)}
     * with the root node as parameter to make sure the whole tree is updated
     * correctly.
     * 
     * @param sort
     */
    public void setSortListedFiles(boolean sort) {
        if (this.sort != sort) {
            this.sort = sort;
            fireTreeStructureChanged(new TreeModelEvent(this,
                    new Object[] { getRoot() }));
        }
    }

    /**
     * Returns whether the listed files of a directory are being sorted.
     */
    public boolean getSortListedFiles() {
        return sort;
    }
}
