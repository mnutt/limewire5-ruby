package com.limegroup.gnutella.gui.library;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;

import com.limegroup.gnutella.gui.dnd.CompositeTransferable;
import com.limegroup.gnutella.gui.dnd.DropInfo;
import com.limegroup.gnutella.gui.dnd.FileTransferable;
import com.limegroup.gnutella.gui.dnd.LimeTransferHandler;

/**
 * A TransferHandler for the library tree.
 * This only allows COPY, not MOVE, and will copy the selected
 * directory.  For drops, this will move shared files to the selected
 * directory, resharing them if dropped in a shared directory.  Unshared
 * drops will ask if you want to individually share the item.
 */
class LibraryTreeTransferHandler extends LimeTransferHandler {

    LibraryTreeTransferHandler() {
        super(COPY);
    }
    
    /**
     * Returns true if the drop can be imported.
     * TODO dnd this method is not called yet, LimeDropTarget has to installed
     * correctly for the component for it to work
     */
    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors, DropInfo info) {
    	return canImport(comp, transferFlavors);
    	
    	// TODO dnd
    	// Require:
    	//   - a directory to move to for table transfers.
        //   - the parent of the files is not the destination 
//  	if(DNDUtils.contains(transferFlavors, LibraryTableTransferable.libraryTableTransferable)) {
//            LibraryTree tree = (LibraryTree)comp;
//            DirectoryHolder dstHolder = tree.getHolderForPoint(info.getPoint());
//            if(dstHolder == null)
//                return false;
//            
//            File dstParent = dstHolder.getDirectory();
//            if(dstParent == null)
//                return false;
//
//            info.setDropAction(MOVE);
//            
//            Transferable t = info.getTransferable();
//            if(t == null)
//                return true;
//            
//            LibraryTableDataLine[] lines;
//            try {
//                lines = (LibraryTableDataLine[])t.getTransferData(LibraryTableTransferable.libraryTableTransferable);
//            } catch(UnsupportedFlavorException ufe) {
//                return false;
//            } catch(IOException iox) {
//                return false;
//            }
//            
//            // Can just check one, since they'll all have the same parent.
//            if(lines == null || lines.length == 0)
//                return false;
//            
//            if(lines[0].getFile().getParentFile().equals(dstParent))
//                return false;
//            
//            return true;
//        }
//        
//        // Require:
//        //   - holder to move to is a normal shared-files holder (not saved, etc..)
//        //  if transferable can be processed, then:
//        //   - folder to be moved is also a normal shared-files holder
//        //   - folder to be moved is not equal to destination
//        //   - folder to be moved is not moving to its direct parent
//        //   - folder to be moved is not moving to a child of itself
//        if(DNDUtils.contains(transferFlavors, LibraryTreeTransferable.libraryTreeFlavor)) {
//            LibraryTree tree = (LibraryTree)comp;
//            DirectoryHolder dstHolder = tree.getHolderForPoint(info.getPoint());
//            if(!(dstHolder instanceof SharedFilesDirectoryHolder))
//                return false;
//            File dstParent = dstHolder.getDirectory();
//            if(dstParent == null)
//                return false;
//        
//            info.setDropAction(MOVE);
//            
//            Transferable t = info.getTransferable();
//            if(t == null) // unknown, can't handle.
//                return true;
//            DirectoryHolder holder = null;
//            try { 
//                holder = (DirectoryHolder)t.getTransferData(LibraryTreeTransferable.libraryTreeFlavor);
//            } catch(UnsupportedFlavorException ufe) {
//                return false;
//            } catch(IOException iox) {
//                return false;
//            }
//            if(!(holder instanceof SharedFilesDirectoryHolder))
//                return false;
//            
//            File src = holder.getDirectory();
//            File dst = new File(dstParent, src.getName());
//            if(src.equals(dst))
//                return false;
//            
//            // Cannot transfer a parent to its child or itself
//            if(FileUtils.isAncestor(src, dstParent))
//                return false;
//            
//        }
//        
//        // Require nothing.
//        if (DNDUtils.containsFileFlavors(transferFlavors)) {
//            return true;
//        }
//        
//        return false;
    }

    /**
     * Imports incoming data.
     */
    @Override
    public boolean importData(JComponent comp, Transferable t, DropInfo info) {
    	return importData(comp, t);
    	// TODO dnd
//        if(!canImport(comp, t.getTransferDataFlavors(), info))
//            return false;
//        
//        LibraryTree tree = (LibraryTree)comp;
//        
//        try {
//            if(t.isDataFlavorSupported(LibraryTreeTransferable.libraryTreeFlavor))
//                return transferFromTree(tree, (DirectoryHolder)t.getTransferData(LibraryTreeTransferable.libraryTreeFlavor), info);
//            else if(t.isDataFlavorSupported(LibraryTableTransferable.libraryTableTransferable))
//                return transferFromTable(tree, (LibraryTableDataLine[])t.getTransferData(LibraryTableTransferable.libraryTableTransferable), info);
//            else
//                return transferPlainFiles(tree, DNDUtils.getFiles(t), info);
//        } catch(IOException iox) {
//            return false;
//        } catch(UnsupportedFlavorException ufe) {
//            return false;
//        }
    }
    
    /**
     * Moves the holder from point A to point B IFF they were
     * SharedFileHolders, otherwise nothing happens.
     * @param tree
     * @param holder
     * @return
     */
