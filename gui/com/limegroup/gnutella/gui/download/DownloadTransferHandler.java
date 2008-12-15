package com.limegroup.gnutella.gui.download;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.JComponent;

import com.limegroup.gnutella.gui.dnd.CompositeTransferable;
import com.limegroup.gnutella.gui.dnd.DNDUtils;
import com.limegroup.gnutella.gui.dnd.DropInfo;
import com.limegroup.gnutella.gui.dnd.FileTransfer;
import com.limegroup.gnutella.gui.dnd.FileTransferable;
import com.limegroup.gnutella.gui.dnd.LimeTransferHandler;
import com.limegroup.gnutella.gui.library.LibraryTableDataLine;
import com.limegroup.gnutella.gui.library.LibraryTableTransferable;
import com.limegroup.gnutella.gui.search.ResultPanel;
import com.limegroup.gnutella.gui.search.SearchMediator;
import com.limegroup.gnutella.gui.search.SearchResultTransferable;
import com.limegroup.gnutella.gui.search.TableLine;

class DownloadTransferHandler extends LimeTransferHandler {

    DownloadTransferHandler() {
        super(COPY);
    }

    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        if (DNDUtils.contains(transferFlavors, SearchResultTransferable.dataFlavor)) {
        	return true;
        }
    	return DNDUtils.DEFAULT_TRANSFER_HANDLER.canImport(comp, transferFlavors);
    }
    
    @Override
    public boolean canImport(JComponent c, DataFlavor[] flavors, DropInfo ddi) {
    	return canImport(c, flavors);
    }

    @Override
    public boolean importData(JComponent comp, Transferable t) {
        if (DNDUtils.contains(t.getTransferDataFlavors(), SearchResultTransferable.dataFlavor)) {
            try {
                SearchResultTransferable srt =
                        (SearchResultTransferable) t.getTransferData(SearchResultTransferable.dataFlavor);
                ResultPanel rp = srt.getResultPanel();
                TableLine[] lines = srt.getTableLines();
                SearchMediator.downloadFromPanel(rp, lines);
                return true;
            } catch (UnsupportedFlavorException e) {
            } catch (IOException e) {
            }
        }
        return DNDUtils.DEFAULT_TRANSFER_HANDLER.importData(comp, t);
    }
    
    @Override
    public boolean importData(JComponent c, Transferable t, DropInfo ddi) {
    	return importData(c, t);
    }
    
    @Override
    protected Transferable createTransferable(JComponent c) {
    	FileTransfer[] transfers = DownloadMediator.instance().getSelectedFileTransfers();
    	if (transfers.length > 0) { 
			// TODO dnd we mark the transferable as an internal one so no
			// internal drops are accepted
    		return new CompositeTransferable(new LibraryTableTransferable(new LibraryTableDataLine[0]), 
    				new FileTransferable(FileTransferable.EMPTY_FILE_LIST, Arrays.asList(transfers)));
    	}
    	return null;
    }
    
}
