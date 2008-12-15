package com.limegroup.gnutella.gui.upload;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.swing.Icon;

import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.IconManager;
import com.limegroup.gnutella.gui.tables.AbstractDataLine;
import com.limegroup.gnutella.gui.tables.ChatHolder;
import com.limegroup.gnutella.gui.tables.IconAndNameHolder;
import com.limegroup.gnutella.gui.tables.IconAndNameHolderImpl;
import com.limegroup.gnutella.gui.tables.LimeTableColumn;
import com.limegroup.gnutella.gui.tables.ProgressBarHolder;
import com.limegroup.gnutella.gui.tables.SizeHolder;
import com.limegroup.gnutella.gui.tables.SpeedRenderer;
import com.limegroup.gnutella.gui.tables.TimeRemainingHolder;
import com.limegroup.gnutella.gui.upload.UploadProgressBarRenderer.UploadProgressBarData;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.uploader.UploadType;

/**
 * This class handles all of the data for a single upload, representing
 * one "line" in the upload window.  It continually updates the
 * displayed data for the upload from the contained <tt>Uploader</tt>
 * instance.
 */
public final class UploadDataLine extends AbstractDataLine<Uploader> {

	/**
	 * Constant for the "connecting" upload state.
	 */
	private static final String CONNECTING_STATE =
		I18n.tr("Connecting");

	/**
	 * Constant for the "uploading" upload state.
	 */
	private static final String UPLOADING_STATE =
		I18n.tr("Uploading");

	/**
	 * Constant for the "limit reached" upload state.
	 */
	private static final String LIMIT_REACHED_STATE =
		I18n.tr("Upload Limit Reached");

	/**
	 * Constant for the "freeloader" upload state.
	 */
	private static final String FREELOADER_STATE =
		I18n.tr("Freeloader Uploading");

	/**
	 * Constant for the "interrupted" upload state.
	 */
	private static final String INTERRUPTED_STATE =
		I18n.tr("Transfer Interrupted");

	/**
	 * Constant for the "complete" upload state.
	 */
	private static final String COMPLETE_STATE =
		I18n.tr("Complete");

	/**
	 * Constant for the "file not found" upload state.
	 */
	private static final String FILE_NOT_FOUND_STATE =
		I18n.tr("File Not Found");

    /**
     * Constant for the "Queued" upload state.
     */
    private static final String QUEUED_STATE =
        I18n.tr("Queued at");
        
    /**
     * Constant for "Unavailable Range" upload state.
     */
    private static final String UNAVAILABLE_RANGE_STATE =
        I18n.tr("Range Unavailable");
        
    /**
     * Constant for the "Malformed Request" upload state.
     */
    private static final String MALFORMED_REQUEST_STATE =
        I18n.tr("Malformed Request");

    /**
	 * Constant for the "Banned Greedy Servent" upload state.
	 */
	private static final String BANNED_GREEDY_STATE =
        I18n.tr("Banned Greedy Servent");

    /**
	 * Constant for the "Uploading Hash Tree" upload state.
	 */
	private static final String HASH_TREE_STATE =
        I18n.tr("Uploading Hash Tree");
    
//    /** Constant for the 'Validating' upload state. */
//	private static final String VALIDATING_STATE =
//        GUIMediator.getStringResource("UPLOAD_TABLE_STRING_VALIDATING");

	/** Constant for the 'Suspended' upload state. */
	private static final String SUSPENDED_STATE =
		I18n.tr("Suspended");
	
	/** Constant for the 'Awaiting Requests' upload state. */
	private static final String AWAITING_REQUESTS_STATE =
		I18n.tr("Awaiting Requests");
	
    /**
     * Constant for "average bandwidth" tip
     */
    private static final String AVERAGE_BANDWIDTH =
        I18n.tr("Average Bandwidth");

    /**
     * Constant for "Started on" tip
     */
    private static final String STARTED_ON =
        I18n.tr("Started On");

    /**
     * Constant for "Finished on" tip
     */
    private static final String FINISHED_ON =
        I18n.tr("Finished On");

    /**
     * Constant for "Time Spent" tip
     */
    private static final String TIME_SPENT =
        I18n.tr("Time Spent");

