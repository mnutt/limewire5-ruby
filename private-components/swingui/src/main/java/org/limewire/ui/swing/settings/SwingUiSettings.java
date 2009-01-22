package org.limewire.ui.swing.settings;

import java.io.File;

import org.limewire.core.settings.LimeProps;
import org.limewire.i18n.I18nMarker;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FileSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetSetting;
import org.limewire.setting.StringSetting;
import org.limewire.util.CommonUtils;
import org.limewire.util.OSUtils;


/**
 * Settings to deal with UI.
 */ 
public final class SwingUiSettings extends LimeProps {
    
    private SwingUiSettings() {}    
    
    /**
     * This setting is used to track whether or not the user wants to show offline buddies in the left panel.
     */
    public static final BooleanSetting XMPP_SHOW_OFFLINE =
        (BooleanSetting)FACTORY.createBooleanSetting("XMPP_SHOW_OFFLINE", true).setPrivate(true);

    /**
     * Whether or not 'REMEMBER_ME' is checked -- this has nothing to do with
     * auto logging in. It's only the state of the REMEMBER ME checkbox, so if
     * you uncheck it it doesn't stay checked on the next login.
     */
    public static final BooleanSetting REMEMBER_ME_CHECKED =
        FACTORY.createBooleanSetting("REMEMBER_ME_XMPP", true);

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
    
    /** Show classic warning. */
    public static final BooleanSetting SHOW_CLASSIC_REMINDER =
        FACTORY.createBooleanSetting("SHOW_CLASSIC_SEARCH_REMINDER", true);

    /**
     * Setting for whether or not to group similar results
     */
    public static final BooleanSetting GROUP_SIMILAR_RESULTS_ENABLED =
        FACTORY.createBooleanSetting("GROUP_SIMILAR_RESULTS_ENABLED", true);

    /**
     * Setting for whether to display search tips based on file in 
     * friends libraries. If true, tips should be displayed, if false
     * they should not. 
     */
    public static final BooleanSetting SHOW_FRIEND_SUGGESTIONS =
        FACTORY.createBooleanSetting("SHOW_FRIEND_SUGGESTIONS", true);

    /**
     * Setting for whether to display old searches as search tips. If true,
     * old search will be displayed as tips, if false they won't be displayed
     */
    public static final BooleanSetting KEEP_SEARCH_HISTORY =
        FACTORY.createBooleanSetting("KEEP_SEARCH_HISTORY", true);

    /**
     * The default search category for the search bar.
     */
    public static final IntSetting DEFAULT_SEARCH_CATEGORY_ID =
        FACTORY.createIntSetting("DEFAULT_SEARCH_CATEGORY_ID", -1);

    /**
     * The default search view, list versus classic.
     */
    public static final IntSetting SEARCH_VIEW_TYPE_ID =
        FACTORY.createIntSetting("SEARCH_VIEW_TYPE_ID", -1);

    /**
     * Auto rename new downloads with filenames matching old downloads.
     */
    public static final BooleanSetting AUTO_RENAME_DUPLICATE_FILES =
        FACTORY.createBooleanSetting("AUTO_RENAME_DUPLICATE_FILES", true);

    /**
     * Setting whether LimeWire should manage the BT settings
     * automatically.
     */
    public static BooleanSetting AUTOMATIC_SETTINGS = 
        FACTORY.createBooleanSetting("BT_AUTOMATIC_SETTINGS", true);

    /** True if any positions have been set. */
    @InspectablePrimitive("application positions set")
    public static final BooleanSetting POSITIONS_SET =
        FACTORY.createBooleanSetting("POSITIONS_SET", false);

    /**
     * The width that the application should be.
     */
    @InspectablePrimitive("application width")
    public static final IntSetting APP_WIDTH =
        FACTORY.createIntSetting("APP_WIDTH_V5", 1024);

    /**
     * The height that the application should be.
     */
    @InspectablePrimitive("application height")
    public static final IntSetting APP_HEIGHT =
        FACTORY.createIntSetting("APP_HEIGHT_V5", 768);

    /**
     * The x position of the window for the next time the application
     * is started.
     */
    public static final IntSetting WINDOW_X =
        FACTORY.createIntSetting("WINDOW_X_V5", 0).setAlwaysSave(true);

    /**
     * The y position of the window for the next time the application
     * is started.
     */
    public static final IntSetting WINDOW_Y =
        FACTORY.createIntSetting("WINDOW_Y_V5", 0).setAlwaysSave(true);

    /** Setting for whether or not LW should start maximized. */
    @InspectablePrimitive("is application maximized")
    public static final BooleanSetting MAXIMIZE_WINDOW =
        FACTORY.createBooleanSetting("MAXIMIZE_WINDOW_V5", false);