//    private boolean transferFromTree(LibraryTree tree, DirectoryHolder holder, DropInfo info) {
//        File src = holder.getDirectory();
//        DirectoryHolder dstHolder = tree.getSelectedDirectoryHolder();
//        File dstParent = dstHolder.getDirectory();
//        File dst = new File(dstParent, src.getName());
//        if(!src.renameTo(dst))
//            return false;
//        
//        RouterService.getFileManager().loadSettings();
//        return true;
//    }
    
    // TODO: allow COPY
//    private boolean transferFromTable(LibraryTree tree, LibraryTableDataLine[] lines, DropInfo info) {
//        File directory = tree.getSelectedDirectory();
//        FileManager fman = RouterService.getFileManager();
//        
//        boolean reload = false;
//        for(int i = 0; i < lines.length; i++) {
//            File oldFile = lines[i].getFile();
//            File newFile = new File(directory, oldFile.getName());
//            if(oldFile.equals(newFile))
//                continue;
//            
//            boolean renamed = oldFile.renameTo(newFile);
//            if(renamed) {
//                if(fman.isFileShared(oldFile))
//                    fman.renameFileIfShared(oldFile, newFile);
//                else if(newFile.isDirectory())
//                    reload = true;
//                else
//                    fman.addFileIfShared(newFile);
//            }
//        }
//        
//        if(reload)
//            fman.loadSettings();
//        
//        return true;
//    }
    
    /**
     * Shares the list of files.
     * 
     * @param tree
     * @param files
     * @return
     */
    // TODO dnd merge table & plain files and do correct MOVE or COPY
    // for now, default handlers take care of files and ask user to share them
//    private boolean transferPlainFiles(LibraryTree tree, File[] files, DropInfo info) {
//    	return false;
//    }    

    /**
     * Returns a FileTransferable of the selected directory, or null if
     * no selected directory.
     */
    @Override
    protected Transferable createTransferable(JComponent comp) {
        LibraryTree tree = (LibraryTree)comp;
        DirectoryHolder holder = tree.getSelectedDirectoryHolder();
        List<File> files = FileTransferable.EMPTY_FILE_LIST;
        
        // For incomplete or saved, just copy the visisible files --
        // for everything else, copy the 
        if(holder == null){
            //nothing selected
            return null;
        } else if(holder instanceof SavedFilesDirectoryHolder || holder instanceof IncompleteDirectoryHolder) {
            files = Arrays.asList(holder.getFiles());
        } else {
            File directory = holder.getDirectory();
            if(directory != null)
                files = Collections.singletonList(directory);
        }
        
        if(files.isEmpty())
            return new LibraryTreeTransferable(holder);
        else
            return new CompositeTransferable(new LibraryTreeTransferable(holder), new FileTransferable(files));
	}
	
}
