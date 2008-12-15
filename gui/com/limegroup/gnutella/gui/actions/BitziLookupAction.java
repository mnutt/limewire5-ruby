package com.limegroup.gnutella.gui.actions;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.limegroup.gnutella.FileDetails;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.gui.FileDetailsProvider;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;


/**
 * Launches a browser for a bitzi lookup for the first item of the array of the
 * selected {@link FileDetails}.
 */
public class BitziLookupAction extends AbstractAction {

	/**
	 * Setting for the bitzi lookup string, has to be processed through 
	 * {@link MessageFormat#format(java.lang.String, java.lang.Object[])} for
	 * adding the urn.
	 */
	private static final String BITZI_LOOKUP_URL =
		"http://bitzi.com/lookup/{0}?ref=limewire";
	
	FileDetailsProvider provider;
	
	public BitziLookupAction(FileDetailsProvider provider) {
		putValue(Action.NAME, I18n.tr
				("Lookup File with Bitzi"));
		this.provider = provider;
	}
	
    public void actionPerformed(ActionEvent e) {
		 FileDetails[] files = provider.getFileDetails();
		 if (files.length == 0) {
			 return ;
		 }

		 URN urn = files[0].getSHA1Urn();
		 if (urn != null) {
			 String urnStr = urn.toString();
			 int hashstart = 1+urnStr.indexOf(":",4);
			 String lookupUrl = MessageFormat.format
			  (BITZI_LOOKUP_URL, new Object[] { urnStr.substring(hashstart) });

			 GUIMediator.openURL(lookupUrl);
		 }
	}	
}