	/**
	 * Variable for the name of the file being uploaded.
	 */
	private String _fileName;

	/**
	 * Variable for the status of the upload.
	 */
	private String _status;

	/**
	 * Variable for the hostname
	 */
	private String _hostName;

	/**
	 * Variable for the userAgent
	 */
	private String _userAgent;

	/**
	 * Variable for the progress bar
	 */
	private UploadProgressBarData _progress;

	/**
	 * Variable for whether or not chat is enabled
	 */
	private boolean _chatEnabled;

	/**
	 * Variable for whether or not browse is enabled
	 */
	private boolean _browseEnabled;

	/**
	 * Variable for the speed
	 */
	private double _speed;

	/**
	 * Variable for the time left
	 */
	private int _timeLeft;

	/**
	 * Variable for whether or not cleanup should do anything
	 */
	private boolean _persistConnection;

	/**
	 * Variable for the time the upload started.
	 */
	private long _startTime;

	/**
	 * Variable for the time the upload ended.
	 */
	private long _endTime = -1;

	/**
	 * Stores the current state of this upload, as of the last update.
	 * This is the state the everything should work off of to avoid the
	 * <tt>Uploader</tt> instance being in a different state than
	 * this data line.
	 */
	private UploadStatus _state;

	/**
	 * Column index for the file name.
	 */
	static final int FILE_INDEX = 0;
	private static final LimeTableColumn FILE_COLUMN =
	    new LimeTableColumn(FILE_INDEX, "UPLOAD_TABLE_STRING_NAME", I18n.tr("Name"),
	                160, true, IconAndNameHolder.class);

	/**
	 * Column index for the host name.
	 */
	static final int HOST_INDEX = 1;
	private static final LimeTableColumn HOST_COLUMN =
	    new LimeTableColumn(HOST_INDEX, "UPLOAD_TABLE_STRING_HOST", I18n.tr("Host"),
	                70, true, String.class);

	/**
	 * Column index for the file size.
	 */
	static final int SIZE_INDEX = 2;
	private static final LimeTableColumn SIZE_COLUMN =
	    new LimeTableColumn(SIZE_INDEX, "UPLOAD_TABLE_STRING_SIZE", I18n.tr("Size"),
	                25, true, SizeHolder.class);

	/**
	 * Column index for the file upload status.
	 */
	static final int STATUS_INDEX = 3;
	private static final LimeTableColumn STATUS_COLUMN =
	    new LimeTableColumn(STATUS_INDEX, "UPLOAD_TABLE_STRING_STATUS", I18n.tr("Status"),
	                100, true, String.class);

	/**
	 * Column index for whether or not the uploader is chat-enabled.
	 */
	static final int CHAT_INDEX = 4;
	private static final LimeTableColumn CHAT_COLUMN =
	    new LimeTableColumn(CHAT_INDEX, "UPLOAD_TABLE_STRING_CHAT", I18n.tr("Chat"),
	                10, true, ChatHolder.class);

	/**
	 * Column index for the progress of the upload.
	 */
	static final int PROGRESS_INDEX = 5;
	private static final LimeTableColumn PROGRESS_COLUMN =
	    new LimeTableColumn(PROGRESS_INDEX, "UPLOAD_TABLE_STRING_PROGRESS", I18n.tr("Progress"),
	                25, true, ProgressBarHolder.class);

	/**
	 * Column index for the upload speed.
	 */
	static final int SPEED_INDEX = 6;
	private static final LimeTableColumn SPEED_COLUMN =
	    new LimeTableColumn(SPEED_INDEX, "UPLOAD_TABLE_STRING_SPEED", I18n.tr("Speed"),
	                15, true, SpeedRenderer.class);

	/**
	 * Column index for the upload time remaining.
	 */
	static final int TIME_INDEX = 7;
	private static final LimeTableColumn TIME_COLUMN =
	    new LimeTableColumn(TIME_INDEX, "UPLOAD_TABLE_STRING_TIME_REMAINING", I18n.tr("Time"),
	                15, true, TimeRemainingHolder.class);

