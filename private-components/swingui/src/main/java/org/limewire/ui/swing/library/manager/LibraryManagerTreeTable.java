package org.limewire.ui.swing.library.manager;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.treetable.TreeTableModel;
import org.limewire.core.api.library.LibraryData;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkCellEditorRenderer;
import org.limewire.ui.swing.table.MouseableTreeTable;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.FileUtils;

/**
 * Tree table component used to display the library folders.
 */
public class LibraryManagerTreeTable extends MouseableTreeTable {
    
    private final LibraryData libraryData;
    private final ExcludedFolderCollectionManager excludedFolders;

    /**
     * Constructs a LibraryManagerTreeTable with the specified services.
     */
    public LibraryManagerTreeTable(IconManager iconManager, LibraryData libraryData, ExcludedFolderCollectionManager excludedFolders) {
        setTableHeader(null); // No table header for this table.
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setSelectionBackground(getBackground());
        setEditable(true);
        setHorizontalScrollEnabled(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setCellSelectionEnabled(false);
        setRowSelectionAllowed(false);
        setColumnSelectionAllowed(false);
        setTreeCellRenderer(new FolderRenderer(iconManager));
        setLeafIcon(null);
        setClosedIcon(null);
        setOpenIcon(null);
        
        this.libraryData = libraryData;
        this.excludedFolders = excludedFolders;
    }
    
    /**
     * Returns the LibraryManagerModel that provides the data displayed by 
     * this tree table.
     */
    public LibraryManagerModel getLibraryModel() {
        return (LibraryManagerModel)super.getTreeTableModel();
    }

    /**
     * Sets the data model for this tree table.  This method also installs
     * the cell editor and renderer for the "remove" column. 
     */
    @Override
    public void setTreeTableModel(TreeTableModel model) {
        super.setTreeTableModel(model);
        // lower case sinc hyperlink
        HyperlinkCellEditorRenderer renderer = new HyperlinkCellEditorRenderer(I18n.tr("remove"));
        HyperlinkCellEditorRenderer editor = new HyperlinkCellEditorRenderer(new RemoveAction());
        TableColumn removeColumn = getColumn(LibraryManagerModel.REMOVE_INDEX);        
        removeColumn.setCellEditor(editor);
        removeColumn.setCellRenderer(renderer);
        removeColumn.setMaxWidth(renderer.getPreferredSize().width);
        removeColumn.setMinWidth(renderer.getPreferredSize().width);
    }

    /**
     * Returns true if the cell at the specified row and column is editable.
     */
    @Override
    public boolean isCellEditable(int row, int col) {
        if (row >= getRowCount() || col >= getColumnCount() || row < 0 || col < 0) {
            return false;
        }
        return getColumnModel().getColumn(col).getCellEditor() != null;
    }


    /** Adds a new directory into this model. */
    public void addDirectory(File directory) {
        directory = FileUtils.canonicalize(directory);
        
        boolean expand = false;
        LibraryManagerItem root = getLibraryModel().getRoot();
        LibraryManagerItem parent = findParent(root, directory);
        LibraryManagerItem item = null;        
        
        // If no parent, it's going to be added to root.
        if(parent == null) {
            parent = root;
        }
        
        item = parent.getChildFor(directory);
        
        if(item == null) {
            item = new LibraryManagerItemImpl(parent, libraryData, excludedFolders, directory);
            getLibraryModel().addChild(item, parent);
            getLibraryModel().getAllExcludedSubfolders().remove(item.getFile());
            getLibraryModel().unexcludeAllSubfolders(item.getFile());
            
            removeDuplicateRoots(root, directory);
        } else {
            removeDuplicateRoots(root, directory);
            
            // If the item already exists, go through all its excluded children and explicitly add them.
            // work off a copy because we'll be modifying the list as we iterate through it.
            for(File excludedChild : getExcludedDescendents(item.getFile())) {
                expand = true;
                LibraryManagerItem excludedParent = findParent(item, excludedChild);
                getLibraryModel().addChild(new LibraryManagerItemImpl(excludedParent, libraryData, excludedFolders, excludedChild), excludedParent);
            }
            getLibraryModel().unexcludeAllSubfolders(item.getFile());
        }
        
        TreePath path = new TreePath(getLibraryModel().getPathToRoot(item));
        scrollPathToVisible(path);
        if(expand) {
            expandPath(path);
        }
        int row = getRowForPath(path);
        getSelectionModel().setSelectionInterval(row, row);
    }
    
    /**
     * Returns the excluded children of this file
     */
    private List<File> getExcludedDescendents(File parent) {
        List<File> excludedChildren = new ArrayList<File>();
        for(File file : getLibraryModel().getAllExcludedSubfolders()) {
            if(FileUtils.isAncestor(parent, file))
                excludedChildren.add(file);
        }
        return excludedChildren;
    }
    
    /**
     * Check all the top level roots to see if they are descendant of this directory.
     * If they are, remove them and their excluded children.
     */
    private void removeDuplicateRoots(LibraryManagerItem root, File directory) {
        // Make sure that we're not the ancestor of any existing item in the list
        // If we are, we remove that item.
        List<LibraryManagerItem> toRemove = new ArrayList<LibraryManagerItem>();
        for(LibraryManagerItem child : root.getChildren()) {
            if(FileUtils.isAncestor(directory, child.getFile()) && !directory.equals(child.getFile())) {
                toRemove.add(child);
            }
        }
        // updating the model that we just iterated over
        for(LibraryManagerItem child : toRemove) {
            getLibraryModel().removeChild(child);
            getLibraryModel().unexcludeAllSubfolders(child.getFile());
        }
    }
    
    private LibraryManagerItem findParent(LibraryManagerItem start, File directory) {
        if(start.getFile() != null && start.getFile().equals(directory.getParentFile())) {
            return start;
        }
        
        for(LibraryManagerItem child : start.getChildren()) {
            if(FileUtils.isAncestor(child.getFile(), directory)) {
                LibraryManagerItem found = findParent(child, directory);
                // Don't return immediately -- it's possible there could
                // be two ancestors listed if a parent directory is excluded
                // and this is explicitly added on its own.
                if(found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Action to remove a LibraryManagerItem from the tree table.  This is
     * called by the Remove button cell editor.
     */
    private class RemoveAction extends AbstractAction {
        
        public RemoveAction() {
            // lower case since hyperlink
            super(I18n.tr("remove"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Get row being edited.
            int row = getEditingRow();
            if (row < 0) return;
            
            // Remove item at row.  
            LibraryManagerItem item = (LibraryManagerItem) getModel().getValueAt(row, LibraryManagerModel.REMOVE_INDEX);
            if (item != null) {
                getLibraryModel().excludeChild(item);
            }
            
            // Cancel editing if cell position is no longer editable.  This
            // ensures that the editor component is removed after the last tree
            // table item is removed.
            if (!isCellEditable(row, getEditingColumn())) {
                TableCellEditor editor = getCellEditor();
                if (editor != null) editor.cancelCellEditing();
            }
            repaint();
        }
    }
}
