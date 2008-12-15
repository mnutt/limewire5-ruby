package org.limewire.ui.swing.util;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import javax.swing.SwingUtilities;


public class SwingUtils {
    
    private SwingUtils() {}

    /**
     * Calls {@link SwingUtilities#invokeAndWait(Runnable)}
     * only if this is not currently on the Swing thread.
     */
    public static void invokeAndWait(Runnable runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
        } else {
            try {
                EventQueue.invokeAndWait(runnable);
            } catch (InvocationTargetException ite) {
                Throwable t = ite.getTargetException();
                if(t instanceof Error) {
                    throw (Error)t;
                } else if(t instanceof RuntimeException) {
                    throw (RuntimeException)t;
                } else {
                    throw new UndeclaredThrowableException(t);
                }
            } catch(InterruptedException ignored) {
                throw new RuntimeException(ignored);
            }
        }
    }
    
    /**
     * Calls {@link SwingUtilities#invokeLater(Runnable)} only
     * if this is not currently on the Swing thread.
     */
    public static void invokeLater(Runnable runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
        } else {
            EventQueue.invokeLater(runnable);
        }
    }

}
