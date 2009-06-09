package org.limewire.http.mongrel;

import org.limewire.core.settings.LimeProps;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings to deal with UI.
 */ 
public final class WebSettings extends LimeProps {
    
    private WebSettings() {}

    /**
     * This setting determines whether the WebUI will start when LimeWire starts.
     */
    public static final BooleanSetting START_WEB_ON_STARTUP =
        FACTORY.createBooleanSetting("START_WEB_ON_STARTUP", false);
    
    /**
     * This setting is the password that the user will type to access WebUI remotely.
     */
    public static final StringSetting WEB_PASSWORD =
        FACTORY.createStringSetting("WEB_PASSWORD", "");
    
    /**
     * This setting is the username that the user will type remotely to access their WebUI.
     */
    public static final StringSetting WEB_ADDRESS =
        FACTORY.createStringSetting("WEB_ADDRESS", "");

}
