package com.limegroup.gnutella.gui.search;


import org.limewire.core.settings.SearchSettings;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;

public class SpamFilter implements TableLineFilter {
    
    private static final Saver SAVER = new Saver();

	/**
	 * return false if a TableLine is rated as spam and _filter is true and true
	 * otherwise
	 */
	public boolean allow(TableLine node) {
		return !isAboveSpamThreshold(node);
	}

	/**
	 * This method is called to mark a TableLine and remember whether it has
	 * been marked as spam or not spam by the user
	 * 
	 * @param line
	 *            the TableLine that has been marked by the user
	 * @param isSpam
	 *            whether or not it is spam or not.
	 */
	public void markAsSpamUser(TableLine line, boolean isSpam) {
		RemoteFileDesc[] descs = line.getAllRemoteFileDescs();
		if (isSpam)
			GuiCoreMediator.getSpamManager().handleUserMarkedSpam(descs);
		else
			GuiCoreMediator.getSpamManager().handleUserMarkedGood(descs);
		line.update();
        
        // save the rating data after each user action
        BackgroundExecutorService.schedule(SAVER);
	}

    private static class Saver implements Runnable {
        public void run() {
            GuiCoreMediator.getRatingTable().save();
        }
    }

    /**
     * Returns true if TableLine's spam rating is above 
     * SearchSettings.FILTER_SPAM_RESULTS threshold
     */
    static final boolean isAboveSpamThreshold(TableLine line) {
        return line.getSpamRating() >= SearchSettings.FILTER_SPAM_RESULTS.getValue();
    }
}
