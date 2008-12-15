package org.limewire.listener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * A class to test the methods of EventListenerList: adding, removing.
 * Need to add tests for multiple adds, adds to existing lists, adding different 
 * types of listeners, as well as broadcasting.
 */
public class EventListenerListTest extends BaseTestCase {
    
    public EventListenerListTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(EventListenerListTest.class);
    }
    
    /**
     * A simple test to add a listener (for which the key did not exist before),
     * and to remove it after
     */
    public void testAddRemoveListener() {
        EventListenerList<Object> list = new EventListenerList<Object>();
        Listener l = new Listener();
        list.addListener(l);
        assertTrue(list.removeListener(l));
        assertFalse(list.removeListener(l));
    }
    
    public void testInlineEvent() {
        EventListenerList<Object> list = new EventListenerList<Object>();
        Listener l = new Listener();
        list.addListener(l);
        assertNull(l.thread);
        list.broadcast(new Object());
        assertEquals(Thread.currentThread(), l.thread);   
    }
    
    public void testSwingProxyEvent() throws Exception {
        EventListenerList<Foo> list = new EventListenerList<Foo>();
        SwingProxyListener l = new SwingProxyListener();
        list.addListener(l);
        assertNull(l.thread);
        list.broadcast(new Foo());
        assertTrue(l.latch.await(1, TimeUnit.SECONDS));
        assertNotEquals(Thread.currentThread(), l.thread);
    }
    
    public void testSwingProxyEventWithSubclassedEvent() throws Exception {
        EventListenerList<Foo> list = new EventListenerList<Foo>();
        SwingProxyListener l = new SwingProxyListener();
        list.addListener(l);
        assertNull(l.thread);
        list.broadcast(new SubFoo());
        assertTrue(l.latch.await(1, TimeUnit.SECONDS));
        assertNotEquals(Thread.currentThread(), l.thread);
    }
    
    
    public void testBlockingEvent() throws Exception {
        EventListenerList<Foo> list = new EventListenerList<Foo>();
        BlockingListener l1 = new BlockingListener();
        BlockingListener l2 = new BlockingListener();
        list.addListener(l1);
        list.addListener(l2);
        assertNull(l1.thread);
        assertNull(l2.thread);        
        list.broadcast(new Foo());
        assertTrue(BlockingListener.latch.await(1, TimeUnit.SECONDS));
        assertNotEquals(Thread.currentThread(), l1.thread);
        assertNotEquals(Thread.currentThread(), l2.thread);
        assertNotEquals(l1.thread, l2.thread);
    }
    
    private static class Listener implements EventListener<Object> {
        private Thread thread; 
        public void handleEvent(Object event) {
            this.thread = Thread.currentThread();
        }
    }
    
    private static class SwingProxyListener implements EventListener<Foo> {
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile Thread thread;
        
        @Override
        @SwingEDTEvent
        public void handleEvent(Foo event) {
            this.thread = Thread.currentThread();
            latch.countDown();
        }
    }
    
    private static class BlockingListener implements EventListener<Foo> {
        private static final CountDownLatch latch = new CountDownLatch(2);
        private volatile Thread thread;
        
        @Override
        @BlockingEvent
        public void handleEvent(Foo event) {
            this.thread = Thread.currentThread();
            try { Thread.sleep(100); } catch(InterruptedException ix) {}
            latch.countDown();
        }        
    }
    
    private static class Foo {}
    private static class SubFoo extends Foo {}
    

}
