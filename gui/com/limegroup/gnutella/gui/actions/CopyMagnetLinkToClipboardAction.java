package com.limegroup.gnutella.gui.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.gui.FileDetailsProvider;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.search.MagnetClipboardListener;

/**
 * Action that creates a list of magnet links for a selection of {@link
 * com.limegroup.gnutella.library.FileDesc FileDescs} and copies the list to the clipboard. 
 *  <p>
 * It also makes sure that the {@link
 * com.limegroup.gnutella.gui.search.MagnetClipboardListener} doesn't pick up
 * the exact same list when the main frame becomes active.
 */
public class CopyMagnetLinkToClipboardAction extends AbstractAction {

	private FileDetailsProvider provider;
	
	public CopyMagnetLinkToClipboardAction(FileDetailsProvider provider) {
		this.provider = provider;
		putValue(Action.NAME, I18n.tr
				("Copy Magnet Links to Clipboard"));
		putValue(Action.SHORT_DESCRIPTION, I18n.tr
				("Copy the Magnet Links of the Selected Files to the Clipboard"));
	}

	
	public void actionPerformed(ActionEvent e) {
		setClipboardString();
	}
		
	private void setClipboardString() {
		FileDetails[] files = provider.getFileDetails();
        StringBuilder buffer = new StringBuilder();
		
		String sep = System.getProperty("line.separator");
		
		for (int i = 0; i < files.length; i++) {
			if (buffer.length() > 0) {
				buffer.append(sep);
			}
			MagnetOptions magnet = MagnetOptions.createMagnet(files[i], getInetSocketAddress(), getClientGUID());
			if (magnet.isDownloadable()) {
				buffer.append(magnet.toExternalForm());
			}
		}

		final String text = buffer.toString();
		MagnetClipboardListener.getInstance().setCopiedText(text);
		StringSelection sel = new StringSelection(text);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
	}
    
    private InetSocketAddress getInetSocketAddress() {
        // TODO maybe cache this, even statically
        NetworkManager networkManager = GuiCoreMediator.getNetworkManager();
        try {
            return new InetSocketAddress(InetAddress.getByAddress(networkManager.getAddress()), networkManager.getPort());
        } catch (UnknownHostException e) {
        }
        return null;
    }
    
    private byte[] getClientGUID() {
        ApplicationServices applicationServices = GuiCoreMediator.getApplicationServices();
        return applicationServices.getMyGUID();
    }

}
