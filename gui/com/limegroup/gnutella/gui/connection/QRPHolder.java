package com.limegroup.gnutella.gui.connection;

import java.text.NumberFormat;

import com.limegroup.gnutella.gui.GUIUtils;


/**
 * Wrapper class that acts as a comparable for the QRP info.
 */
public final class QRPHolder implements Comparable<QRPHolder> {
    
    /**
     * The percent full of this QRP table.
     */
    final float _percentFull;
    
    /**
     * The size of this QRP table.
     */
    final int _size;
    
    /**
     * The string representation.
     */
    final String _string;
    
    /**
     * Format for the double.
     */
    private final static NumberFormat PERCENT_FORMAT;
    
    static {
        PERCENT_FORMAT = NumberFormat.getPercentInstance();
        PERCENT_FORMAT.setMaximumFractionDigits(2);
        PERCENT_FORMAT.setMinimumFractionDigits(0);
        PERCENT_FORMAT.setGroupingUsed(false);
    }
    
    /**
     * Constructor.
     */
    public QRPHolder(double percentFull, int size) {
        _percentFull = (float)percentFull;
        _size = size;
        _string = PERCENT_FORMAT.format(percentFull/100) + " / " + 
                  GUIUtils.toKilobytes(size);
    }
    
    /**
     * Add up the two things and see which is larger.
     */
    public int compareTo(QRPHolder other) {
        if (_percentFull != other._percentFull)
            return _percentFull < other._percentFull ? -1 : 1;
        if (_size != other._size)
            return _size < other._size ? -1 : 1;
        return 0;
    }

    /**
     *
     * @return the formatted string
     */
    @Override
    public String toString() {
        return _string;
    }
}