	/**
	 * Column index for the user agent
	 */
	static final int USER_AGENT_INDEX = 8;
	private static final LimeTableColumn USER_AGENT_COLUMN =
	    new LimeTableColumn(USER_AGENT_INDEX, "UPLOAD_TABLE_STRING_USER_AGENT", I18n.tr("Vendor/Version"),
	                70, true, String.class);

	/** Number of columns visible
	 *
	 */
	static final int NUMBER_OF_COLUMNS = 9;

	//implements DataLine interface
	public int getColumnCount() { return NUMBER_OF_COLUMNS; }

	/**
	 * Must initialize data.
	 *
	 * @param uploader the <tt>Uploader</tt>
	 *  that provides access to
	 *  information about the upload
	 */
	@Override
    public void initialize(Uploader uploader) {
        boolean started = initializer == null;
	    super.initialize(uploader);

        if (started) {
            _startTime = System.currentTimeMillis();
	        _chatEnabled = initializer.isChatEnabled();
	        _browseEnabled = initializer.isBrowseHostEnabled();
            _fileName = initializer.getFileName();
            _hostName = initializer.getHost();
    	    _userAgent = initializer.getUserAgent();
    	    if (_hostName == Uploader.BITTORRENT_UPLOAD)
    	    	_hostName = I18n.tr("Swarm for {0}", _fileName);
    	    if (_userAgent == Uploader.BITTORRENT_UPLOAD)
    	    	_userAgent = I18n.tr("Multiple");
    	    			
        }

		_endTime = -1;
		_status = "";
		_persistConnection = false;
		update();
	}

	// implements DataLine interface
	@Override
    public void cleanup() { if ( !_persistConnection) initializer.stop(); }

	/*
	 * Returns the <tt>Object</tt> stored at the specified column in this
	 * line of data.
	 *
	 * @param index the index of the column to retrieve data from
	 * @return the <tt>Object</tt> stored at that index
	 * @implements DataLine interface
	 */
	public Object getValueAt(int index) {
        switch(index) {
        case FILE_INDEX:
        	Icon icon;
        	if (initializer.getCustomIconDescriptor() == Uploader.BITTORRENT_UPLOAD)
        		icon = GUIMediator.getThemeImage("bittorrent_upload");
        	else {
        		FileDesc fd = initializer.getFileDesc();
        		icon = fd == null ? null : 
        			IconManager.instance().getIconForFile(fd.getFile());
        	}
            return new IconAndNameHolderImpl(icon, _fileName);
	    case HOST_INDEX:
	        return _hostName;
		case SIZE_INDEX:
			return new SizeHolder(getLength());
		case STATUS_INDEX:
			return _status;
		case CHAT_INDEX:
			return _chatEnabled ? Boolean.TRUE : Boolean.FALSE;
		case PROGRESS_INDEX:
			return _progress;
		case SPEED_INDEX:
			return new Double(_speed);
        case TIME_INDEX:
            return new TimeRemainingHolder(_timeLeft);
        case USER_AGENT_INDEX:
            return _userAgent;
		}
		return null;
	}

	// Implements DataLine interface
	public LimeTableColumn getColumn(int idx) {
	    switch (idx) {
	        case FILE_INDEX:            return FILE_COLUMN;
	        case HOST_INDEX:            return HOST_COLUMN;
	        case SIZE_INDEX:            return SIZE_COLUMN;
	        case STATUS_INDEX:          return STATUS_COLUMN;
	        case CHAT_INDEX:            return CHAT_COLUMN;
	        case PROGRESS_INDEX:        return PROGRESS_COLUMN;
	        case SPEED_INDEX:           return SPEED_COLUMN;
	        case TIME_INDEX:            return TIME_COLUMN;
	        case USER_AGENT_INDEX:      return USER_AGENT_COLUMN;
	    }
	    return null;
	}
	
	public boolean isClippable(int idx) {
	    switch(idx) {
        case CHAT_INDEX:
        case PROGRESS_INDEX:
            return false;
        default:
            return true;
	    }
    }
    
    public int getTypeAheadColumn() {
        return FILE_INDEX;
    }

