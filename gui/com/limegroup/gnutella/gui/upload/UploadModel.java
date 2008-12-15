package com.limegroup.gnutella.gui.upload;

import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.gui.tables.BasicDataLineModel;

/**
 * This class provides access to the <tt>ArrayList</tt> that stores all of the
 * uploads displayed in the upload window.
 */
final class UploadModel extends BasicDataLineModel<UploadDataLine, Uploader> {

    /**
     * Constructs the upload model & sets its DataLine class
     */
    UploadModel() {
        super(UploadDataLine.class);
    }
    
    /**
     * Creates a new UploadDataLine
     */
    @Override
    public UploadDataLine createDataLine() {
        return new UploadDataLine();
    }    	    
    
	/** 
	 * Update the row associated with this uploader's host & fileIndex
	 * @return the index of the uploader we updated, or -1 if we couldn't
	 *  find it
	 * @implements DataLineModel interface
	 */	
	@Override
    public int update(Uploader uploader) {
        int end = getRowCount();
        UploadDataLine ud;
        for (int i = 0; i < end; i++ ) {
            ud = get(i);
            // If the current line is inactive, the file indexes are the same
            // and the hosts are the same, then replace the data line.
            if ( ud.getFileIndex() == uploader.getIndex() &&
                 ud.getHost().equals(uploader.getHost()) &&
                 ud.isInactive()
               ) {
               //rather than removing & adding a new DataLine
               // (which would fire two table events and remove
               //  any selection on that row), just
               //re-initialize the existing dataline with a
               //different uploader.
               ud.initialize(uploader);
               fireTableRowsUpdated(i, i);
               return i;
            }
        }
        //we couldn't find the uploader.
        return -1;
    }
	
	/**
	 * Returns a count of the active uploads.
	 *
	 * @return the number of active uploads
	 */
	int countActiveUploads() {
		int size  = getRowCount();
		int count = 0;

		for (int i=0; i<size; i++) {
			UploadDataLine ud = get(i);
			if(!ud.isInactive()) count++;
		}
		return count;
	}

	/**
	 * Returns the currently connected uploads.
	 *
	 * @return the number of current uploads
	 */
	int getCurrentUploads() {
		int size  = getRowCount();
		int count = 0;

		for (int i=0; i<size; i++) {
			UploadDataLine dd = get(i);
			if(dd.isUploading()) count++;
		}
		return count;
	}

    /**
     * Returns the aggregate amount of bandwidth being consumed by active uploads.
     *  
     * @return the total amount of bandwidth being consumed by active uploads.
     */
    double getActiveUploadsBandwidth() {
        int size = getRowCount();
        double count = 0.0;

        for (int i=0; i<size; i++) {
            UploadDataLine dd = get(i);
            if(dd.isUploading()) {
                //  Speed can be -1 for some inactive states, so max with 0 
                count += Math.max(dd.getSpeed(), 0.0);
            }
        }
        return count;
    }

	/**
	 * Over-ride the default refresh so that we can
	 * set the CLEAR_BUTTON as appropriate.
	 */
	@Override
    public Object refresh() {
		int size = getRowCount();
		boolean inactiveUploadPresent = false;
		for(int i=0; i<size; i++) {
			UploadDataLine ud = get(i);
			ud.update();
			inactiveUploadPresent |= ud.isInactive();
		}
		fireTableRowsUpdated(0, size);
		return inactiveUploadPresent ? Boolean.TRUE : Boolean.FALSE;
	}

	/**
	 * Clears all completed uploads from the upload list.
	 */
	void clearCompleted() {
		for(int i=getRowCount()-1; i>=0; i--) {
			UploadDataLine line = get(i);
			if(line.isInactive()) remove(i);
		}
	}
}








