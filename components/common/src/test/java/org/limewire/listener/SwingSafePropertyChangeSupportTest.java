package org.limewire.listener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * A class to test the methods of EventListenerList: adding, removing.
 * Need to add tests for multiple adds, adds to existing lists, adding different 
 * types of listeners, as well as broadcasting.
 */
public class SwingSafePropertyChangeSupportTest extends BaseTestCase {
    
    public SwingSafePropertyChangeSupportTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(SwingSafePropertyChangeSupportTest.class);
    }
    
    public void testSwing() throws Exception {
        final PropertyChangeSupport support = new SwingSafePropertyChangeSupport(this);
        Listener listener = new Listener();
        support.addPropertyChangeListener(listener);
        assertNull(listener.t);
        support.firePropertyChange("a", false, true);
        assertSame(Thread.currentThread(), listener.t);
        
        PropertyChangeListener[] listeners = support.getPropertyChangeListeners();
        assertEquals(1, listeners.length);
        assertSame(listener, listeners[0]);
        
        final Listener swingListener = new Listener();
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                support.addPropertyChangeListener(swingListener);
            }
        });
        
        listener.t = null;
        support.firePropertyChange("a", false, true);
        assertSame(Thread.currentThread(), listener.t);
        final AtomicReference<Thread> swingThread = new AtomicReference<Thread>();
        SwingUtilities.invokeAndWait(new Runnable() { public void run() {
            swingThread.set(Thread.currentThread());
        }});
        assertSame(swingThread.get(), swingListener.t);
        
        listeners = support.getPropertyChangeListeners();
        assertEquals(2, listeners.length);
        assertSame(listener, listeners[0]);
        assertSame(swingListener, listeners[1]);
        
        support.removePropertyChangeListener(swingListener);
        listeners = support.getPropertyChangeListeners();
        assertEquals(1, listeners.length);
        assertSame(listener, listeners[0]);
        
        support.removePropertyChangeListener(listener);
        listeners = support.getPropertyChangeListeners();
        assertEquals(0, listeners.length);
    }
    
    private static class Listener implements PropertyChangeListener {
        volatile Thread t;
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            this.t = Thread.currentThread();
        }
    }
}
