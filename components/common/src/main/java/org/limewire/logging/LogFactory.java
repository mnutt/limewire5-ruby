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

    /**
     * @param clazz; can not be null
     * @return a Log named by the fully qualifed class name of clazz
     */
    public static Log getLog(Class clazz) {
        return getLog(clazz, null);
    }

    /**
     * @param clazz; cannot be null
     * @param category; can be null
     * @return a log with the name <code>category.classname</code>
     * where <code>classname</code> is the non-qualified name of the 
     * class.
     */
    public static Log getLog(Class clazz, String category) {
        // not synchronized as LogFactory is also not synchronized, Logs are created
        // at class load time which is synchronized
        org.apache.commons.logging.Log log;
        if (category == null) {
            log = org.apache.commons.logging.LogFactory.getLog(clazz);
        } else {
            log = org.apache.commons.logging.LogFactory.getLog(category + "." + clazz.getSimpleName());
        }
        Log decorator = logs.get(log);
        if (decorator == null) {
            decorator = new LogImpl(log);
            logs.put(log, decorator);
        }
        return decorator;
    }
    
}
