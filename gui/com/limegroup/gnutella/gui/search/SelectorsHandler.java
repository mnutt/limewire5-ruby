package com.limegroup.gnutella.gui.search;

import java.util.HashMap;
import java.util.Map;

import org.limewire.core.settings.LimeWireSettings;
import org.limewire.setting.SettingsFactory;
import org.limewire.setting.StringSetting;

import com.limegroup.gnutella.gui.search.Selector.PropertyType;


/**
 * Maintains the settings of what selectors are associated with
 * the various schema searches.
 */
class SelectorsHandler extends LimeWireSettings {
    
    /**
     * The sole instance of this SelectorSettings class.
     */
    private static final SelectorsHandler INSTANCE = new SelectorsHandler();
    
    /**
     * The factory for the SelectorSettings.
     */
    private static final SettingsFactory FACTORY = INSTANCE.getFactory();
    
    /**
     * The mapping of active selectors.
     *
     * The key is the type of search + "_" + the number of the selector.
     * The value is the actual selector.
     *
     * For example, if the default selectors for someone's "any type" searches
     * are first "schema", then "audio/artist", then "audio/album", this would
     * have:
     *  ANY_TYPE_1 -> Selector (of type schema) / StringSetting
     *  ANY_TYPE_2 -> Selector (of type field)  / StringSetting 
     *  ANY_TYPE_3 -> Selector (of type field)  / StringSetting
     */
    private static final Map<String, SelectorSettingPair> FILTERS =
        new HashMap<String, SelectorSettingPair>();
    
    /**
     * Returns the instance of this class.
     */
    public static SelectorsHandler instance() {
        return INSTANCE;
    }
    
    /**
     * Private constructor to ensure that nothing else can instantiate.
     */
    private SelectorsHandler() {
        super("filters.props", "LimeWire Filters File");
    }
    
    /**
     * Retrieves the selector for the specified type at the specified depth.
     */
    public static Selector getSelector(String type, int depth) {
        String key = type + "_" + depth;
        SelectorSettingPair pair = FILTERS.get(key);
        if(pair == null) {
            pair = addDefault(type, depth);
        }
        return pair.selector;
    }
    
    /**
     * Sets the new selector for the specified type/depth.
     */
    public static void setSelector(String type, int depth, Selector selector) {
        String key = type + "_" + depth;   
        SelectorSettingPair pair = FILTERS.get(key);
        StringSetting setting;
        if(pair == null)
            pair = addDefault(type, depth);
        setting = pair.setting;
        setting.setValue(selector.toString());
        FILTERS.put(key, new SelectorSettingPair(selector, setting));
    }   
        
    /**
     * Adds the default selector for the specified type & depth.
     */
    private static SelectorSettingPair addDefault(String type, int depth) {
        String key = type + "_" + depth;
        Selector selector = getDefaultSelector(type, depth);
        StringSetting setting =
            FACTORY.createStringSetting(key, selector.toString());
        try {
            selector = Selector.createFromString(setting.getValue());
        } catch(IllegalArgumentException iae) {
            // invalid data on disk, ignore.
        }
        
        SelectorSettingPair pair = new SelectorSettingPair(selector, setting);
        FILTERS.put(key, pair);
        return pair;
    }
            
    /**
     * Creates default selectors for the specified types and depths.
     *
     * Default 'all' selectors are:
     *   1: schema
     *   2: artist
     *   3: album
     *
     * Default audio selectors are:
     *   1: schema
     *   2: artist
     *   3: album
     *
     * Default 'video' selectors are:
     *   1: schema
     *   2: type
     *   3: rating
     *
     * Default selectors for everything else are:
     *   1: schema
     *   2: extension
     *   3: quality
     */
    private static Selector getDefaultSelector(String type, int depth) {
        if("*".equals(type)) {
            switch(depth) {
            case 0: return Selector.createSchemaSelector();
            case 1: return Selector.createFieldSelector("audio",
                                            "audios__audio__artist__");
            case 2: return Selector.createFieldSelector("audio",
                                            "audios__audio__album__");
            }
        } else if("audio".equals(type)) {
            switch(depth) {
            case 0: return Selector.createFieldSelector("audio",
                                            "audios__audio__genre__");
            case 1: return Selector.createFieldSelector("audio",
                                             "audios__audio__artist__");
            case 2: return Selector.createFieldSelector("audio",
                                            "audios__audio__album__");
            }
        } else if("video".equals(type)) {
            switch(depth) {
            case 0: return Selector.createPropertySelector(PropertyType.TYPE);
            case 1: return Selector.createFieldSelector("video",
                                            "videos__video__type__");
            case 2: return Selector.createFieldSelector("video",
                                            "videos__video__rating__");
            }
        } else {
            switch(depth) {
            case 0: return Selector.createSchemaSelector();
            case 1: return Selector.createPropertySelector(PropertyType.TYPE);
            case 2: return Selector.createPropertySelector(PropertyType.SPEED);
            }
        }
        throw new IllegalArgumentException("invalid depth: " + depth);
    }
    
    /**
     * A Pair of Selector/Setting.
     */
    private static class SelectorSettingPair {
        private final Selector selector;
        private final StringSetting setting;
        
        SelectorSettingPair(Selector selector, StringSetting setting) {
            this.selector = selector;
            this.setting = setting;
        }
    }

}
        