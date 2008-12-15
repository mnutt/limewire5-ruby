package com.limegroup.gnutella.gui.xml;

import com.limegroup.gnutella.xml.SchemaFieldInfo;

// Marks an object as being an XMLValue for tables.
public class XMLValue implements Comparable<XMLValue> {
    
    private final String value;
    private final SchemaFieldInfo sfi;
    private String display;
    
    public XMLValue(String value, SchemaFieldInfo sfi) {
        this.value = value;
        this.sfi = sfi;
    }
    
    public String getValue() {
        return value;
    }
    
    public SchemaFieldInfo getSchemaFieldInfo() {
        return sfi;
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if(o instanceof XMLValue)
            return value.equals(((XMLValue)o).value);
        else
            return false;
    }
    
    @SuppressWarnings("unchecked")
    public int compareTo(XMLValue other) {
        if(other == null)
            return 1;
        
        Comparable a = XMLUtils.getComparable(sfi, value);
        Comparable b = XMLUtils.getComparable(other.sfi, other.value);
        if(a == null && b == null)
            return 0;
        else if(b == null)
            return 1;
        else if(a == null)
            return -1;
        else if((a instanceof String) && (b instanceof String))
            return ((String)a).compareToIgnoreCase((String)b);
        else
            return a.compareTo(b);
    }
    
    @Override
    public String toString() {
        if(display == null) {
            if(value == null || sfi == null)
                return null;
            else
                display = XMLUtils.getDisplay(sfi, value);
        }
        return display;
    }

}
