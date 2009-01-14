package org.limewire.core.settings;

import java.io.File;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FileSetSetting;
import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.ProbabilisticBooleanSetting;
import org.limewire.setting.StringArraySetting;

/**
 * Settings for downloads
 */
public class DownloadSettings extends LimeProps {
    private DownloadSettings() {}
                                                        
	/**
	 * Setting for the number of bytes/second to allow for all uploads.
	 */
	public static final IntSetting DOWNLOAD_SPEED =
		FACTORY.createIntSetting("DOWNLOAD_SPEED", 100);
    
    /**
	 * The maximum number of downstream bytes per second ever passed by
	 * this node.
	 */
    public static final IntSetting MAX_DOWNLOAD_BYTES_PER_SEC =
        FACTORY.createExpirableIntSetting("MAX_DOWNLOAD_BYTES_PER_SEC", 0);
    
    /**
	 * The maximum number of simultaneous downloads to allow.
	 */
    public static final IntSetting MAX_SIM_DOWNLOAD =
        FACTORY.createIntSetting("MAX_SIM_DOWNLOAD", 10);
    
    /**
     * Enable/disable skipping of acks
     */
    public static final BooleanSetting SKIP_ACKS =
        FACTORY.createRemoteBooleanSetting("SKIP_ACKS",true,"skip_acks");
    
    /**
     * various parameters of the formulas for skipping acks.
     */
    public static final IntSetting MAX_SKIP_ACKS =
        FACTORY.createRemoteIntSetting("MAX_SKIP_ACKS",5,"max_skip_ack",2,15);
    
    public static final FloatSetting DEVIATION =
        FACTORY.createRemoteFloatSetting("SKIP_DEVIATION",1.3f,"skip_deviation",1.0f,2.0f);
    
    public static final IntSetting PERIOD_LENGTH =
        FACTORY.createRemoteIntSetting("PERIOD_LENGTH",500,"period_length",100,2000);
    
    public static final IntSetting HISTORY_SIZE=
        FACTORY.createRemoteIntSetting("HISTORY_SIZE",10,"history_size",2,50);
    
    /**
     * Whether the client should use HeadPings when ranking sources
     */
    public static final BooleanSetting USE_HEADPINGS =
        FACTORY.createRemoteBooleanSetting("USE_HEADPINGS",true,"use_headpings");
    
    /**
     * Whether the client should drop incoming HeadPings.
     */
    public static final BooleanSetting DROP_HEADPINGS =
        FACTORY.createRemoteBooleanSetting("DROP_HEADPINGS",false,"drop_headpings");
    
    /**
     * We should stop issuing HeadPings when we have this many verified sources
     */
    public static final IntSetting MAX_VERIFIED_HOSTS = 
        FACTORY.createRemoteIntSetting("MAX_VERIFIED_HOSTS",1,"max_verified_hosts",0,5);
    
    /**
     * We should not schedule more than this many head pings at once
     */
    public static final IntSetting PING_BATCH =
        FACTORY.createRemoteIntSetting("PING_BATCH",10,"PingRanker.pingBatch",1,50);
    
    /**
     * Do not start new workers more than this often
     */
    public static final IntSetting WORKER_INTERVAL =
        FACTORY.createIntSetting("WORKER_INTERVAL",2000);
    
    /** The maximum number of headers we'll read when parsing a download */
    public static final IntSetting MAX_HEADERS =
        FACTORY.createRemoteIntSetting("MAX_DOWNLOAD_HEADERS", 20, "download.maxHeaders", 5, 50);
    
    /** The maximum size of a single header we'll read when parsing a download. */
    public static final IntSetting MAX_HEADER_SIZE =
        FACTORY.createRemoteIntSetting("MAX_DOWWNLOAD_HEADER_SIZE", 2048, "download.maxHeaderSize", 512, 5096);
    
    /**
     * Use a download SelectionStrategy tailored for previewing if the file's extension is
     * in this list.
     */
    private static String[] defaultPreviewableExtensions = {"html", "htm", "xml", "txt", "rtf", "tex",
        "mp3", "mp4", "wav", "au", "aif", "aiff", "ra", "ram", "wma", "wmv", "midi", "aifc", "snd",
        "mpg", "mpeg", "asf", "qt", "mov", "avi", "mpe", "ogg", "rm", "m4a", "flac", "fla", "flv"};
    public static final StringArraySetting PREVIEWABLE_EXTENSIONS = 
        FACTORY.createRemoteStringArraySetting("PREVIEWABLE_EXTENSIONS", 
                defaultPreviewableExtensions,
                "PREVIEWABLE_EXTENSIONS");
    
    /** Whether to report disk problems to the bug server */
    public static final ProbabilisticBooleanSetting REPORT_DISK_PROBLEMS =
        FACTORY.createRemoteProbabilisticBooleanSetting("REPORT_HTTP_DISK_PROBLEMS", 0f, 
                "DownloadSettings.reportDiskProblems", 0f, 1f);
    
    /**
     * Whether or not to remember recently completed downloads.
     */
    public static final BooleanSetting REMEMBER_RECENT_DOWNLOADS =
        FACTORY.createBooleanSetting("REMEMBER_RECENT_DOWNLOADS", true);
    
    /**
     * List of recent downloads.
     */
    public static final FileSetSetting RECENT_DOWNLOADS =
        FACTORY.createFileSetSetting("RECENT_DOWNLOADS", new File[0]);
    
        
}
