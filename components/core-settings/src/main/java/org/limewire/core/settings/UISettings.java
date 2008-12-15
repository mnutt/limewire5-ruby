package org.limewire.core.settings;

import java.awt.Dimension;
import java.awt.Toolkit;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings to deal with UI.
 */ 
public final class UISettings extends LimeProps {

    private UISettings() {}

    /**
     * Setting for autocompletion
     */
    public static final BooleanSetting AUTOCOMPLETE_ENABLED =
		FACTORY.createBooleanSetting("AUTOCOMPLETE_ENABLED", true);
		
    /**
     * Setting for search-result filters.
     */
    public static final BooleanSetting SEARCH_RESULT_FILTERS =
        FACTORY.createBooleanSetting("SEARCH_RESULT_FILTERS", true);
                                     
    /**
     * Setting for using small icons.
     */
    public static final BooleanSetting SMALL_ICONS =
        FACTORY.createBooleanSetting("UI_SMALL_ICONS", isResolutionLow());
        
    /**
     * Setting for displaying text under icons.
     */
    public static final BooleanSetting TEXT_WITH_ICONS =
        FACTORY.createBooleanSetting("UI_TEXT_WITH_ICONS", true);
        
    /**
     * Setting for not grouping search results in GUI
     */
    public static final BooleanSetting UI_GROUP_RESULTS =
        FACTORY.createBooleanSetting("UI_GROUP_RESULTS", true);
        
    /**
     * Setting to allow ignoring of alt-locs in replies.
     */
    public static final BooleanSetting UI_ADD_REPLY_ALT_LOCS =
        FACTORY.createBooleanSetting("UI_ADD_REPLY_ALT_LOCS", true);
        
    /**
     * For people with bad eyes.
     */
    private static boolean isResolutionLow() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return screenSize.width <= 800 || screenSize.height <= 600;
    }

    /**
     * Setting to persist monitor check box state.
     */
    public static final BooleanSetting UI_MONITOR_SHOW_INCOMING_SEARCHES =
        FACTORY.createBooleanSetting("UI_MONITOR_SHOW_INCOMING_SEARCHES", false);
	
	/**
	 * Setting for the divider location between library tree and table.
	 */
	public static final IntSetting UI_LIBRARY_TREE_DIVIDER_LOCATION =
		FACTORY.createIntSetting("UI_LIBRARY_TREE_DIVIDER_LOCATION", -1);
	
	/**
	 * Setting for the divider location between library and playlist.
	 */
	public static final IntSetting UI_LIBRARY_PLAY_LIST_TAB_DIVIDER_LOCATION =
		FACTORY.createIntSetting("UI_LIBRARY_PLAY_LIST_TAB_DIVIDER_LOCATION",
				300);
	
	/**
	 * Setting for the divider location between incoming query monitors and
	 * upload panel.
	 */
	public static final IntSetting UI_MONITOR_UPLOAD_TAB_DIVIDER_LOCATION =
		FACTORY.createIntSetting("UI_MONITOR_UPLOAD_TAB_DIVIDER_LOCATION", 300);
    
    /** Setting for if native icons should be preloaded. */
    public static final BooleanSetting PRELOAD_NATIVE_ICONS =
        FACTORY.createBooleanSetting("PRELOAD_NATIVE_ICONS", true);
    
    /**
     * Setting to persist the width of the options dialog if the dialog
     * was resized by the user.
     */
    public static final IntSetting UI_OPTIONS_DIALOG_WIDTH = 
        FACTORY.createIntSetting("UI_OPTIONS_DIALOG_WIDTH", 600);
    
    /**
     * Setting to persist the height of the options dialog if the dialog
     * was resized by the user.
     */
    public static final IntSetting UI_OPTIONS_DIALOG_HEIGHT= 
        FACTORY.createIntSetting("UI_OPTIONS_DIALOG_HEIGHT", 500);
    
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
    
    /** Whether or not to use network-based images, or just always use built-in ones. */
    private static final BooleanSetting USE_NETWORK_IMAGES = FACTORY.createRemoteBooleanSetting("USE_NETWORK_IMAGES",
           true, "UI.useNetworkImages");
    
    /** Collection of info for the 'Getting Started' image. */
    public static final ImageInfo INTRO_IMAGE_INFO = new ImageInfoImpl(true);
    
    /** Collection of info for the 'After Search' image. */
    public static final ImageInfo AFTER_SEARCH_IMAGE_INFO = new ImageInfoImpl(false);
    
    public static interface ImageInfo {     
        /** The URL to pull the image from. */
        public String getImageUrl();
        /** Whether or not PRO users should show this pic. */
        public boolean canProShowPic();
        /** Whether or not this pic can have an outgoing link. */
        public boolean canLink();
        /** The outgoing link if triggered from the backup image. */
        public String getLocalLinkUrl();
        /** The outgoing link if triggered from the network image. */
        public String getNetworkLinkUrl();
        /** True if network images should be used. */
        public boolean useNetworkImage();
        /** True if this is the 'Into' pic. */
        boolean isIntro();
    }
    
    private static class ImageInfoImpl implements ImageInfo {
        private final boolean intro;
        private final StringSetting imageUrl;
        private final BooleanSetting proShowPic;
        private final BooleanSetting canLink;
        private final StringSetting localLink;
        private final StringSetting networkLink;
        
        ImageInfoImpl(boolean intro) {
            this.intro = intro;
            imageUrl = FACTORY.createRemoteStringSetting(key("URL"), 
                "http://clientpix.limewire.com/pix/" + (intro ? "intro" : "afterSearch"), remoteKey("Url"));
            proShowPic = FACTORY.createRemoteBooleanSetting(key("PRO_SHOW"), 
                false, remoteKey("ProShow"));
            canLink = FACTORY.createRemoteBooleanSetting(key("HAS_LINK"), 
                true, remoteKey("CanLink"));
            localLink = FACTORY.createRemoteStringSetting(key("LOCAL_LINK"), 
                intro ? "" 
                      : "http://www.limewire.com/inclient/?stage=after&resource=local", remoteKey("ClickLinkLocal"));
            networkLink = FACTORY.createRemoteStringSetting(key("NETWORK_LINK"), 
                intro ? "http://www.limewire.com/inclient/?stage=intro&resource=network"
                      : "http://www.limewire.com/inclient/?stage=after&resource=network", remoteKey("ClickLink"));
        }
        
        private String key(String key) {
            return intro ? "INTRO_" + key : "AFTER_SEARCH_" + key;
        }
        
        private String remoteKey(String key) {
            return intro ? "UI.intro" + key : "UI.afterSearch" + key;
        }

        public boolean canLink() {
            return canLink.getValue();
        }

        public boolean canProShowPic() {
            return proShowPic.getValue();
        }

        public String getImageUrl() {
            return imageUrl.getValue();
        }

        public String getLocalLinkUrl() {
            return localLink.getValue();
        }

        public String getNetworkLinkUrl() {
            return networkLink.getValue();
        }
        
        public boolean useNetworkImage() {
            return USE_NETWORK_IMAGES.getValue();
        }
        
        public boolean isIntro() {
            return intro;
        }
    }
    


}
