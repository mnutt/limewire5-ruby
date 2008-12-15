package org.limewire.util;

/**
 * Central location to setup a runtime ErrorService callback that
 * delegates calls to the given class.
 */
public class ErrorUtils {
    private ErrorUtils() {}
    
    /**
     * Sets up ErrorService's callback to a dynamic proxy that 
     * forwards the calls to this class.  This ass-backwards way
     * of setting up ErrorService must be used because this class
     * doesn't have compile-time access to ErrorService nor ErrorCallback.
     * 
     * This will never throw an exception.  Instead, it will return false
     * if the callback couldn't be setup.
     */
    public static boolean setCallback(Object x) {
        try {
            Class errorCallbackClass = Class.forName("org.limewire.service.ErrorCallback");
            Object errorCallbackDelegate = DuckType.implement(errorCallbackClass, x);
            Class errorServiceClass = Class.forName("org.limewire.service.ErrorService");
            PrivilegedAccessor.invokeMethod(errorServiceClass,
                                            "setErrorCallback",
                                            new Object[] { errorCallbackDelegate },
                                            new Class[] { errorCallbackClass } );
        } catch (Throwable t) {
            System.err.println("WARNING: Could not setup ErrorCallback within ErrorService.");
            return false;
        }
        
        return true;
    }
    
}
