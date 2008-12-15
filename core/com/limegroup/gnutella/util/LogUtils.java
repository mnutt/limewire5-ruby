package com.limegroup.gnutella.util;

/**
 * A class for Log utilities.
 */
public final class LogUtils {
    
    private LogUtils() {
        
    }

    /**
     * Returns wheather or not the Log4J library is available
     */
    public static boolean isLog4JAvailable() {
        try {
            Class.forName("org.apache.log4j.LogManager");
            return true;
        } catch (ClassNotFoundException ignore) {
        } catch (NoClassDefFoundError ignore) {
        }
        
        return false;
    }
}
