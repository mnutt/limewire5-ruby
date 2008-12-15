package org.limewire.core.settings;

import org.limewire.i18n.I18nMarker;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringArraySetting;

/**
 * Settings for the startup Dialog: Upgrade to Pro.
 * Contains the message string and remote changeable values for the dialog.
 */
public class StartupAdSettings extends LimeProps {

    private StartupAdSettings(){}
    
    /** Messages to display at startup. An array of messages can be displayed. Each message contains
     * fourteen tab-delimited parts. If a message does not contain all the tabs, it is thrown out. URLS can
     * be null, if that is the case the button will simply dispose of the dialog. 
     * 
     * Each message String is delimited as follows, all of these values can be null except for the last which
     * is the percetage to show this ad: 
     * 
     *  Dialog Title       \t   
     *  Message            \t
     *  Button Message     \t
     *  1st Button Text    \t   
     *  1st Button Tooltip \t
     *  2nd Button Text    \t
     *  2nd Button Tooltip \t
     *  3rd Button Text    \t
     *  3rd Button Tooltip \t
     *  1st Button URL     \t
     *  2nd Button URL     \t
     *  3rd Button URL     \t
     *  image URL          \t
     *  percentage
     * 
     * */
    public static final StringArraySetting PRO_STARTUP_ADS = 
        FACTORY.createRemoteStringArraySetting("PRO_STARTUP_ADS", 
                new String[]{
                I18nMarker.marktr("Upgrade to PRO") + "\t" +
                I18nMarker.marktr("For Turbo-Charged downloads, get LimeWire PRO. We guarantee that you will love the improved performance of PRO. Thank you for helping keep the Internet open by running LimeWire.") + "\t" +
                I18nMarker.marktr("Upgrade to LimeWire PRO?") + "\t" + 
                "\t" + 
                "\t" +
                "\t" +
                "\t" +
                "\t" +
                "\t" +
                "http://www.limewire.com/index.jsp/pro" + "\t" +
                "http://www.limewire.com/promote/whygopro" + "\t" +
                "\t" + 
                "http://clientpix.limewire.com/pix/defaultProAd.jpg" + "\t" +
                "1.0f"
        },"StartupAdSettings.PRO_STARTUP_ADS");
    
    /** Whether to display the Purchase Pro dialog at startup*/
    public static final BooleanSetting PRO_STARTUP_IS_VISIBLE = 
        FACTORY.createRemoteBooleanSetting("PRO_STARTUP_IS_VISIBLE", true, "StartupAdSettings.proStartupIsVisible");
    
    /** Whether to randomize the Purchase Pro dialog buttons*/
    public static final BooleanSetting PRO_STARTUP_RANDOM_BUTTONS = 
        FACTORY.createRemoteBooleanSetting("PRO_STARTUP_RANDOM_BUTTONS", false, "StartupAdSettings.proStartupRandomButtons");
    
    /** Whether to remotely load the background image on the dialog, false display default image always*/
    public static final BooleanSetting PRO_STARTUP_REMOTE_IMAGE = 
        FACTORY.createRemoteBooleanSetting("PRO_STARTUP_REMOTE_IMAGE",  false, "StartupAdSettings.proStartupRemoteImage");
}
