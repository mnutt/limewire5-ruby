package com.limegroup.gnutella.gui.search;

import org.limewire.util.StringUtils;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.xml.XMLUtils;

/**
 * Represents a single selector.
 */
class Selector {
    /**
     * A SCHEMA selector.
     */
    public static final int SCHEMA = 1;
    
    /**
     * A FIELD selector.
     */
    public static final int FIELD = 2;
    
    /**
     * A PROPERTY selector.
     */
    public static final int PROPERTY = 3;
    
    /**
     * The value for the selector if it represents listing all schemas.
     */
    private static final String S_SCHEMA = "schema";
    
    /**
     * The value for the selector if it represents a specific field within
     * a schema.
     */
    private static final String S_FIELD = "field";
    
    /**
     * The value for the selector if it represents a property, such as
     * "extension", "quality", "speed", etc.
     */
    private static final String S_PROPERTY = "property";    
    
    /**
     * The schema of this selector.
     */
    private final String _SCHEMA;
    
    /**
     * The value of this selector.
     */
    private final String _VALUE;
    
    /**
     * The type of this selector.
     */
    private final int _TYPE;
    
    /**
     * The properties type if the selector is a property selector, otherewise
     * null.
     */
    private final PropertyType propertyType;
    
    /**
     * Whether or not this selector is minimized.
     */
    private boolean _minimized = false;
    /**
	 * Stores the once computed hash code of this selector. This works as long
	 * as the Selector class stays immutable, as it is now.
	 */
	private int cachedHashCode;
	
	/**
	 * Used for mapping the properties settings keys user visible
	 * translations.
	 */
	static enum PropertyType {
	    TYPE("RESULT_PANEL_TYPE", I18n.tr("Type")),
	    SPEED("RESULT_PANEL_SPEED", I18n.tr("Speed")),
	    VENDOR("RESULT_PANEL_VENDOR", I18n.tr("Vendor"));
	    
	    private final String key;
	    
	    private final String title;
	    
	    private PropertyType(String key, String title) {
	        this.key = key;
	        this.title = title;
        }
	    
	    public String getTitle() {
            return title;
        }
	    
	    public String getKey() {
            return key;
        }
	    
	    /**
	     * Looks up the type by its key
	     * @throws IllegalArgumentException if the key doesn't match a type
	     */
	    public static PropertyType fromKey(String key) throws IllegalArgumentException {
	        for (PropertyType type : values()) {
	            if (key.equals(type.key)) {
	                return type;
	            }
	        }
	        throw new IllegalArgumentException("not a key for a type: " + key);
	    }
	};
	
    /**
     * Constructs a new selector.
     */
    private Selector(String schema, String value, int type) {
        this._SCHEMA = schema;
        this._VALUE = value;
        this._TYPE = type;
        this.propertyType = null;
    }
    
    private Selector(String schema, PropertyType type) {
        this._SCHEMA = schema;
        this._VALUE = type.getKey();
        this._TYPE = PROPERTY;
        this.propertyType = type;
    }
    
    /**
     * Creates a new schema selector.
     */
    public static Selector createSchemaSelector() {
        return new Selector("", "", SCHEMA);
    }
    
    /**
     * Creates a new selector for the specified field within a schema.
     */
    public static Selector createFieldSelector(String schema, String field) {
        return new Selector(schema, field, FIELD);
    }
    
    /**
     * Creates a new selector for the specified property.
     * @throws IllegalArgumentException if the property does not exist
     */
    public static Selector createPropertySelector(String property) throws IllegalArgumentException {
        PropertyType type = PropertyType.fromKey(property);
        return new Selector("", type);
    }
    
    /**
     * Creates a new selector for the specified property.
     */
    public static Selector createPropertySelector(PropertyType propertyType) {
        return new Selector("", propertyType);
    }
    
