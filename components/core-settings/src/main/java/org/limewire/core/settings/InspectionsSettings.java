package org.limewire.core.settings;

import org.limewire.setting.StringSetting;
import org.limewire.setting.BooleanSetting;

/**
 * Settings related to push inspections
 */
public class InspectionsSettings extends LimeProps {

    /**
     * Setting for whether or not push inspections are enabled
     */
    public static final BooleanSetting PUSH_INSPECTIONS_ENABLED =
        FACTORY.createRemoteBooleanSetting("PUSH_INSPECTIONS_ENABLED", 
                true, "Inspection.enabled");
    
    
    /**
     * Inspection server URL(s)
     */
    public static final StringSetting INSPECTION_SPEC_REQUEST_URL = 
        FACTORY.createRemoteStringSetting("INSPECTION_SPEC_REQUEST_URL", 
                "http://localhost:9876", "Inspection.requestUrl");
    
    public static final StringSetting INSPECTION_SPEC_SUBMIT_URL = 
        FACTORY.createRemoteStringSetting("INSPECTION_SPEC_SUBMIT_URL", 
                "http://localhost:9876", "Inspection.submitUrl");
}
