package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;

/**
 * Settings for Statistics related stuff.
 */ 

public class StatisticsSettings extends LimeProps {
    
    public static BooleanSetting RECORD_VM_STATS = 
        FACTORY.createBooleanSetting("RECORD_VM_STATS", false);
}
