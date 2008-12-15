package com.limegroup.gnutella.gui.upload;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * This class is an <tt>ActionListener</tt> that resumes the selected uploads
 * in the upload window.
 */
final class ChatListener implements ActionListener {
    
    private UploadMediator um;
    
    ChatListener(UploadMediator um) {
        this.um = um;
    }    
	
	public void actionPerformed(ActionEvent ae) {
		um.chatWithSelectedUploads();
	}
}
