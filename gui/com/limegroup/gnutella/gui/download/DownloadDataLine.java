package com.limegroup.gnutella.gui.download;

import java.awt.Color;
import java.io.File;
import java.text.MessageFormat;

import javax.swing.Icon;

import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.IconManager;
import com.limegroup.gnutella.gui.dnd.FileTransfer;
import com.limegroup.gnutella.gui.dnd.LazyFileTransfer;
import com.limegroup.gnutella.gui.mp3.MediaPlayerComponent;
import com.limegroup.gnutella.gui.mp3.PlayListItem;
import com.limegroup.gnutella.gui.tables.AbstractDataLine;
import com.limegroup.gnutella.gui.tables.CenteredHolder;
import com.limegroup.gnutella.gui.tables.ChatHolder;
import com.limegroup.gnutella.gui.tables.ColoredCell;
import com.limegroup.gnutella.gui.tables.ColoredCellImpl;
import com.limegroup.gnutella.gui.tables.IconAndNameHolder;
import com.limegroup.gnutella.gui.tables.IconAndNameHolderImpl;
import com.limegroup.gnutella.gui.tables.LimeTableColumn;
import com.limegroup.gnutella.gui.tables.ProgressBarHolder;
import com.limegroup.gnutella.gui.tables.SizeHolder;
import com.limegroup.gnutella.gui.tables.SpeedRenderer;
import com.limegroup.gnutella.gui.tables.TimeRemainingHolder;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;

/**
 * This class handles all of the data for a single download, representing
 * one "line" in the download window.  It continually updates the
 * displayed data for the download from the contained <tt>Downloader</tt>
 * instance.
 */
