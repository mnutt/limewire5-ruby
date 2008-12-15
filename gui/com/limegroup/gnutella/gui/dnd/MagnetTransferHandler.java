package com.limegroup.gnutella.gui.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.JComponent;

import com.limegroup.gnutella.browser.ExternalControl;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.gui.search.MagnetClipboardListener;

/**
 * Transferhandler that handles drags of magnet links onto limewire by
 * starting downloads for them. Defers actual handling of magnets to {@link
 * ExternalControl#handleMagnetRequest(String)}.
 */
public class MagnetTransferHandler extends LimeTransferHandler {

	@Override
	public boolean canImport(JComponent c, DataFlavor[] flavors, DropInfo ddi) {
		return canImport(c, flavors);
	}
	
	@Override
	public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
		if (DNDUtils.contains(transferFlavors, FileTransferable.URIFlavor)) {
			return true;
		}
		return false;
	}
	
	@Override
	public boolean importData(JComponent c, Transferable t, DropInfo ddi) {
		return importData(c, t);
	}
	
	@Override
	public boolean importData(JComponent comp, Transferable t) {
		if (!canImport(comp, t.getTransferDataFlavors()))
			return false;
		
		try {
			MagnetOptions[] magnets =
				MagnetOptions.parseMagnets((String)t.getTransferData(FileTransferable.URIFlavor));
			if (magnets.length > 0) {
				MagnetClipboardListener.handleMagnets(magnets, false);
				return true;
			}
		} catch (UnsupportedFlavorException e) {
		} catch (IOException e) {
		}
		return false;
	}
	
}
