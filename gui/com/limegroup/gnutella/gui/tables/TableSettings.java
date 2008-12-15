package com.limegroup.gnutella.gui.tables;

import org.limewire.core.settings.LimeProps;
import org.limewire.core.settings.TablesHandler;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.SettingsFactory;


/**
 * Manages settings for tables and their associated components.
 */
public class TableSettings {
    
    /**
     * The SettingsFactory settings will be added/read to/from.
     */
    protected static final SettingsFactory FACTORY =
        TablesHandler.instance().getFactory();
        
    /**
     * The old setting for whether or not row striping was
     * enabled in the GUI.  Used for migrating the setting
     * to the newer system.
     */
    private static final BooleanSetting OLD_STRIPE =
        LimeProps.instance().getFactory().
            createBooleanSetting("ROW_STRIPE_ENABLED", true);        
    
    /**
     * Additions to the ID to identify the setting.
     */

    private static final String STRIPE = "_ROWSTRIPE";
    private static final String SORT = "_SORT";
    private static final String TOOLTIP = " _TOOLTIP";
    private static final String MIGRATED = "_MIGRATED";
    
    /**
     * The setting for whether or not to rowstripe this table.
     */
    public BooleanSetting ROWSTRIPE;
    
    /**
     * The setting for whether or not to sort in real time.
     */
    public BooleanSetting REAL_TIME_SORT;
    
    /**
     * The setting for whether or not to display tooltips.
     */
    public BooleanSetting DISPLAY_TOOLTIPS;
    
    /**
     * Setting for whether or not the old 'rowstripe' setting was migrated.
     */
    private BooleanSetting ROWSTRIPE_MIGRATED;
    
    /**
     * The id of this settings object.
     */
    private final String ID;
    
    /**
     * Constructs a new TableSettings whose settings
     * are identified by the specified ID.
     */
    public TableSettings(String id) {
        ID = id;
        ROWSTRIPE =
            FACTORY.createBooleanSetting(id + STRIPE, getDefaultRowStripe());
        REAL_TIME_SORT =
            FACTORY.createBooleanSetting(id + SORT, getDefaultSorting());
        DISPLAY_TOOLTIPS =
            FACTORY.createBooleanSetting(id + TOOLTIP, getDefaultTooltips());
        ROWSTRIPE_MIGRATED =
            FACTORY.createBooleanSetting(id + MIGRATED, false);
            
        // Row stripe used to be a global setting.
        // Now that it is per-table, we need to migrate
        // that setting over to tables.props as needed.
        // Note that we cannot say OLD_STRIPE.revertToDefault()
        // after migratation because multiple TableSettings
        // objects are created. (One per table.)
        if( !ROWSTRIPE_MIGRATED.getValue() ) {
            ROWSTRIPE.setValue( OLD_STRIPE.getValue() );
            ROWSTRIPE_MIGRATED.setValue(true);
        }
    }
    
    /**
     * Gets the ID of this TableSettings object.
     */
    public String getID() {
        return ID;
    }
    
    /**
     * Returns the default value for row striping.
     */
    protected boolean getDefaultRowStripe() {
        return true;
    }
    
    /**
     * Returns the default value for sorting.
     */
    protected boolean getDefaultSorting() {
        return true;
    }
    
    /**
     * Returns the default value for displaying tooltips.
     */
    protected boolean getDefaultTooltips() {
        return true;
    }
    
    /**
     * Reverts all options to their default for this table.
     */
    public void revertToDefault() {
        ROWSTRIPE.revertToDefault();
        REAL_TIME_SORT.revertToDefault();
        DISPLAY_TOOLTIPS.revertToDefault();
    }
    
    /**
     * Determines if all the options are already at their defaults.
     */
    public boolean isDefault() {
        return ROWSTRIPE.isDefault() &&
               REAL_TIME_SORT.isDefault() &&
               DISPLAY_TOOLTIPS.isDefault();
    }
}

