package com.limegroup.gnutella.gui.upload;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * This class is an <tt>ActionListener</tt> that clears completed uploads
 */
final class ClearListener implements ActionListener {

    private UploadMediator um;
    
    ClearListener(UploadMediator um) {
        this.um = um;
    }    
	
	public void actionPerformed(ActionEvent ae) {
		um.clearCompletedUploads();
	}
}