public final class DownloadDataLine extends AbstractDataLine<Downloader>
                                    implements LazyFileTransfer {

	/**
	 * Constant for the "queued" download state.
	 */
	private static final String QUEUED_STATE =
		I18n.tr("Queued");

	/**
	 * Constant for the "connecting" download state.
	 */
	private static final String CONNECTING_STATE =
		I18n.tr("Connecting...");

	private static final String CONNECTING_STATE_TRIED_COUNT =
		I18n.tr("Connecting ({0} hosts tried)...");

	/**
	 * Constant for the "waiting" download state.
	 */
	private static final String WAITING_STATE =
		I18n.tr("Waiting On Busy Hosts");

	/**
	 * Constant for the "complete" download state.
	 */
	private static final String COMPLETE_STATE =
		I18n.tr("Complete");

	/**
	 * Constant for the "aborted" download state.
	 */
	private static final String ABORTED_STATE =
		I18n.tr("Aborted");

	/**
	 * Constant for the "failed" download state.
	 */
	private static final String FAILED_STATE =
		I18n.tr("Awaiting Sources");

	/**
	 * Constant for the "downloading" download state.
	 */
	private static final String DOWNLOADING_STATE =
		I18n.tr("Downloading from");

  	/**
	 * Constant for the "Could Not Move to Library" download state.
	 * TODO: change this to a more generic disk problem message
	 */
	private static final String LIBRARY_MOVE_FAILED_STATE =
		I18n.tr("Disk Problem");

  	/**
	 * Constant for the "Corrupt File" download state.
	 */
	private static final String CORRUPT_FILE_STATE =
		I18n.tr("File Corrupted");

  	/**
	 * Constant for the "Waiting for Results" download state.
	 */
	 // s stands for seconds
	private static final String REQUERY_WAITING_STATE_START = I18n.tr("Waiting {0}s for Sources");

  	/**
	 * Constant for the "Waiting for Results" download state.
	 */
	private static final String REQUERY_WAITING_FOR_USER = 
		I18n.tr("Need More Sources");
    
  	/**
	 * Constant for the "Waiting for Connections" download state.
	 */
	private static final String WAITING_FOR_CONNECTIONS_STATE = 
		I18n.tr("Waiting for Stable Connections");
    
    /**
     * Constant for the "Remote Queued" download state
     */
    private static final String REMOTE_QUEUED_STATE =
        I18n.tr("Waiting in Line, Position");

    /**
     * Constant for the "Hashing" download state
     */
    private static final String HASHING_STATE =
        I18n.tr("Verifying File Contents...");

    /**
     * Constant for the "Saving" download state
     */
    private static final String SAVING_STATE =
        I18n.tr("Saving File...");
        
    /**
     * Constant for the "Identifying Corruption" download state
     */
    private static final String IDENTIFY_CORRUPTION_STATE =
        I18n.tr("Recovering Corrupted File");
        
    /**
     * Constant for the "Pausing" download state.
     */
    private static final String PAUSING_STATE =
        I18n.tr("Pausing...");
    
    /**
     * Constant for the "Paused" download state.
     */
    private static final String PAUSED_STATE =
        I18n.tr("Paused");
    
    /** Constant for 'Invalid' download state. */
    private static final String INVALID_STATE =
        I18n.tr("Content Removed");
    
    /** Constant for 'Resuming' download state. */
    private static final String RESUMING_STATE =
        I18n.tr("Resuming...");
    
    /** Constant for 'Fetching' download state. */
    private static final String FETCHING_STATE =
    	I18n.tr("Downloading .torrent file...");

    /**
     * Constant for "average bandwidth" tip
     */
    private static final String AVERAGE_BANDWIDTH =
        I18n.tr("Average Bandwidth");

    /**
     * Constant for the "alternate locations" tip
     */
    private static final String ALTERNATE_LOCATIONS =
        I18n.tr("Valid Alternate Locations");

    /**
     * Constant for the "invalid alternate locations" tip
     */
    private static final String INVALID_ALTERNATE_LOCATIONS =
        I18n.tr("Invalid Alternate Locations");

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
     * Constant for "Chunks" tip
     */
	private static final String CHUNKS =
		I18n.tr("Chunks");
	
	/**
     * Constant for "Lost By Corruption" tip
     */
	private static final String LOST =
		I18n.tr("Lost To Corruption");
	
	/**
     * Constant for "KB" tip word
     */
	private static final String KB =
		I18n.tr("KB");

    /**
     * Constant for the " hosts" message.
     */
    private static final String HOSTS_LABEL =
        I18n.tr("hosts");

    private static final String HOST_LABEL = 
        I18n.tr("host");
        
    /**
     * Constant for the 'gave up' tooltip message.
     */
    private static final String[] GAVE_UP_MESSAGE =
        { I18n.tr("A more specific search may be required...") };
    
    /** Constant for the 'invalid' tooltip msg. */
    private static final String[] INVALID_MESSAGE =
        { I18n.tr("The owner of this content has requested that this file be removed from the network. To learn more, double-click on the download (or click preview).") }; 
        
    /**
     * Constant for the number of possible hosts this downloader has
     */
    private static final String POSSIBLE_HOSTS = 
        I18n.tr("Possible Hosts");

    private static final String BUSY_HOSTS = 
        I18n.tr("Busy Hosts");

    private static final String QUEUED_HOSTS = 
        I18n.tr("Remotely Queued");

    private static final String DOWNLOAD_SEARCHING =
        I18n.tr("Locating Sources...");
    
    private static final String TRACKER_FAILURE_REASON =
        I18n.tr("LimeWire cannot download this torrent.  The tracker responded");

	/**
	 * Variable for the name of the file being downloaded.
	 */
	private String _fileName;

	/**
	 * Variable for the status of the download.
	 */
	private String _status;

    /**
	 * Variable for the amount of the file that has been read.
	 */
	private long _amountRead = 0;

	/**
	 * Variable for the progress made in the progressbar.
	 */
	private int _progress;

	/**
	 * Variable for whether or not chat is enabled.
	 */
	private boolean _chatEnabled;

	/**
	 * Variable for whether or not browse is enabled.
	 */
	private boolean _browseEnabled;

	/**
	 * Variable for the size of the download.
	 */
	private long _size = -1;

	/**
	 * Variable for the speed of the download.
	 */
	private double _speed;

	/**
	 * Variable for how much time is left.
	 */
	private int _timeLeft;

	/**
	 * Variable for the time this download started
	 */
	private long _startTime;

	/**
	 * Variable for the time this download ended.
	 */
	private long _endTime;
    
    /**
     * The current vendor we are downloading from.
     */
    private String _vendor;
	
	/**
	 * Stores the current state of this download, as of the last update.
	 * This is the state the everything should work off of to avoid the
	 * <tt>Downloader</tt> instance being in a different state than
	 * this data line.
	 */
	private DownloadStatus _state;
	
	/**
	 * Whether or not we've cleaned up this line.
	 */
	private boolean _cleaned = false;
	
	/**
     * The colors for cells.
     */
    private Color _cellColor;
    private Color _othercellColor;
	
	/**
	 * Column index for priority.
	 */
	static final int PRIORITY_INDEX = 0;
	private static final LimeTableColumn PRIORITY_COLUMN =
	    new LimeTableColumn(PRIORITY_INDEX, "DOWNLOAD_PRIORITY_COLUMN", I18n.tr("Priority"),
	                40, false, CenteredHolder.class);

	/**
	 * Column index for the file name.
	 */
	static final int FILE_INDEX = 1;
	private static final LimeTableColumn FILE_COLUMN =
	    new LimeTableColumn(FILE_INDEX, "DOWNLOAD_NAME_COLUMN", I18n.tr("Name"),
	                201, true, ColoredCell.class);

	/**
	 * Column index for the file size.
	 */
	static final int SIZE_INDEX = 2;
	private static final LimeTableColumn SIZE_COLUMN =
	    new LimeTableColumn(SIZE_INDEX, "DOWNLOAD_SIZE_COLUMN", I18n.tr("Size"),
	                65, true, SizeHolder.class);

	/**
	 * Column index for the file download status.
	 */
	static final int STATUS_INDEX = 3;
	private static final LimeTableColumn STATUS_COLUMN =
	    new LimeTableColumn(STATUS_INDEX, "DOWNLOAD_STATUS_COLUMN", I18n.tr("Status"),
	                152, true, String.class);

	/**
	 * Column index for whether or not the uploader is chat-enabled.
	 */
	static final int CHAT_INDEX = 4;
	private static final LimeTableColumn CHAT_COLUMN =
	    new LimeTableColumn(CHAT_INDEX, "DOWNLOAD_CHAT_COLUMN", I18n.tr("Chat"),
	                10, false, ChatHolder.class);

	/**
	 * Column index for the progress of the download.
	 */
	static final int PROGRESS_INDEX = 5;
	private static final LimeTableColumn PROGRESS_COLUMN =
	    new LimeTableColumn(PROGRESS_INDEX, "DOWNLOAD_PROGRESS_COLUMN", I18n.tr("Progress"),
	                71, true, ProgressBarHolder.class);

	/**
	 * Column index for the download speed.
	 */
	static final int SPEED_INDEX = 6;
	private static final LimeTableColumn SPEED_COLUMN =
	    new LimeTableColumn(SPEED_INDEX, "DOWNLOAD_SPEED_COLUMN", I18n.tr("Speed"),
	                58, true, SpeedRenderer.class);

	/**
	 * Column index for the download time remaining.
	 */
	static final int TIME_INDEX = 7;
	private static final LimeTableColumn TIME_COLUMN =
	    new LimeTableColumn(TIME_INDEX, "DOWNLOAD_TIME_REMAINING_COLUMN", I18n.tr("Time"),
	                49, true, TimeRemainingHolder.class);
	    
    /**
     * Column index for the vendor of the downloader.
     */
    static final int VENDOR_INDEX = 8;
    private static final LimeTableColumn VENDOR_COLUMN =
        new LimeTableColumn(VENDOR_INDEX, "DOWNLOAD_SERVER_COLUMN", I18n.tr("Vendor/Version"),
                    20, false, String.class);
	
	/**
	 * Number of columns to display
	 */
	static final int NUMBER_OF_COLUMNS = 9;
	
	// Implements DataLine interface
	public int getColumnCount() { return NUMBER_OF_COLUMNS; }

	/**
	 * Must initialize data.
	 *
	 * @param downloader the <tt>Downloader</tt>
	 *  that provides access to
	 *  information about the download
	 */
	@Override
    public void initialize(Downloader downloader) {
	    super.initialize(downloader);
		_startTime = System.currentTimeMillis();
		_endTime = -1;
		_size = initializer.getContentLength();
		// don't cache filename anymore, since we allow renames henceforth
		_fileName  = initializer.getSaveFile().getName();
		if (_fileName==null) //TODO: does this ever happen with an downloader?
			_fileName="";
		_status = "";
		_chatEnabled = false;
		_browseEnabled = false;
		initColors();
		update();
	}

	/**
	 * Tell the downloader to close its sockets.
	 */
	@Override
    public void cleanup() {
	    BackgroundExecutorService.schedule(new Runnable() {
	        public void run() {
	            initializer.stop(false);
            }
        });
	    _cleaned = true;
    }
    
    /**
     * Determines if this was cleaned up.
     */
    public boolean isCleaned() {
        return _cleaned;
    }
    
    /**
     * Gets the file if the download was completed.
     */
    public File getFile() {
        if(!OSUtils.isWindows())
            return initializer.getFile();
        else {
            if(initializer.isCompleted())
                return initializer.getFile();
            else
                return null;
        }
    }
    
    /**
     * Lazily gets the file -- constructs it only if necessary.
     */
    public FileTransfer getFileTransfer() {
        return new FileTransfer() {
            public File getFile() {
                return initializer.getDownloadFragment();
            }
        };
    }
    
	/**
	 * Returns the <tt>Object</tt> stored at the specified column in this
	 * line of data.
	 *
	 * @param index the index of the column to retrieve data from
	 * @return the <tt>Object</tt> stored at that index
	 * @implements DataLine interface
	 */
	public Object getValueAt(int index) {
		switch(index) {
		case PRIORITY_INDEX:
		    if(initializer.isPaused())
		        return PriorityHolder.PAUSED_P;
		    if(initializer.isInactive())
		        return new PriorityHolder(initializer.getInactivePriority());
            else if(initializer.isCompleted())
                return PriorityHolder.COMPLETE_P;
            else
                return PriorityHolder.ACTIVE_P;
		case FILE_INDEX:
            PlayListItem currentPlaying = MediaPlayerComponent.getInstance().getCurrentSong();
            Color color;
            if( currentPlaying == null )
                color = getColor(false);
            else
                color = getColor(currentPlaying.getName().contains(_fileName));
		    return new ColoredCellImpl(new IconAndNameHolderImpl(getIcon(), _fileName), color, IconAndNameHolder.class);
		case SIZE_INDEX:
			return new SizeHolder(_size);
		case STATUS_INDEX:
			return _status;
		case CHAT_INDEX:
			return _chatEnabled ? Boolean.TRUE : Boolean.FALSE;
		case PROGRESS_INDEX:
			return Integer.valueOf(_progress);
		case SPEED_INDEX:
			return new Double(_speed);
        case TIME_INDEX:
            return new TimeRemainingHolder(_timeLeft);
        case VENDOR_INDEX:
            return _vendor;
        }
		return null;
	}

	/**
	 * @implements DataLine interface
	 */
	public LimeTableColumn getColumn(int idx) {
	    switch(idx) {
	        case PRIORITY_INDEX: return PRIORITY_COLUMN;
    	    case FILE_INDEX:     return FILE_COLUMN;
    	    case SIZE_INDEX:     return SIZE_COLUMN;
    	    case STATUS_INDEX:   return STATUS_COLUMN;
    	    case CHAT_INDEX:     return CHAT_COLUMN;
    	    case PROGRESS_INDEX: return PROGRESS_COLUMN;
    	    case SPEED_INDEX:    return SPEED_COLUMN;
    	    case TIME_INDEX:     return TIME_COLUMN;
    	    case VENDOR_INDEX:   return VENDOR_COLUMN;
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
    public boolean isTooltipRequired(int col) {
        return _state == DownloadStatus.INVALID;
    }
	
	@Override
    public String[] getToolTipArray(int col) {
	    // give a new message if we gave up
	    if( _state == DownloadStatus.GAVE_UP )
	        return GAVE_UP_MESSAGE;
        
        if(_state == DownloadStatus.INVALID )
            return INVALID_MESSAGE;
        
        if (_state == DownloadStatus.WAITING_FOR_USER) {
            String custom = (String)initializer.getAttribute(Downloader.CUSTOM_INACTIVITY_KEY);
            if (custom != null)
                return new String[]{TRACKER_FAILURE_REASON,custom};
        }
	    	    
	    String[] info = new String[11];
	    String bandwidth = AVERAGE_BANDWIDTH + ": " + GUIUtils.rate2speed(
	        initializer.getAverageBandwidth()
	    );
	    String numHosts = POSSIBLE_HOSTS + ": " + 
	                     initializer.getPossibleHostCount();
        String busyHosts = BUSY_HOSTS + ": " +initializer.getBusyHostCount();
        String queuedHosts=QUEUED_HOSTS + ": "+initializer.getQueuedHostCount();
	    String numLocs = ALTERNATE_LOCATIONS + ": " +
	                     initializer.getNumberOfAlternateLocations();
        String numInvalidLocs = INVALID_ALTERNATE_LOCATIONS + ": " +
                         initializer.getNumberOfInvalidAlternateLocations();
		int chunkSize = 0;
		String numChunks = null;
		String lost;
		// DPINJ: pass in the shared disk controller!!!
        int totalPending = GuiCoreMediator.getDiskController().getNumPendingItems();
		synchronized(initializer) {
			if (_endTime == -1) {
				chunkSize = initializer.getChunkSize();
				numChunks = CHUNKS + ": "+initializer.getAmountVerified() / chunkSize +"/"+
				initializer.getAmountRead() / chunkSize+ "["+ 
				initializer.getAmountPending()+"|"+totalPending+"]"+ 
				"/"+
				initializer.getContentLength() / chunkSize+
				", "+chunkSize/1024+KB;
		
			}
		 	lost = LOST+": "+initializer.getAmountLost()/1024+KB;
		}

        info[0] = STARTED_ON + " " + GUIUtils.msec2DateTime( _startTime );
	    if( _endTime != -1 ) {
	        info[1] = FINISHED_ON + " " + GUIUtils.msec2DateTime( _endTime );
	        info[2] = TIME_SPENT + ": " + CommonUtils.seconds2time(
	            (int)((_endTime - _startTime) / 1000 ) );
	        info[3] = "";
	        info[4] = bandwidth;
	        info[5] = numHosts;
            info[6] = busyHosts;
            info[7] = queuedHosts;
	        info[8] = numLocs;
            info[9] = numInvalidLocs;
			info[10] = lost;
	    } else {
	        info[1] = TIME_SPENT + ": " + CommonUtils.seconds2time(
	            (int) ((System.currentTimeMillis() - _startTime) / 1000 ) );
	        info[2] = "";
	        info[3] = bandwidth;
	        info[4] = numHosts;
            info[5] = busyHosts;
            info[6] = queuedHosts;
	        info[7] = numLocs;
            info[8] = numInvalidLocs;
			info[9] = numChunks;
			info[10] = lost;}

	    return info;
	}

	public boolean isDynamic(int idx) {
	    switch(idx) {
	        case PRIORITY_INDEX:
	        case STATUS_INDEX:
	        case PROGRESS_INDEX:
	        case SPEED_INDEX:
	        case TIME_INDEX:
	        case VENDOR_INDEX:
	            return true;
	    }
	    return false;
	}

	/**
	 * Returns the total size in bytes of the file being downloaded.
	 *
	 * @return the total size in bytes of the file being downloaded
	 */
	long getLength() {
		return _size;
	}

    /**
     * Returns name of the file being downloaded.
     * @return name of the downloaded file.
     */
    public String getFileName() {
        return _fileName;
    }
    
	/**
	 * Returns whether or not the <tt>Downloader</tt> for this download
	 * is equal to the one passed in.
	 *
	 * @return <tt>true</tt> if the passed-in downloader is equal to the
	 *  <tt>Downloader</tt> for this download, <tt>false</tt> otherwise
	 */
	boolean containsDownloader(Downloader downloader) {
		return initializer.equals(downloader);
	}

	/**
	 * Returns the <tt>Downloader</tt> associated with this download.
	 *
	 * @return the <tt>Downloader</tt> associated with this download
	 */
	Downloader getDownloader() {
		return initializer;
	}

	/**
	 * Return the state of the Downloader
	 *
	 * @return the state of the downloader
	 */
    DownloadStatus getState() {
	    return _state;
	}
    
    /**
     * Return the speed of the Download
     *
     * @return the speed of the download
     */
    double getSpeed() {
        return _speed;
    }

	/**
	 * Returns whether or not the download has completed.
	 *
	 * @return <tt>true</tt> if the download is complete, <tt>false</tt> otherwise
	 */
	boolean isCompleted() {
		return _state == DownloadStatus.COMPLETE;
	}

	/**
	 * Returns whether or not chat is enabled for this download.
	 *
	 * @return <tt>true</tt> if the host we're downloading from is chattable,
	 *  <tt>false</tt> otherwise
	 */
	boolean getChatEnabled() {
		return _chatEnabled;
	}

	/**
	 * Returns whether or not browse is enabled for this download.
	 *
	 * @return <tt>true</tt> if the host we're downloading from is browsable,
	 *  <tt>false</tt> otherwise
	 */
	boolean getBrowseEnabled() {
		return _browseEnabled;
	}

	private Icon getIcon() {
	    if (initializer.getCustomIconDescriptor() == Downloader.BITTORRENT_DOWNLOAD)
	        return GUIMediator.getThemeImage("bittorrent_download");
	    else
	        return IconManager.instance().getIconForFile(initializer.getFile());
	}

	/**
	 * Updates all of the data for this download, obtaining fresh information
	 * from the contained <tt>Downloader</tt> instance.
	 *
	 * @implements DataLine interface
	 */
	@Override
    public void update() {
		synchronized(initializer) {
		// always get new file name it might have changed
		_fileName = initializer.getSaveFile().getName();
	    _speed = -1;
	    _size = initializer.getContentLength();
		_amountRead = initializer.getAmountRead();
		_chatEnabled = initializer.hasChatEnabledHost();
        _browseEnabled = initializer.hasBrowseEnabledHost();
        _timeLeft = 0;
        //note: we *always* want to update progress
        // specifically for when the user has downloaded stuff,
        // closed the app, and then re-opened the app.
        //previously, because progress was only set while downloading
        //or corrupted, the GUI would display 0 progress, even
        //though it actually had progress.
		double d = (double)_amountRead/(double)_size;
		_progress = (int)(d*100);
		this.updateStatus();
		// downloads can go from inactive to active through resuming.
		if ( !this.isInactive() ) _endTime = -1;
	}
	}


	/**
	 * Updates the status of the download based on the state stored in the
	 * <tt>Downloader</tt> instance for this <tt>DownloadDataLine</tt>.
	 */
	private void updateStatus() {
	    final String lastVendor = _vendor;
	    _vendor = "";
		_state = initializer.getState();
		boolean paused = initializer.isPaused();
		if(paused && _state != DownloadStatus.PAUSED && !initializer.isCompleted()) {
		    _status = PAUSING_STATE;
		    return;
		}
		
		switch (_state) {
		case QUEUED:
			_status = QUEUED_STATE;
			break;
		case CONNECTING:
			int triedCount = initializer.getTriedHostCount();
			if (triedCount < 15) {
				_status = CONNECTING_STATE;
			}
			else { 
				_status = MessageFormat.format(CONNECTING_STATE_TRIED_COUNT, 
						new Object[] { triedCount });
			}
			break;
		case BUSY:
			_status = WAITING_STATE;
			break;
	    case HASHING:
	        _status = HASHING_STATE;
	        break;
	    case SAVING:
	        _status = SAVING_STATE;
	        break;
		case COMPLETE:
            _status = COMPLETE_STATE;
			_progress = 100;
			break;
		case ABORTED:
			_status = ABORTED_STATE;
			break;
		case GAVE_UP:
			_status = FAILED_STATE;
			break;
        case IDENTIFY_CORRUPTION:
			_status = IDENTIFY_CORRUPTION_STATE;
 			break;
        case RECOVERY_FAILED:
            _status = "Recovery Failed";
            break;
		case DOWNLOADING:
		    _vendor = lastVendor;
		    updateHostCount(initializer);
            try {
                _speed = initializer.getMeasuredBandwidth();
            } catch(InsufficientDataException ide) {
                _speed = 0;
            }
            // If we have a valid rate (can't compute if rate is 0),
            // then determine how much time (in seconds) is remaining.
            if ( _speed > 0) {
                double kbLeft = ((_size/1024.0) -
								 (_amountRead/1024.0));
                _timeLeft = (int)(kbLeft / _speed);
            }
			break;
		case DISK_PROBLEM:
			_status = LIBRARY_MOVE_FAILED_STATE;
			_progress = 100;
			break;
        case CORRUPT_FILE:
            _status = CORRUPT_FILE_STATE;
            break;
        case WAITING_FOR_GNET_RESULTS:
			int stateTime=initializer.getRemainingStateTime();
			_status = MessageFormat.format(REQUERY_WAITING_STATE_START, stateTime);
            break;
        case ITERATIVE_GUESSING:
        case QUERYING_DHT:
            _status = DOWNLOAD_SEARCHING;
            break;
        case WAITING_FOR_USER:
            _status = REQUERY_WAITING_FOR_USER;
            break;
        case WAITING_FOR_CONNECTIONS:
            _status = WAITING_FOR_CONNECTIONS_STATE;
            break;
        case REMOTE_QUEUED:
            _status = REMOTE_QUEUED_STATE+" "+initializer.getQueuePosition();
            _vendor = initializer.getVendor();  
            updateVendor();
            break;
        case PAUSED:
            _status = PAUSED_STATE;
            break;
        case INVALID:
            _status = INVALID_STATE;
            break;
        case RESUMING:
        	_status = RESUMING_STATE;
        	break;
        case FETCHING:
        	_status = FETCHING_STATE;
        	break;
		default:
		    throw new IllegalStateException("Unknown status "+initializer.getState()+" of downloader");
		}
	}

    /**
     * Returns a human-readable description of the address(es) from
     * which d is downloading.
     */
    private void updateHostCount(Downloader d) {
        int count = d.getNumHosts();

        // we are in between chunks with this host,
        // use the previous count so-as not to confuse
        // the user.
        if (count == 0) {
            // don't change anything.
            return;
        }
        
        if (count==1) {
            _status = DOWNLOADING_STATE + " " + count + " "+ HOST_LABEL; 
            _vendor = d.getVendor();
        } else {
            _status = DOWNLOADING_STATE + " " +  count + " " + HOSTS_LABEL;
            _vendor = d.getVendor();
        }
        updateVendor();
    }
    
    private void updateVendor() {
    	if (_vendor == Downloader.BITTORRENT_DOWNLOAD)
        	_vendor = I18n.tr("BitTorrent");
    }

	/**
	 * Returns whether or not this download is in what
	 * is considered an "inactive"
	 * state, such as completeed, aborted, failed, etc.
	 *
	 * @return <tt>true</tt> if this download is in an inactive state,
	 *  <tt>false</tt> otherwise
	 */
	boolean isInactive() {
		return (_state == DownloadStatus.COMPLETE ||
				_state == DownloadStatus.ABORTED ||
				_state == DownloadStatus.GAVE_UP ||
				_state == DownloadStatus.DISK_PROBLEM ||
                _state == DownloadStatus.CORRUPT_FILE);
	}
	
	/**
	 * Determines if the downloader is in what it considers an inactive state.
	 */
	boolean isDownloaderInactive() {
	    return initializer.isInactive();
	}

	/**
	 * Returns whether or not the
	 * download for this line is currently downloading
	 *
	 * @return <tt>true</tt> if this download is currently downloading,
	 *  <tt>false</tt> otherwise
	 */
	boolean isDownloading() {
		return _state == DownloadStatus.DOWNLOADING;
	}

	/**
	 * Sets the time this download ended.
	 */
	void setEndTime(long time) {
	    _endTime = time;
	}
	
	private void initColors() {
	    _cellColor = ThemeFileHandler.WINDOW8_COLOR.getValue();
        _othercellColor = ThemeFileHandler.SEARCH_RESULT_SPEED_COLOR.getValue();
	}
	
	private Color getColor(boolean playing) {
        return playing ? _othercellColor : _cellColor;
    }


}
