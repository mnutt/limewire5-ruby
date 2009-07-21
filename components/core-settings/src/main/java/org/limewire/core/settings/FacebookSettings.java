package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
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
}
