package com.limegroup.gnutella.gui.search;


/**
 * Returns true if the TableLine's schema matches the
 * given schema.
 */
class SchemaFilter implements TableLineFilter {
    private final NamedMediaType SCHEMA;
    
	SchemaFilter(NamedMediaType schema) {
        SCHEMA = schema;
    }
    
    public boolean allow(TableLine line) {
        NamedMediaType type = line.getNamedMediaType();
        return type != null && type.equals(SCHEMA);
    }
    
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof SchemaFilter))
            return false;
        else
            return SCHEMA.equals(((SchemaFilter)o).SCHEMA);
    }    
}