package org.limewire.logging;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Mimics and wraps {@link org.apache.commons.logging.LogFactory} and implements a
 * subset of its methods, but returns {@link Log} instead of 
 * {@link org.apache.commons.logging.Log} instances.
 */
public class LogFactory {
    
    private static Map<org.apache.commons.logging.Log, Log> logs = new IdentityHashMap<org.apache.commons.logging.Log, Log>();

    public static Log getLog(Class clazz) {
        // not synchronized as LogFactory is also not synchronized, Logs are created
        // at class load time which is synchronized
        org.apache.commons.logging.Log log = org.apache.commons.logging.LogFactory.getLog(clazz);
        Log decorator = logs.get(log);
        if (decorator == null) {
            decorator = new LogImpl(log);
            logs.put(log, decorator);
        }
        return decorator;
    }
    
}
