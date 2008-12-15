package com.limegroup.gnutella.gui.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import javax.swing.JComponent;

import com.limegroup.gnutella.gui.GUIMediator;

/**
 * TransferHandler that handles uris pointing to http urls of torrent files.
 * Downloads are started if all uris are of this type.
 * 
 * The scheme of the uri has to be "http" and the path name of the uri has to
 * end with ".torrent" not regarding casing.
 */
public class TorrentURITransferHandler extends LimeTransferHandler {

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
			URI[] uris = DNDUtils.getURIs(t);
			if (areAllTorrentURLs(uris)) {
				for (URI uri : uris) {
					GUIMediator.instance().openTorrentURI(uri);
				}
				return true;
			}
		} catch (UnsupportedFlavorException e) {
		} catch (IOException e) {
		}
		return false;
	}

	// made package private for tests
	boolean areAllTorrentURLs(URI[] uris) {
		for (URI uri : uris) {
			String scheme = uri.getScheme();
			if (scheme == null || !scheme.equalsIgnoreCase("http")) {
				return false;
			}
			String path = uri.getPath();
            if (path == null || !path.toLowerCase(Locale.US).endsWith(".torrent")) {
                return false;
            }
		}
		return true;
	}
}
