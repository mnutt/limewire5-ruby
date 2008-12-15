package com.limegroup.gnutella.gui.search;

import com.limegroup.gnutella.gui.GUIUtils;


/**
 * The speed displayed in a search result line.  Contains a string description,
 * the numerical speed (for accurate sorting purposes), and whether the speed is
 * measured or set by user.  These values are rendered by ResultSpeedRenderer.  
 */
class ResultSpeed implements Comparable<ResultSpeed> {
    private int speed;   
    /** Note that this is calculated once upon construction so rendering is
     *  efficient--a classic time-space tradeoff! */
    private String description;
    private boolean isMeasured;

    public ResultSpeed(int speed, boolean isMeasured) {
        this.speed=speed;
        this.isMeasured=isMeasured;
        this.description=GUIUtils.speed2name(speed);
    }

    /** Returns the actual speed of this, in kb/s. */
    public int intValue() {
        return speed;
    }

    /** A textual description of this speed, e.g., 'Modem'. */
    public String stringValue() {
        return description;
    }

    /** Returns truee iff this speed was measured, not set by the user.
     *  (That is, true iff the measured speed bit was set in the query reply. */
    public boolean isMeasured() {
        return isMeasured;
    }
    
    /**
     * Determines which speed is faster.  This returns a better measurement
     * than compareTo, which will be equal if the strings are the same.
     */
    public int isFaster(ResultSpeed other) {
        //We currently do NOT look at isMeasured property.
        return this.speed - other.speed;
    }
    
    /**
     * Returns true if the two ResultSpeeds are exactly the same speed.
     */
    @Override
    public boolean equals(Object other) {
        if(other instanceof ResultSpeed) {
            ResultSpeed o = (ResultSpeed)other;
            return o.speed == speed;
        } else {
            return false;
        }
    }    

    /**
     * Determines if these are the same 'visible' speeds.
     *
     * The order is still based on the speed differences if the string
     * is visibly different.
     */
    public int compareTo(ResultSpeed o) {
        int diff = this.speed  - (o).speed;
        if(diff == 0)
            return 0;
        else {
            int strDiff = description.compareTo(o.description);
            if(strDiff == 0)
                return 0;
            else
                return diff;
        }
    }
    
    /**
     * Returns true if the two ResultSpeeds have the same description.
     */
    public boolean isSameSpeed(ResultSpeed other) {
        return description.equals(other.description);
    }
        
    /** Same as stringValue(). */
    @Override
    public String toString() {
        return stringValue();
    }

    /*
    public static void main(String args[]) {
        //These unit tests don't work right because GUIUtils.speed2name
        //forces the GUI to initialize!
        ResultSpeed a=new ResultSpeed(100, true);
        ResultSpeed b=new ResultSpeed(101, false);
        Assert.that(a.compareTo(a)==0);
        Assert.that(b.compareTo(b)==0);
        Assert.that(a.compareTo(b)<0);
        Assert.that(b.compareTo(a)>0);
        Assert.that(ResultSpeed.max(a,b)==b);
        Assert.that(ResultSpeed.max(b,a)==b);
    }
    */
}    