    /**
     * Creates a selector from a string.
     *
     * Valid strings are:
     *   schema | <true|false>
     *   field, <schema>, <value> | <true|false>
     *   property, <value> | <true|false>
     *
     * All other strings will throw an IllegalArgumentException
     */
    public static Selector createFromString(String value) {
        if(value == null)
            throw new IllegalArgumentException("null value");
        
        value = value.toLowerCase();
        String left, right;
        int pipe = value.indexOf("|");
        if(pipe == -1 || pipe == value.length()) {
            left = value.trim();
            right = "";
        } else {
            left = value.substring(0, pipe).trim();
            right = value.substring(pipe+1).trim();
        }
        
        String[] data = StringUtils.split(left, ",");
        if(data.length == 0)
            throw new IllegalArgumentException(value);
        for(int i = 0; i < data.length; i++)
            data[i] = data[i].trim();

        Selector selector;      
        // First parse the 'data'  
        if(S_SCHEMA.equals(data[0])) {
            selector = createSchemaSelector();
            // must have nothing else
            if(data.length != 1)
                throw new IllegalArgumentException(value);
        } else if(S_FIELD.equals(data[0])) {
            // Must have schema & value
            if(data.length != 3)
                throw new IllegalArgumentException(value);
            String schema = data[1];
            String info = data[2];
            selector = createFieldSelector(schema, info);
        } else if(S_PROPERTY.equals(data[0])) {
            // must have value.
            if(data.length != 2)
                throw new IllegalArgumentException(value);
            String info = data[1].toUpperCase();
            selector = createPropertySelector(info);
        } else {
            throw new IllegalArgumentException(value);
        }
        
        // Then process the additional fields
        data = StringUtils.split(right, ",");
        for(int i = 0; i < data.length; i++)
            data[i] = data[i].trim();        
        if(data.length > 0)
            selector.setMinimized(data[0].equals("true"));
        // as more fields are added, process them here, ala:
        // if(data.length > 1)
        //   selector.setNewData(data[1].equals("data"));

        return selector;
    }
    
    /**
     * Writes the selector out in the specified format.
     */
    @Override
    public String toString() {
        String value;
        switch(_TYPE) {
        case SCHEMA: value = S_SCHEMA; break;
        case FIELD: value = S_FIELD + ", " + _SCHEMA + ", " + _VALUE; break;
        case PROPERTY: value = S_PROPERTY + ", " + _VALUE; break;
        default:
            throw new IllegalStateException("invalid type: " + _TYPE);
        }
        return value + " | " + _minimized;
    }
    
    /**
     * Determines if this is minimized.
     */
    public boolean isMinimized() {
        return _minimized;
    }
    
    /**
     * Sets the minimized status.
     */
    public void setMinimized(boolean minimized) {
        _minimized = minimized;
    }
    
    /**
     * Determines the title of this selector.
     *
     * TODO: Move somewhere else?  Don't want to have dependencies...
     */
    public String getTitle() {
        switch(_TYPE) {
        case Selector.SCHEMA:
            return I18n.tr("Media");
        case Selector.FIELD:
            return XMLUtils.getResource(_VALUE);
        case Selector.PROPERTY:
            return propertyType.getTitle();
        default:
            throw new IllegalArgumentException("invalid type: " + _TYPE);
        }
    }
    
    /**
     * Determines if this is a schema selector.
     */
    public boolean isSchemaSelector() {
        return _TYPE == SCHEMA;
    }
    
    /**
     * Determines if this is a field selector.
     */
    public boolean isFieldSelector() {
        return _TYPE == FIELD;
    }
    
    /**
     * Determines if this is a property selector.
     */
    public boolean isPropertySelector() {
        return _TYPE == PROPERTY;
    }
    
    /**
     * Determines the type of selector.
     */
    public int getSelectorType() {
        return _TYPE;
    }
    
    /**
     * Determines the schema of this selector.  Only valid
     * if the type is a field.
     */
    public String getSchema() {
        if(_TYPE != FIELD)
            throw new IllegalStateException("invalid type: " + _TYPE);
        
        return _SCHEMA;
    }
    
    /**
     * Determines the value of this schema.  Only valid if the type
     * is a field or property.
     */
    public String getValue() {
        if(_TYPE != FIELD && _TYPE != PROPERTY)
            throw new IllegalStateException("invalid type: " + _TYPE);
        
        return _VALUE;
    }
    
    /**
     * Determines whether or not the specified selector is the same as this one.
     */
    @Override
    public boolean equals(Object o) {
        if(o == this)
            return true;
        if(o instanceof Selector) {
            Selector f = (Selector)o;
            return f._TYPE == _TYPE &&
                   f._SCHEMA.equals(_SCHEMA) &&
                   f._VALUE.equals(_VALUE);
        } else {
            return false;
        }
    }
    
    /**
     * Returns the hash code of this object.
     */
    @Override
    public int hashCode() {
        if (cachedHashCode == 0) {
			cachedHashCode = _TYPE + 31 * _SCHEMA.hashCode() + 31 * 31 * _VALUE.hashCode();
        }
		return cachedHashCode;
    }
}
