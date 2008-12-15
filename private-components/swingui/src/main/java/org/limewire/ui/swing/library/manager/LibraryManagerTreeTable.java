package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.TreeTableModel;
import org.limewire.core.api.library.LibraryData;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.util.FileUtils;

public class LibraryManagerTreeTable extends JXTreeTable {
    
    private final LibraryData libraryData;

    public LibraryManagerTreeTable(LibraryData libraryData) {
        setTableHeaderRenderer();
        
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setEditable(true);
        this.libraryData = libraryData;
    }
    
    public LibraryManagerModel getLibraryModel() {
        return (LibraryManagerModel)super.getTreeTableModel();
    }
    
    @Override
    public void setTreeTableModel(TreeTableModel model) {
        super.setTreeTableModel(model);

        // all the nodes are always folders, set the leaf node to the folder icon
        setLeafIcon(UIManager.getIcon("Tree.closedIcon"));
  
        getColumn(LibraryManagerModel.SCAN_INDEX).setCellRenderer(new ScanButtonRenderer());
        getColumn(LibraryManagerModel.SCAN_INDEX).setCellEditor(new ScanButtonEditor(this));
        getColumn(LibraryManagerModel.DONT_SCAN_INDEX).setCellEditor(new DontScanButtonEditor(this));
        getColumn(LibraryManagerModel.DONT_SCAN_INDEX).setCellRenderer(new DontScanButtonRenderer());        
        
        getColumn(LibraryManagerModel.SCAN_INDEX).setMinWidth(80);
        getColumn(LibraryManagerModel.SCAN_INDEX).setMaxWidth(80);
        getColumn(LibraryManagerModel.DONT_SCAN_INDEX).setMinWidth(80);
        getColumn(LibraryManagerModel.DONT_SCAN_INDEX).setMaxWidth(80);
        
        //position the columns
        moveColumn(LibraryManagerModel.SCAN_INDEX, LibraryManagerModel.FOLDER);
        moveColumn(LibraryManagerModel.DONT_SCAN_INDEX, LibraryManagerModel.SCAN_INDEX);
    }

    
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
        
        LibraryManagerItem root = getLibraryModel().getRoot();
        LibraryManagerItem item = findItem(root, directory);
        if(item != null) {
            item.setScanned(true);
        } else {
            item = new LibraryManagerItemImpl(root, libraryData, directory, true, true);
            getLibraryModel().addChildToRoot(item);
            // Make sure that we're not the ancestor of any existing item in the list
            // If we are, we remove that item.
            List<LibraryManagerItem> toRemove = new ArrayList<LibraryManagerItem>();
            for(LibraryManagerItem child : root.getChildren()) {
                if(FileUtils.isAncestor(directory, child.getFile()) && !directory.equals(child.getFile())) {
                    toRemove.add(child);
                }
            }
            for(LibraryManagerItem child : toRemove) {
                getLibraryModel().removeChildFromRoot(child);
            }
        }
        
        TreePath path = new TreePath(getLibraryModel().getPathToRoot(item));
        scrollPathToVisible(path);
        int row = getRowForPath(path);
        getSelectionModel().setSelectionInterval(row, row);
    }
    
    private LibraryManagerItem findItem(LibraryManagerItem start, File directory) {
        if(start.getFile() != null && start.getFile().equals(directory)) {
            return start;
        }
        
        for(LibraryManagerItem child : start.getChildren()) {
            if(FileUtils.isAncestor(child.getFile(), directory)) {
                return findItem(child, directory);
            }
        }
        return null;
    }
    
    protected void setTableHeaderRenderer() { 
        JTableHeader th = getTableHeader(); 
        th.setDefaultRenderer(new TableCellHeaderRenderer()); 
    }
    
}
