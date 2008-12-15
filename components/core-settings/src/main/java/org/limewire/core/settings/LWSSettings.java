package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for The LimeWire Store&#8482;. This is used by
 * {@link LWSManagerImpl} for the host name to which we connect for
 * authentication.
 */
public final class LWSSettings extends LimeProps {
    
    
    private LWSSettings() {}

    /**
     * The hostname to which we connect for authentication.
     * <br>e.g. <code>1.2.3.4</code>
     */
    public static final StringSetting LWS_AUTHENTICATION_HOSTNAME = FACTORY.createRemoteStringSetting(
            "LWS_AUTHENTICATION_HOSTNAME", "www.store.limewire.com", "LWSSettings.lwsAuthenticationHostname");

    /**
     * The port on which we connect for authentication. This can be
     * <code><= 0</code> for no port.
     * <br>e.g. <code>80</code>
     */
    public static final IntSetting LWS_AUTHENTICATION_PORT = FACTORY.createRemoteIntSetting(
            "LWS_AUTHENTICATION_PORT", 80, "LWSSettings.lwsAuthenticationPort", -Integer.MIN_VALUE,
            10000);
    
    /**
     * Allow us to turn on/off SSL messages to the Server.
     */
    public static final BooleanSetting LWS_USE_SSL = FACTORY.createRemoteBooleanSetting(
            "LWS_USE_SSL", false, "LWSSettings.lwsUseSSL");    
    

    /**
     * The entire prefix to put before a url is downloaded.  This is encoded/decoded many times
     * so needs to be all together.  This also makes it clearer.  <b>This HAS to end in a <code>/</code></b>.
     * <br>e.g. <code>1.2.3.4:80</code>
     */
    public static final StringSetting LWS_DOWNLOAD_PREFIX = FACTORY.createRemoteStringSetting(
            "LWS_DOWNLOAD_PREFIX", "www.store.limewire.com:80", "LWSSettings.lwsDownloadPrefix"); 
    
    /**
     * The hostname to which we connect for adding to playlists.
     */
    public static final StringSetting LWS_ADD_TO_PLAYLIST_HOSTNAME = FACTORY.createRemoteStringSetting(
            "LWS_ADD_TO_PLAYLIST_HOSTNAME", "", "LWSSettings.lwsAddToPlaylistHostname");

    /**
     * The port on which we connect for adding to playlists. This can be
     * <code><= 0</code> for no port.
     */
    public static final IntSetting LWS_ADD_TO_PLAYLIST_PORT = FACTORY.createRemoteIntSetting(
            "LWS_ADD_TO_PLAYLIST_PORT", 80, "LWSSettings.lwsAddToPlaylistPost", -Integer.MIN_VALUE,
            10000);  

    /**
     * Allow us to disable the lws server.
     */
    public static final BooleanSetting LWS_IS_ENABLED = FACTORY.createRemoteBooleanSetting(
            "LWS_IS_ENABLED", true, "LWSSettings.lwsIsEnabled");
   
}
