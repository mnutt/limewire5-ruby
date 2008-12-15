/**
 * 
 */
package com.limegroup.gnutella.gui.search;

import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;

import javax.swing.JComponent;

import com.limegroup.gnutella.gui.dnd.LimeTransferHandler;

class ResultPanelTransferHandler extends LimeTransferHandler {

	/** The ResultPanel this is handling. */
    private final ResultPanel panel;
    
    ResultPanelTransferHandler(ResultPanel panel) {
    	super(DnDConstants.ACTION_COPY | DnDConstants.ACTION_MOVE | DnDConstants.ACTION_LINK);
        this.panel = panel;
    }

    /**
     * Creates a Transferable for the selected lines.
     */
    @Override
    protected Transferable createTransferable(JComponent c) {
		return new SearchResultTransferable(panel, panel.getAllSelectedLines());
	}

}