package org.limewire.core.settings;

import java.util.Properties;

import org.limewire.i18n.I18nMarker;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.Setting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;

public class FacebookSettings extends LimeProps {

    /**
     * Cached chat channel.
     */
    public static final StringSetting CHAT_CHANNEL =
        FACTORY.createStringSetting("CHAT_CHANNEL", "");
    
    /**
     * Whether Facebook is enabled.
     */
    public static final BooleanSetting FACEBOOK_ENABLED =
        FACTORY.createRemoteBooleanSetting("FACEBOOK_ENABLED", true, "Facebook.facebookEnabled");
    
    /**
     * Facebook auth server urls.
     */
    public static final StringArraySetting AUTH_SERVER_URLS =
        // have to be with trailing /
        FACTORY.createRemoteStringArraySetting("FACEBOOK_AUTH_SERVER_URLS", new String[] {
                "https://fbauth.limewire.com/"
        }, "Facebook.authServerUrls");

    /**
     * The Facebook API key, a remote setting.
     */
    public static final StringSetting API_KEY = 
        FACTORY.createRemoteStringSetting("FACEBOOK_API_KEY", "28615065c80948753945e963923c43ca", "Facebook.apiKey");
    
    /**
     * The Facebook LimeWire Application ID, a remote setting.
     */
    public static final StringSetting APP_ID = 
        FACTORY.createRemoteStringSetting("FACEBOOK_APP_ID", "93767281887", "Facebook.appId");
    
    /**
     * Remote setting to turn on bug reporting for facebook related bugs.
     */
    public static final BooleanSetting REPORT_BUGS = 
        FACTORY.createRemoteBooleanSetting("FACEBOOK_REPORT_BUGS", true, "Facebook.reportBugs");
    
    public static final StringSetting HOME_PAGE_URL = 
        FACTORY.createRemoteStringSetting("FACEBOOK_HOME_PAGE_URL", "http://www.facebook.com/home.php", "Facebook.homePageURL");
    
    public static final StringSetting PRESENCE_POPOUT_PAGE_URL = 
        FACTORY.createRemoteStringSetting("FACEBOOK_PRESENCE_POPOUT_PAGE_URL", "http://www.facebook.com/presence/popout.php", "Facebook.presencePopoutPageURL");
    
    public static final StringSetting CHAT_SETTINGS_URL = 
        FACTORY.createRemoteStringSetting("FACEBOOK_CHAT_SETTINGS_URL", "http://www.facebook.com/ajax/chat/settings.php?", "Facebook.chatSettingsURL");
    
    public static final StringSetting RECONNECT_URL = 
        FACTORY.createRemoteStringSetting("FACEBOOK_RECONNECT_URL", "http://www.facebook.com/ajax/presence/reconnect.php?reason=3", "Facebook.reconnectURL");
    
    public static final StringSetting LOGOUT_URL = 
        FACTORY.createRemoteStringSetting("FACEBOOK_LOGOUT_URL", "http://www.facebook.com/logout.php?", "Facebook.logoutURL");
    
    public static final StringSetting SEND_CHAT_URL = 
        FACTORY.createRemoteStringSetting("FACEBOOK_SEND_CHAT_URL", "http://www.facebook.com/ajax/chat/send.php", "Facebook.sendChatURL");
    
    public static final StringSetting SEND_CHAT_STATE_URL = 
        FACTORY.createRemoteStringSetting("FACEBOOK_SEND_CHAT_STATE_URL", "http://www.facebook.com/ajax/chat/typ.php", "Facebook.sendChatStateURL");
    
    public static final StringSetting UPDATE_PRESENCES_URL = 
        FACTORY.createRemoteStringSetting("FACEBOOK_UPDATE_PRESENCES_URL", "http://www.facebook.com/ajax/presence/update.php", "Facebook.updatePresencesURL");
    
    public static final StringSetting RECEIVE_CHAT_URL = 
        FACTORY.createRemoteStringSetting("FACEBOOK_RECEIVE_CHAT_URL", "http://0.channel" + "$channel" + ".facebook.com/x/0/false/p_" + "$uid" + "=" + "$seq", "Facebook.receiveChatURL");

    public static final Setting<Properties> ATTRIBUTES =
        FACTORY.createPropertiesSetting("FACEBOOK_ATTRIBUTES", new Properties()).setPrivate(true);
    
    public static final BooleanSetting SEND_SHARE_NOTIFICATIONS =
        FACTORY.createRemoteBooleanSetting("FACEBOOK_SEND_SHARE_NOTIFICATIONS", true, "Facebook.sendShareNotifications");
    
    public static final StringArraySetting SHARE_NOTIFICATIONS_TEXTS = 
        FACTORY.createRemoteStringArraySetting("FACEBOOK_NOTIFICATIONS_TEXTS", I18nMarker.marktrn("just shared a file with you on LimeWire. Start {0}LimeWire{1} or {2}download{3} it to get the file.",
                "just shared {4} files with you on LimeWire. Start {0}LimeWire{1} or {2}download{3} it to get them."), "Facebook.shareNotificationsTexts");

    public static final StringArraySetting SHARE_LINK_URLS = 
        FACTORY.createRemoteStringArraySetting("FACEBOOK_SHARE_LINK_URLS", 
                new String[] { "http://client-data.limewire.com/fb-client-open/", "http://client-data.limewire.com/fb-download-notification/" },
        "Facebook.shareLinkUrls");
}
