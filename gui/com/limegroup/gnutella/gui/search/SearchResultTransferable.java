/**
 * 
 */
package com.limegroup.gnutella.gui.search;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * A Search Result transferable.  Contains the ResultPanel & TableLines
 * that are being transferred.
 */
public class SearchResultTransferable implements Transferable {
    
    public static final DataFlavor dataFlavor = new DataFlavor(SearchResultTransferable.class, "LimeWire Search Result");
    
	private ResultPanel panel;
	private TableLine[] lines;
	
	public SearchResultTransferable(ResultPanel panel, TableLine[] lines) {
		this.panel = panel;
		this.lines = lines;
	}
	
	public ResultPanel getResultPanel() {
		return panel;
	}
	
	public TableLine[] getTableLines() {
		return lines;
	}
	
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { dataFlavor };
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return flavor.equals(dataFlavor);
	}

	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (!isDataFlavorSupported(flavor))
			throw new UnsupportedFlavorException(flavor);
        else
            return this;
	}
	
}