    /**
     * A flag for whether or not the application should be minimized
     * to the system tray on windows.
     */
    public static final BooleanSetting MINIMIZE_TO_TRAY =
        FACTORY.createBooleanSetting("MINIMIZE_TO_TRAY", 
            OSUtils.supportsTray());

    /**
     * Whether LimeWire should handle magnets.
     */
    public static final BooleanSetting HANDLE_MAGNETS = 
    	FACTORY.createBooleanSetting("HANDLE_MAGNETS", true);

    /**
     * Whether LimeWire should handle torrents.
     */
    public static final BooleanSetting HANDLE_TORRENTS = 
    	FACTORY.createBooleanSetting("HANDLE_TORRENTS", true);

    /**
     * Whether LimeWire should warn user about file association changes.
     */
    public static final BooleanSetting WARN_FILE_ASSOCIATION_CHANGES = 
        FACTORY.createBooleanSetting("WARN_FILE_ASSOCIATION_CHANGES", true);

    /** The last directory used for opening a file chooser. */
    public static final FileSetting LAST_FILECHOOSER_DIRECTORY =
        FACTORY.createFileSetting("LAST_FILECHOOSER_DIR", getDefaultLastFileChooserDir()).setAlwaysSave(true);

    /** Whether collecting and reporting usage stats is allowed */
    public static final BooleanSetting USAGE_STATS =
        FACTORY.createBooleanSetting("USAGE_STATS", false);

    /** Setting for if native icons should be preloaded. */
    public static final BooleanSetting PRELOAD_NATIVE_ICONS =
        FACTORY.createBooleanSetting("PRELOAD_NATIVE_ICONS", true);

    /**
     * Setting that globally enables or disables notifications.
     */
    public static final BooleanSetting SHOW_NOTIFICATIONS = 
        FACTORY.createBooleanSetting("SHOW_NOTIFICATIONS", true);

    /**
     * Setting that globally enables or disables notification sounds.
     */
    public static final BooleanSetting PLAY_NOTIFICATION_SOUND =
        FACTORY.createBooleanSetting("PLAY_NOTIFICATION_SOUND", false);

    /**
     * Out-of-the-box list of well known jabber servers
     */
    public static final StringSetSetting XMPP_SERVERS =
        FACTORY.createStringSetSetting("XMPP_SERVERS",
                "false,true,gmail.com,Gmail;" +
                "false,false,livejournal.com,LiveJournal;" +
        "");

    /**
     * User-defined custom jabber server
     */
    public static final StringSetting USER_DEFINED_XMPP_SERVER =
        (StringSetting)FACTORY.createStringSetting("XMPP_SERVER", "").setPrivate(true);

    public static final StringSetting XMPP_AUTO_LOGIN =
    (StringSetting)FACTORY.createStringSetting("XMPP_AUTO_LOGIN", "").setPrivate(true);
    
    /** If the 'offline contacts' in the nav are collapsed. */
    public static final BooleanSetting OFFLINE_COLLAPSED = 
        FACTORY.createBooleanSetting("OFFLINE_CONTACTS_COLLAPSED", true);
    
    /** If the 'online contacts' in the nav are collapsed. */
    public static final BooleanSetting ONLINE_COLLAPSED =
        FACTORY.createBooleanSetting("ONLINE_CONTACTS_COLLAPSED", false);
    
    /** If the LimeWire media player is enabled. */
    public static final BooleanSetting PLAYER_ENABLED =
        FACTORY.createBooleanSetting("PLAYER_ENABLED", true);
    
    /** Shows an overlay on My Library first time logging in */
    public static final BooleanSetting SHOW_FRIEND_OVERLAY_MESSAGE =
        FACTORY.createBooleanSetting("SHOW_FRIEND_OVERLAY_MESSAGE", true);
    
    /** True if the user has logged into chat and viewed their library after, false otherwise */
    public static final BooleanSetting HAS_LOGGED_IN_AND_SHOWN_LIBRARY =
        FACTORY.createBooleanSetting("HAS_LOGGED_IN_AND_SHOWN_LIBRARY", false);


    /**
     * Returns the default directory for the file chooser.
     * Defaults to the users home directory if it exists,
     * otherwise the current directory is used. 
     */
    private static File getDefaultLastFileChooserDir() {
        File defaultDirectory = CommonUtils.getUserHomeDir();
        if(defaultDirectory == null || !defaultDirectory.exists()) {
            defaultDirectory = CommonUtils.getCurrentDirectory();
        }
        return defaultDirectory;
    }

}
