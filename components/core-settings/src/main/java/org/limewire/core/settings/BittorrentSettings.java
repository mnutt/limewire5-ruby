package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.ProbabilisticBooleanSetting;

/**
 * BitTorrent settings.
 */
public class BittorrentSettings extends LimeProps {
    private BittorrentSettings() {
        // empty constructor
    }

    /**
     * minimum tracker reask delay in seconds that we will use
     */
    public static IntSetting TRACKER_MIN_REASK_INTERVAL = FACTORY
            .createIntSetting("TRACKER_MIN_REASK_INTERVAL", 5 * 60);

    /**
     * maximum tracker reask delay in seconds
     */
    public static IntSetting TRACKER_MAX_REASK_INTERVAL = FACTORY
            .createIntSetting("TRACKER_MAX_REASK_INTERVAL", 2 * 60 * 60);

    /**
     * maximum uploads per torrent
     */
    public static IntSetting TORRENT_MAX_UPLOADS = FACTORY.createIntSetting(
            "TORRENT_MAX_UPLOADS", 6);

    /**
     * the number of uploads to allow to random hosts, ignoring tit-for-tat
     */
    public static IntSetting TORRENT_MIN_UPLOADS = FACTORY.createIntSetting(
            "TORRENT_MIN_UPLOADS", 4);
    
    /**
     * Whether to flush written blocks to disk before verifying them
     */
    public static BooleanSetting TORRENT_FLUSH_VERIRY = 
        FACTORY.createBooleanSetting("TORRENT_FLUSH_VERIFY", false);
    
    /**
     * Whether to use memory mapped files for disk access.
     */
    public static BooleanSetting TORRENT_USE_MMAP =
        FACTORY.createBooleanSetting("TORRENT_USE_MMAP", false);
    
    /**
     * Whether to report Disk problems to the bug server
     */
    public static ProbabilisticBooleanSetting REPORT_DISK_PROBLEMS =
        FACTORY.createRemoteProbabilisticBooleanSetting("REPORT_BT_DISK_PROBLEMS", 
                0f, "BTSettings.reportDiskProblems",0f,1f);
    
    /**
     * Whether to auto publish the peer as someone sharing the torrent in DHT.
     */
    public static BooleanSetting TORRENT_AUTO_PUBLISH = 
        FACTORY.createRemoteBooleanSetting("TORRENT_AUTO_PUBLISH", true, "BTSettings.disableTorrentAutoPublish");
    
    /**
     * Whether to perform auto lookup in DHT for an alternation location
     * when there is a tracker failure.
     */
    public static BooleanSetting TORRENT_ALTLOC_SEARCH = 
        FACTORY.createRemoteBooleanSetting("TORRENT_ALTLOC_SEARCH", true, "BTSettings.disableTorrentAltLocSearch");

}
