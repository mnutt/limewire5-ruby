package org.limewire.core.settings;

/**
 * Constants used for speeds.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class SpeedConstants {
	
    // STRINGS FOR DIFFERENT CONNECTION SPEEDS
    public static final String MODEM_SPEED = "Modem";
    public static final String CABLE_SPEED = "Cable/DSL";
    public static final String T1_SPEED    = "T1";
    public static final String T3_SPEED    = "T3 or Higher";
    
	/**
	 * INTS ASSOCIATED WITH CONNECTION SPEED NAMES.
	 */
    public static final int MODEM_SPEED_INT         = 56;
    public static final int CABLE_SPEED_INT         = 350; // Must change ConnectionSettings too!
    public static final int T1_SPEED_INT            = 1000;
    public static final int T3_SPEED_INT            = 3000;
    public static final int THIRD_PARTY_SPEED_INT   = Integer.MAX_VALUE - 2;
    
    public static final int MIN_SPEED_INT	= 0;
    public static final int MAX_SPEED_INT	= 20000;

    public static final int MODEM_SWARM = 2;
    public static final int T1_SWARM = 8;
    public static final int T3_SWARM = 10;
    
   
    
    /** 
     * Converts the following bandwidth value, in kbytes/second, to 
     *  a human readable string. 
     */
    public static String speed2name(long rate) {
        if (rate<=MODEM_SPEED_INT)
            return MODEM_SPEED;
        else if (rate<=CABLE_SPEED_INT)
            return CABLE_SPEED;
        else if (rate<=T1_SPEED_INT)
            return T1_SPEED;
        else
            return T3_SPEED;
    }
}