	@Override
    public String[] getToolTipArray(int col) {
	    String[] info = new String[ _endTime != -1 ? 5 : 4];
	    String tp = AVERAGE_BANDWIDTH + ": " + GUIUtils.rate2speed(
	        initializer.getAverageBandwidth()
	    );
	    info[0] = STARTED_ON + " " + GUIUtils.msec2DateTime( _startTime );
	    if( _endTime != -1 ) {
	        info[1] = FINISHED_ON + " " + GUIUtils.msec2DateTime( _endTime );
	        info[2] = TIME_SPENT + ": " + CommonUtils.seconds2time(
	            (int)((_endTime - _startTime) / 1000 ) );
            info[3] = "";
	        info[4] = tp;
	    } else {
	        info[1] = TIME_SPENT + ": " + CommonUtils.seconds2time(
	            (int) ((System.currentTimeMillis() - _startTime) / 1000 ) );
            info[2] = "";
	        info[3] = tp;
	    }

	    return info;
	}

	// Implements DataLine interface
	public boolean isDynamic(int idx) {
	    switch(idx) {
	        case STATUS_INDEX:
	        case PROGRESS_INDEX:
	        case SPEED_INDEX:
	        case TIME_INDEX:
	            return true;
	    }
	    return false;
	}

	/**
	 * Returns the total size in bytes of the file being uploaded.
	 *
	 * @return the total size in bytes of the file being uploaded
	 */
	long getLength() {
		return initializer == null ? 0 : initializer.getFileSize();
	}

	/**
	 * Returns whether or not the <tt>Uploader</tt> for this upload
	 * is equal to the one passed in.
	 *
	 * @return <tt>true</tt> if the passed-in uploader is equal to the
	 *  <tt>Uploader</tt> for this upload, <tt>false</tt> otherwise
	 */
	boolean containsUploader(Uploader uploader) {
		return initializer.equals(uploader);
	}

	/**
	 * Returns the <tt>Uploader</tt> associated with this upload.
	 *
	 * @return the <tt>Uploader</tt> associated with this upload
	 */
	Uploader getUploader() {
		return initializer;
	}

	/**
	 * Returns the ip address string of the host we are uploading from.
	 *
	 * @return the ip address string of the host we are uploading from
	 */
	String getHost() {
		return initializer.getHost();
	}

	/**
	 * Returns the index of the file of the uploader
	 *
	 * @return the index of the file of the uploader
	 */
	int getFileIndex() {
	    return initializer.getIndex();
	}

	/**
	 * Return the state of the Uploader
	 *
	 * @return the state of the uploader
	 */
    UploadStatus getState() {
	    return _state;
	}

    /**
     * Return the speed of the Upload
     *
     * @return the speed of the upload
     */
    double getSpeed() { 
        return _speed;
    }
    
	/**
	 * Returns whether or not the upload has completed.
	 *
	 * @return <tt>true</tt> if the upload is complete, <tt>false</tt> otherwise
	 */
	boolean isCompleted() {
		return _state == UploadStatus.COMPLETE;
	}

	/**
	 * Returns whether or not chat is enabled for this upload.
	 *
	 * @return <tt>true</tt> if the host we're uploading from is chattable,
	 *  <tt>false</tt> otherwise
	 */
	boolean isChatEnabled() {
		return _chatEnabled;
	}

	/**
	 * Returns whether or not browse is enabled for this upload.
	 */
	boolean isBrowseEnabled() {
	    return _browseEnabled;
	}

	/**
	 * Updates all of the data for this upload, obtaining fresh information
	 * from the contained <tt>Uploader</tt> instance.
	 * @implements DataLine interface
	 */
	@Override
    public void update() {
	    // do not change the display if we are at an intermediary
	    // complete or connecting state.
	    // (meaning that this particular chunk finished, but more will come)
	    // we use _endTime to tell us when it's finished, because that is
	    // set when remove is called, which is only called when the entire
	    // upload has finished.
	    // we use getTotalAmountUploaded to know if a byte has been read
	    // (which would mean we're not connecting anymore)
        UploadStatus state = initializer.getState();
        UploadStatus lastState = initializer.getLastTransferState();
	    if ( (state == UploadStatus.COMPLETE && _endTime == -1) ||
	         (state == UploadStatus.CONNECTING &&
	          initializer.getTotalAmountUploaded() != 0)
	       ) {
            state = lastState;
        }
        
        // Reset the current state to be the lastState if we're complete now,
        // but our last transfer wasn't uploading, queued, or thex.
        if(state == UploadStatus.COMPLETE && 
          lastState != UploadStatus.UPLOADING &&
          lastState != UploadStatus.QUEUED &&
          lastState != UploadStatus.THEX_REQUEST) {
            state = lastState;
        }
            

		_speed = -1;
		_timeLeft = 0;
		this.updateStatus(state);
	}


