package org.limewire.util;

/** A utillity for boxing & unboxing, so that null values can be unboxed to be 0 or false. */
public class UnboxUtils {
    
    private UnboxUtils() {}

    public static int toInt(Integer obj) {
        if(obj == null) {
            return 0;
        } else {
            return obj;
        }
    }

    public static long toLong(Long obj) {
        if(obj == null) {
            return 0;
        } else {
            return obj;
        }
    }

    public static boolean toBoolean(Boolean obj) {
        if(obj == null) {
            return false;
        } else {
            return obj;
        }
    }
    
    

}
