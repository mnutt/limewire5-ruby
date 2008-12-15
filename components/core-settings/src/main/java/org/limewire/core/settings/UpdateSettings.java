package org.limewire.core.settings;

import org.limewire.i18n.I18nMarker;
import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetSetting;

/**
 * Settings for messages
 */
public class UpdateSettings extends LimeProps {  
    private UpdateSettings() {}
    
    /**
     * Delay for showing message updates, in milliseconds.
     */
    public static final LongSetting UPDATE_DELAY =
        FACTORY.createRemoteLongSetting("UPDATE_DELAY", 24*60*60*1000,
            "updateDelay", 7*60*60*1000, 5*24*60*60*1000);
            
    /**
     * Delay for downloading updates, in milliseconds.
     */
    public static final LongSetting UPDATE_DOWNLOAD_DELAY =
        FACTORY.createRemoteLongSetting("UPDATE_DOWNLOAD_DELAY", 60*60*1000,
            "updateDownloadDelay", 30*60*1000, 77*60*60*1000);
    
    /**
     * How often to retry download any updates, in milliseconds.
     */
    public static final LongSetting UPDATE_RETRY_DELAY = 
        FACTORY.createRemoteLongSetting("UPDATE_RETRY_DELAY",30 * 60 * 1000,
                "updateRetryDelay", 15 * 60 * 1000, 2 * 60 * 60 * 1000); 
    
    /**
     * If this many times the initial delay passed since the update timestamp, we may
     * give up.
     */
    public static final IntSetting UPDATE_GIVEUP_FACTOR =
        FACTORY.createRemoteIntSetting("UPDATE_GIVEUP_FACTOR", 5, 
                "updateGiveUpFactor", 2, 50);
    
    /**
     * If we try downloading a given update more than this many times, we may give up.
     */
    public static final IntSetting UPDATE_MIN_ATTEMPTS =
        FACTORY.createRemoteIntSetting("UPDATE_MIN_ATTEMPTS", 500,
                "updateMinAttempts", 50, 2000);
            
    /**
     * The style of updates.
     */
    public static final IntSetting UPDATE_STYLE = 
        FACTORY.createIntSetting("UPDATE_STYLE", 1).setAlwaysSave(true);
    
    /**
     * Failed updates.
     */
    public static final StringSetSetting FAILED_UPDATES = 
        FACTORY.createStringSetSetting("FAILED_UPDATES","");
    
    /**
     * Pro ads.
     */
    public static final StringArraySetting PRO_ADS =
        FACTORY.createRemoteStringArraySetting("PRO_ADS",
                new String[] {
                I18nMarker.marktr("For Turbo-Charged searches get LimeWire PRO."),
                "http://www.limewire.com/index.jsp/pro&21",
                "0.111111",
                I18nMarker
                        .marktr("Support LimeWire\'s peer-to-peer development. Get PRO."),
                "http://www.limewire.com/index.jsp/pro&22",
                "0.111111",
                I18nMarker
                        .marktr("Purchase LimeWire PRO to help us make downloads faster."),
                "http://www.limewire.com/index.jsp/pro&23",
                "0.111111",
                I18nMarker.marktr("For Turbo-Charged downloads get LimeWire PRO."),
                "http://www.limewire.com/index.jsp/pro&24",
                "0.111111",
                I18nMarker.marktr("Support open networks. Get LimeWire PRO."),
                "http://www.limewire.com/index.jsp/pro&25",
                "0.111111",
                I18nMarker
                        .marktr("Support open source and open protocols. Get LimeWire PRO."),
                "http://www.limewire.com/index.jsp/pro&26",
                "0.111111",
                I18nMarker.marktr("For Turbo-Charged performance get LimeWire PRO."),
                "http://www.limewire.com/index.jsp/pro&27",
                "0.111111",
                I18nMarker.marktr("Keep the Internet open. Get LimeWire PRO."),
                "http://www.limewire.com/index.jsp/pro&28",
                "0.111111",
                I18nMarker.marktr("Developing LimeWire costs real money. Get PRO."),
                "http://www.limewire.com/index.jsp/pro&29",
                "0.111111"},
                "UpdateSettings.proAds");
    
    /**
     * The timestamp of the last update message we've received.
     */
    public static final LongSetting LAST_UPDATE_TIMESTAMP = 
        FACTORY.createLongSetting("LAST_UPDATE_TIMESTAMP", -1L);
    
    /**
     * The last time we checked the failover url for updates.
     */
    public static final LongSetting LAST_HTTP_FAILOVER = 
        FACTORY.createLongSetting("LAST_HTTP_FAILOVER", -1L);
    
    public static final LongSetting LAST_SIMPP_FAILOVER =
        FACTORY.createLongSetting("LAST_SIMPP_FAILOVER", -1);
}