	/**
	 * Updates the status of the upload based on the state stored in the
	 * <tt>Uploader</tt> instance for this <tt>UploadDataLine</tt>.
	 */
	private void updateStatus(UploadStatus state) {
		_state = state;
		switch (_state) {
		case CONNECTING:
			_status = CONNECTING_STATE;
			break;
		case FREELOADER:
		    _status = FREELOADER_STATE;
			break;
		case COMPLETE:
		    //must set progress for the case of when
		    //an upload completes when someone isn't watching the screen
		    //when they come back to it, it would have displayed as 0%
		    //since that's the first update to the dataline.
		    if ( _status != COMPLETE_STATE ) 
		        setProgress();
		    _status = COMPLETE_STATE;
			break;
        case UNAVAILABLE_RANGE:
            _status = UNAVAILABLE_RANGE_STATE;
            break;
        case MALFORMED_REQUEST:
            _status = MALFORMED_REQUEST_STATE;
            break;
//        case Uploader.NOT_VALIDATED:
//            _status = VALIDATING_STATE;
//            break;
        case SUSPENDED:
        	_status = SUSPENDED_STATE;
        	break;
        case WAITING_REQUESTS:
        	_status = AWAITING_REQUESTS_STATE;
        	break;
		case LIMIT_REACHED:
			_status = LIMIT_REACHED_STATE;
			break;
		case INTERRUPTED:
		    //must set progress for the case of when
		    //an upload completes when someone isn't watching the screen
		    //when they come back to it, it would have displayed as 0%
		    //since that's the first update to the dataline.
    		if ( _status != INTERRUPTED_STATE )
    		    setProgress();
			_status = INTERRUPTED_STATE;
			break;
		case FILE_NOT_FOUND:
		    _status = FILE_NOT_FOUND_STATE;
		    break;
        case THEX_REQUEST:
            _status = HASH_TREE_STATE;
            setProgress();
            setSpeedAndTimeLeft();
            break;
		case UPLOADING:
			_status = UPLOADING_STATE;
			setProgress();
			setSpeedAndTimeLeft();
			break;
	    case BROWSE_HOST:
	        throw new IllegalStateException("Browse Host status in GUI Upload view");
        case QUEUED:
            _status = QUEUED_STATE + " (" + 
                        (initializer.getQueuePosition() + 1) + ")";
            setProgress();
            break;
        case BANNED_GREEDY:
            _status = BANNED_GREEDY_STATE;
            break;
		default:
			throw new IllegalStateException("Unknown status " + state + " of uploader");
		}
	}
	
	/**
	 * Sets the speed & time left.
	 */
    private void setSpeedAndTimeLeft() {
            try {
                _speed = initializer.getMeasuredBandwidth();
            } catch(InsufficientDataException ide) {
                _speed = 0;
            }
            // If we have a valid rate (can't compute if rate is 0),
            // then determine how much time (in seconds) is remaining.
            if ( _speed > 0) {
                double kbLeft = (
                                 (double)getLength() - 
                                 (double)initializer.getTotalAmountUploaded()
                                ) / 1024.0;
                _timeLeft = Math.max(0,(int)(kbLeft / _speed));
            }
    }

	/**
	 * Set the _progress variable based on the cumulative amount
	 * read, current amount uploaded & the filesize.
	 */
	private void setProgress() {
        if (_progress == null)
        	_progress = new UploadProgressBarData(getHost() == Uploader.BITTORRENT_UPLOAD);
        _progress.totalSize = initializer.getFileSize();
        _progress.totalUploaded = initializer.getTotalAmountUploaded();
    }


