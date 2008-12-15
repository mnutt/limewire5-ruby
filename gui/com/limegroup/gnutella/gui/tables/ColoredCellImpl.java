package com.limegroup.gnutella.gui.tables;

import java.awt.Color;




public final class ColoredCellImpl implements ColoredCell, Comparable<Object> {
    private final Object val;
    private final Color col;
    private final Class<?> clazz;
    
    public ColoredCellImpl( Object dsp, Color cl ) {
        this(dsp, cl, dsp == null ? String.class : dsp.getClass());
    }
    
    public ColoredCellImpl(Object dsp, Color cl, Class <?>clazz) {
        this.val = dsp;
        this.col = cl;
        this.clazz = clazz;
    }
        
    
    public Object getValue() { return val; }
    public Color getColor() { return col; }
    public Class<?> getCellClass() { return clazz; }
    
    @Override
    public String toString() { return val == null ? null : val.toString(); }
    
    public int compareTo( Object o ) {
        return AbstractTableMediator.compare(val, ((ColoredCellImpl)o).val );
    }
}

