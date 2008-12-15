/*
 * Created on Mar 8, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.limegroup.gnutella.gui.search;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.util.MediaType;

import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.CheckBoxListPanel;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MessageService;
import com.limegroup.gnutella.gui.MultiLineLabel;
import com.limegroup.gnutella.gui.CheckBoxList.TextProvider;
import com.limegroup.gnutella.gui.download.DownloaderUtils;
import com.limegroup.gnutella.util.QueryUtils;


/**
 *
 * This singleton class listens to window activated events and parses the clipboard to see
 * if a magnet uri is present.  If it is it asks the user whether to download the file.
 */
public class MagnetClipboardListener extends WindowAdapter {
	
	private static final Log LOG = LogFactory.getLog(MagnetClipboardListener.class);
	
	private static final MagnetClipboardListener instance = new MagnetClipboardListener();
	
	//the system clipboard
	private final Clipboard CLIPBOARD = Toolkit.getDefaultToolkit().getSystemClipboard();
	
	//dummy clipboard content
	private final StringSelection empty =new StringSelection("");
		
	private volatile String copiedText;
	
	/**
	 * a thread which parses the clipboard and launches magnet downloads.
	 */
	private final ExecutorService clipboardParser = ExecutorsHelper.newProcessingQueue("clipboard parser");
	
	private Runnable parser = new Runnable() {
	    public void run() {
              parseAndLaunch();
	    }
	};
	
	/**
	 * @return true if no errors occurred.  False if we should not try to 
	 * parse the clipboard anymore.
	 */
	private void parseAndLaunch() {
	    Transferable data = null;
    	try{
    	//check if there's anything in the clipboard
    		data = CLIPBOARD.getContents(this);
    	}catch(IllegalStateException isx) {
    		//we can't use the clipboard, give up.
    		return;
    	}
    	
    	//is there anything in the clipboard?
    	if (data==null) 
    		return;
    	
    	//then, check if the data in the clipboard is text
    	if (!data.isDataFlavorSupported(DataFlavor.stringFlavor)) 
    		return;
    		
    	
    	//next, extract the content into a string
    	String contents=null;
    	
    	try{
    		contents = (String)data.getTransferData(DataFlavor.stringFlavor);
    	} catch (IOException iox) {
    		LOG.info("problem occured while trying to parse clipboard, do nothing",iox);
    		return;
    	} catch (UnsupportedFlavorException ufx) {
    		LOG.error("UnsupportedFlavor??",ufx);
    		return;
    	} 
    	
    	//could not extract the clipboard as text.
    	if (contents == null)
    		return;
		
		String copied = copiedText;
		if (copied != null && copied.equals(contents)) {
			// it is the magnet we just created
			return;
		}
    	
    	//check if the magnet is valid
    	final MagnetOptions[] opts = MagnetOptions.parseMagnets(contents);
    	if (opts.length == 0)
    		return; //not a valid magnet link
    	
    	//at this point we know we have a valid magnet link in the clipboard.
    	LOG.info("clipboard contains "+ contents);
    	
    	//purge the clipboard at this point
    	purgeClipboard();
    	
    	handleMagnets(opts, true);
	}
	
	private MagnetClipboardListener() {
        super();
	}
	
	public static MagnetClipboardListener getInstance() {
		return instance;
	}
	
	/**
	 * Sets the text that is going to be copied to the clipboard from withing 
	 * LimeWire, so that the listener can discern between our own copied magnet 
	 * links and the ones pasted from the outside.
	 * @param text
	 */
	public void setCopiedText(String text) {
		copiedText = text;
	}
	
	/**
	 * ask the clipboard parser to see if there is a magnet.
	 */
	@Override
    public void windowActivated(WindowEvent e) {
	    clipboardParser.execute(parser);
	}
	
	/**
	 * clears the clipboard from the current string  
	 */
	private void purgeClipboard(){
		try {
			CLIPBOARD.setContents(empty, empty);
		}catch(IllegalStateException isx) {
			//do nothing
		}
	}
	
