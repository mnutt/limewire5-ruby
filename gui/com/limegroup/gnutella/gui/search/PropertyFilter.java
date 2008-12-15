package com.limegroup.gnutella.gui.search;

import com.limegroup.gnutella.gui.search.Selector.PropertyType;
import com.limegroup.gnutella.gui.tables.IconAndNameHolder;

/**
 * Filters TableLines based on certain properties.
 */
class PropertyFilter implements TableLineFilter {
    private final TableLineFilter FILTER;
    
    /**
     * Constructs a new PropertyFilter for the given property/value.
     *
     * If the property is unknown, IllegalArgumentException is thrown.
     */
	PropertyFilter(String property, Object value) {
        if(property == null)
            throw new NullPointerException("null property");
        if(value == null)
            throw new NullPointerException("null value");
        
        // Set the actual internal filter depending on the type
        // of property.
        if(PropertyType.TYPE.getKey().equals(property))
            FILTER = new ExtensionFilter(value);
        else if (PropertyType.SPEED.getKey().equals(property))
            FILTER = new SpeedFilter(value);
        else if (PropertyType.VENDOR.getKey().equals(property))
            FILTER = new VendorFilter(value);
        else
            throw new IllegalArgumentException("bad property: " + property);
    }
    
    public boolean allow(TableLine line) {
        return FILTER.allow(line);
    }
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof PropertyFilter))
            return false;
        return FILTER.equals(((PropertyFilter)o).FILTER);
    }
    
    /**
     * A filter for extensions.
     */
    private static class ExtensionFilter implements TableLineFilter {
        private final String TYPE;
        
        /**
         * Constructs a new ExtensionFilter.
         */
        ExtensionFilter(Object value) {
            TYPE = ((IconAndNameHolder)value).getName();
        }
        
        public boolean allow(TableLine line) {
            return TYPE.equalsIgnoreCase(line.getExtension());
        }
        
        @Override
        public boolean equals(Object o) {
            if(!(o instanceof ExtensionFilter))
                return false;
            else
                return TYPE.equals(((ExtensionFilter)o).TYPE);
        }
    }
    
    /**
     * A filter for speeds.
     */
    private static class SpeedFilter implements TableLineFilter {
        private final ResultSpeed SPEED;
        
        /**
         * Constructs a new SpeedFilter.
         */
        SpeedFilter(Object value) {
            SPEED = (ResultSpeed)value;
        }
        
        public boolean allow(TableLine line) {
            return SPEED.isSameSpeed(line.getSpeed());
        }
        
        @Override
        public boolean equals(Object o) {
            if(!(o instanceof SpeedFilter))
                return false;
            else
                return SPEED.isSameSpeed(((SpeedFilter)o).SPEED);
        }        
    }
    
    /**
     * A filter for vendor.
     */
    private static class VendorFilter implements TableLineFilter {
        private final String VENDOR;
        
        /**
         * Constructs a new VendorFilter.
         */
        VendorFilter(Object value) {
            VENDOR = (String)value;
        }
        
        public boolean allow(TableLine line) {
            return VENDOR.equals(line.getVendor());
        }
        
        @Override
        public boolean equals(Object o) {
            if(!(o instanceof VendorFilter))
                return false;
            else
                return VENDOR.equals(((VendorFilter)o).VENDOR);
        }            
    }   
}