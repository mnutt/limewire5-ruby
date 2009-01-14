package org.limewire.ui.swing.settings;

import org.limewire.core.settings.LimeWireSettings;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.SettingsFactory;

/**
 * Handles installation preferences.
 */
public final class InstallSettings extends LimeWireSettings {

    private static final InstallSettings INSTANCE =
        new InstallSettings();
    private static final SettingsFactory FACTORY =
        INSTANCE.getFactory();

    public static InstallSettings instance() {
        return INSTANCE;
    }

    private InstallSettings() {
        super("installation.props", "LimeWire installs file");
    }
    
    /**
     * Whether or not the 'Choose your Save directory' question has
     * been asked.
     */
    public static final BooleanSetting SAVE_DIRECTORY =
        FACTORY.createBooleanSetting("SAVE_DIRECTORY", false);
    
    /**
     * Whether or not the 'Choose your speed' question has been asked.
     */
    public static final BooleanSetting SPEED =
        FACTORY.createBooleanSetting("SPEED", false);
    
    /**
     * Whether or not the 'Scan for files' question has been asked.
     */
    public static final BooleanSetting SCAN_FILES =
        FACTORY.createBooleanSetting("SCAN_FILES", false);
        
    /**
     * Whether or not the 'Start on startup' question has been asked.
     */
    public static final BooleanSetting START_STARTUP =
        FACTORY.createBooleanSetting("START_STARTUP", false);
        
    /**
     * Whether or not the 'Choose your language' question has been asked.
     */
    public static final BooleanSetting LANGUAGE_CHOICE =
        FACTORY.createBooleanSetting("LANGUAGE_CHOICE", false);
        
    /**
     * Whether or not the firewall warning question has been asked.
     */
    public static final BooleanSetting FIREWALL_WARNING =
        FACTORY.createBooleanSetting("FIREWALL_WARNING", false);
    
    /** Whether or not the filter question has been asked. */
    public static final BooleanSetting FILTER_OPTION =
        FACTORY.createBooleanSetting("FILTER_OPTION", false);
    
    /** Whether the association option has been asked */
    public static final IntSetting ASSOCIATION_OPTION =
    	FACTORY.createIntSetting("ASSOCIATION_OPTION", 0);

    /** Whether the association option has been asked */
    public static final BooleanSetting EXTENSION_OPTION =
        FACTORY.createBooleanSetting("EXTENSION_OPTION", false);
    
    /** Whether the setup wizard has been completed on 5.0 */
    public static final BooleanSetting UPGRADED_TO_5 =
        FACTORY.createBooleanSetting("UPGRADED_TO_5", false);
}