	/**
	 * Returns whether or not this upload is in what is considered an "inactive"
	 * state, such as completeed, aborted, failed, etc.
	 *
	 * @return <tt>true</tt> if this upload is in an inactive state,
	 *  <tt>false</tt> otherwise
	 */
	boolean isInactive() {
	    //The upload is active up until 'remove' has been called on it.
	    return _endTime != -1;
	}

	/**
	 * Returns whether or not the upload for this line is currently uploading
	 *
	 * @return <tt>true</tt> if this upload is currently uploading,
	 *  <tt>false</tt> otherwise
	 */
	boolean isUploading() {
		return _state == UploadStatus.UPLOADING;
	}

	/**
	 * Updates the connection persistance.  Changes how cleanup() works.
	 */
	void setPersistConnection(boolean persist) {
	    _persistConnection = persist;
	}

	/**
	 * Sets the time this upload finished.
     * When this is called, we create a fake "Uploader" object so that the real
     * Uploader can have all its references garbage collected.  Otherwise, we
     * can end up holding too many things in memory.
	 */
	void setEndTime(long time) {
	    _endTime = time;
        initializer = new FakeUploader(initializer);
        super.initialize(initializer);
	}
	
    private static class FakeUploader implements Uploader {
        private final int idx;
        private final long tUp;
        private final int gPort;
        private final float mBand;
        private final float aBand;
        private final String name;
        private final long size;
        private final String host;
        private final UploadStatus state;
        private final String agent;
        private final boolean chat;
        private final boolean browse;
        private final UploadStatus lastState;
        private final FileDesc fd;
        private final String iconDesc;
        private final boolean tlsCapable;
        private final String addr;
        private final InetAddress inetAddr;
        private final int port;
        private final InetSocketAddress inetSocketAddr;
                
        FakeUploader(Uploader u) {
            idx = u.getIndex();
            tUp = u.getTotalAmountUploaded();
            gPort = u.getGnutellaPort();
            float bandwidth;
            try {
                bandwidth = u.getMeasuredBandwidth();
            } catch(InsufficientDataException e) {
                bandwidth = 0;
            }
            mBand = bandwidth;
            aBand = u.getAverageBandwidth();
            name = u.getFileName();
            size = u.getFileSize();
            host = u.getHost();
            state = u.getState();
            chat = u.isChatEnabled();
            browse = u.isBrowseHostEnabled();
            agent = u.getUserAgent();
            lastState = u.getLastTransferState();
            fd = u.getFileDesc();
            iconDesc = u.getCustomIconDescriptor();
            tlsCapable = u.isTLSCapable();
            addr = u.getAddress();
            inetAddr = u.getInetAddress();
            port = u.getPort();
            inetSocketAddr = u.getInetSocketAddress();
        }    
    
        public void stop() { }
        public String getFileName() { return name; }
        public long getFileSize() { return size; }
        public FileDesc getFileDesc() { return fd; }
        public int getIndex() { return idx; }
        public long amountUploaded() { return 0; }
        public long getTotalAmountUploaded() { return tUp; }
        public String getHost() { return host; }
        public UploadStatus getState() { return state; }
        public UploadStatus getLastTransferState() { return lastState; }
        public boolean isChatEnabled() { return chat; }
        public boolean isBrowseHostEnabled() { return browse; }
        public int getGnutellaPort() { return gPort; }
        public String getUserAgent() { return agent; }
        public int getQueuePosition() { return -1; }
        public boolean isInactive() { return true; }
        public void measureBandwidth() {  }
        public float getMeasuredBandwidth() { return mBand; }
        public float getAverageBandwidth() { return aBand; }
        public String getCustomIconDescriptor() { return iconDesc; }
        public UploadType getUploadType() { return UploadType.SHARED_FILE; }
        public boolean isTLSCapable() { return tlsCapable; }
        public String getAddress() { return addr; }
        public InetAddress getInetAddress() { return inetAddr; }
        public int getPort() { return port; }
        public InetSocketAddress getInetSocketAddress() { return inetSocketAddr; }
        public String getAddressDescription() { return inetSocketAddr.toString(); }

        @Override
        public File getFile() {
            return fd.getFile();
        }
        
        @Override
        public URN getUrn() {
            return fd.getSHA1Urn();
        }

        @Override
        public int getNumUploadConnections() {
            return 0;
        }
	}
}