	/**
	 * Handles an array of magnets:
	 * The magnets that are downloadable are shown in a dialog where the 
	 * user can choose which ones he would like to download.
	 * 
	 * Once single search is also started for a magnet that is 
	 * {@link MagnetOptions#isKeywordTopicOnly()}.
	 * 
	 * @param showDialog if true a dialog with the magnets is shown asking
	 * the user if s/he wants to download them
	 */
	public static void handleMagnets(final MagnetOptions[] magnets, 
			final boolean showDialog) {
		// get a nicer looking address from the magnet
    	// turns out magnets are very liberal.. so display the whole thing
		final MagnetOptions[] downloadCandidates = extractDownloadableMagnets(magnets);
    	
    	// and fire off the download
    	Runnable r = new Runnable() {
    		public void run() {
    			if (!showDialog) {
    				for (MagnetOptions magnet : downloadCandidates) {
						DownloaderUtils.createDownloader(magnet);
					}
    			}
    			else if (downloadCandidates.length > 0 ) {
					List<MagnetOptions> userChosen = showStartDownloadsDialog(downloadCandidates);
					for (MagnetOptions magnet : userChosen) {
						DownloaderUtils.createDownloader(magnet);
					}
				}
				boolean oneSearchStarted = false;
				for (int i = 0; i < magnets.length; i++) {
					if (!magnets[i].isDownloadable() 
						&& magnets[i].isKeywordTopicOnly() && !oneSearchStarted) {
						String query = QueryUtils.createQueryString
							(magnets[i].getKeywordTopic());
						SearchInformation info = 
							SearchInformation.createKeywordSearch
							(query, null, MediaType.getAnyTypeMediaType());
						if (SearchMediator.validateInfo(info) 
							== SearchMediator.QUERY_VALID) {
							oneSearchStarted = true;
							SearchMediator.triggerSearch(info);
						}
					}
				}
				GUIMediator.instance().setWindow(GUIMediator.Tabs.SEARCH);
    		}
    	};
   	    GUIMediator.safeInvokeLater(r);
	}
	
	/**
	 * Extracts magnets that are not keyword topic only magnets
	 * @param magnets
	 * @return
	 */
	private static MagnetOptions[] extractDownloadableMagnets(MagnetOptions[] magnets) {
		List<MagnetOptions> dls = new ArrayList<MagnetOptions>(magnets.length);
		for (int i = 0; i < magnets.length; i++) {
			MagnetOptions magnet = magnets[i];
			if (!magnet.isKeywordTopicOnly()) {
				dls.add(magnets[i]);
			}
		}
		// all magnets are downloadable, return original array
		if (dls.size() == magnets.length) {
			return magnets;
		}
		else {
			return dls.toArray(new MagnetOptions[0]);
		}
	}

    private static List<MagnetOptions> showStartDownloadsDialog(MagnetOptions[] opts) {
		
		CheckBoxListPanel<MagnetOptions> listPanel =
			new CheckBoxListPanel<MagnetOptions>(Arrays.asList(opts), new MagnetOptionsTextProvider(), true);
		listPanel.getList().setVisibleRowCount(5);
		
		Object[] content = new Object[] {
				new MultiLineLabel(I18n.tr
						   ("Would you like to start downloads from the following magnets?"), 400),
				Box.createVerticalStrut(ButtonRow.BUTTON_SEP),
				listPanel,
				Box.createVerticalStrut(ButtonRow.BUTTON_SEP),
				new MultiLineLabel(I18n.tr("All folders you select will also have their subfolders shared."), 400),
		};
		
		int response = JOptionPane.showConfirmDialog
            (MessageService.getParentComponent(), content, 
			 I18n.tr("Message"),
			 JOptionPane.YES_NO_OPTION);
		
		List<MagnetOptions> selected = listPanel.getSelectedElements();
		
		if (response == JOptionPane.YES_OPTION) {
		    return selected;
		}
		else {
		    return Collections.emptyList();
		}
	}
	
	private static class MagnetOptionsTextProvider implements TextProvider<MagnetOptions> {
		
		public String getText(MagnetOptions magnet) {
			String fileName = magnet.getDisplayName();
			if (fileName == null) {
				fileName = I18n.tr("No Filename");
			}
			return fileName;
		}

		public String getToolTipText(MagnetOptions magnet) {
			return GUIUtils.restrictWidth(magnet.toString(), 400);		
		}

		public Icon getIcon(MagnetOptions magnet) {
			return null;
		}
	}
}
