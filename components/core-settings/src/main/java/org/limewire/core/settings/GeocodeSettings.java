package org.limewire.core.settings;

import java.util.Properties;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.PropertiesSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for geo coding the client
 */
public class GeocodeSettings extends LimeProps {
    
    private GeocodeSettings() {}
                                                        
	/**
	 * Whether geo coding is enabled.
	 */
	public static final BooleanSetting IS_ENABLED =
		FACTORY.createRemoteBooleanSetting("IS_ENABLED", false, "GeocodeSettings.isEnabled");
    
    /**
     * The URL used to geocode the client.
     */
    public static final StringSetting GEOCODE_URL =
        FACTORY.createRemoteStringSetting("GEOCODE_URL", "http://geo.links.limewire.com/geo/", "GeocodeSettings.geocodeUrlV2");    
    
    /** Last recorded geo location. */
    public static final PropertiesSetting GEO_LOCATION =
        FACTORY.createPropertiesSetting("GEO_LOCATION", new Properties());

}
