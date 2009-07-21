package org.limewire.listener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
     * and to remove it after.
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
        CountDownLatch latch = new CountDownLatch(2);
        EventListenerList<Foo> list = new EventListenerList<Foo>();
        BlockingListener l1 = new BlockingListener(latch);
        BlockingListener l2 = new BlockingListener(latch);
        list.addListener(l1);
        list.addListener(l2);
        assertNull(l1.thread);
        assertNull(l2.thread);        
        list.broadcast(new Foo());
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertNotEquals(Thread.currentThread(), l1.thread);
        assertNotEquals(Thread.currentThread(), l2.thread);
        assertNotEquals(l1.thread, l2.thread);
    }
    
    public void testBlockingEventWithQueueNames() throws Exception {
        CountDownLatch latch = new CountDownLatch(6);
        EventListenerList<Foo> list = new EventListenerList<Foo>();
        BlockingListener l1 = new BlockingListener(latch);
        BlockingListener l2 = new BlockingListener(latch);
        BlockingListenerQueueNameBar bar1 = new BlockingListenerQueueNameBar(latch);
        BlockingListenerQueueNameBar bar2 = new BlockingListenerQueueNameBar(latch);
        BlockingListenerQueueNameBob bob1 = new BlockingListenerQueueNameBob(latch);
        BlockingListenerQueueNameBob bob2 = new BlockingListenerQueueNameBob(latch);
        list.addListener(l1);
        list.addListener(l2);
        list.addListener(bar1);
        list.addListener(bar2);
        list.addListener(bob1);
        list.addListener(bob2);
        assertNull(l1.thread);
        assertNull(l2.thread);
        assertNull(bar1.thread);
        assertNull(bar2.thread); 
        assertNull(bob1.thread);
        assertNull(bob2.thread); 
        list.broadcast(new Foo());
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        
        assertNotEquals(Thread.currentThread(), l1.thread);
        assertNotEquals(Thread.currentThread(), l2.thread);
        assertNotEquals(Thread.currentThread(), bar1.thread);
        assertNotEquals(Thread.currentThread(), bar2.thread);
        assertNotEquals(Thread.currentThread(), bob1.thread);
        assertNotEquals(Thread.currentThread(), bob2.thread);
        
        assertEquals(bar1.thread, bar2.thread);
        assertEquals(bob1.thread, bob2.thread);
        
        assertNotEquals(l1.thread, l2.thread);
        assertNotEquals(l1.thread, bar1.thread);
        assertNotEquals(l1.thread, bob1.thread);
        assertNotEquals(l2.thread, bar1.thread);
        assertNotEquals(l2.thread, bob1.thread);
        assertNotEquals(bar1.thread, bob1.thread);
    }
    
    public void testMultipleEventsToNamedBlockingEventAreSequentialInAnotherThread() throws Exception {
        EventListenerList<Foo> list = new EventListenerList<Foo>();
        
        final Foo foo1 = new Foo();
        final Foo foo2 = new Foo();
        final List<Foo> fooList = new CopyOnWriteArrayList<Foo>();
        final List<Thread> threadList = new CopyOnWriteArrayList<Thread>();
        final CountDownLatch foo1EntryLatch = new CountDownLatch(1);
        final CountDownLatch foo2EntryLatch = new CountDownLatch(1);
        final CountDownLatch foo1ExitLatch = new CountDownLatch(1);
        final CountDownLatch foo2ExitLatch = new CountDownLatch(1);

        list.addListener(new EventListener<Foo>() {
            @Override
            @BlockingEvent(queueName="foo")
            public void handleEvent(Foo foo) {
                try {
                    if(foo == foo1) {
                        assertTrue(foo1EntryLatch.await(2, TimeUnit.SECONDS));
                    } else if(foo == foo2) {
                        assertTrue(foo2EntryLatch.await(2, TimeUnit.SECONDS));
                    } else {
                        throw new IllegalStateException("wrong foo: " + foo);
                    }
                } catch(InterruptedException ie) {
                    throw new IllegalStateException(ie);
                }
                
                fooList.add(foo);
                threadList.add(Thread.currentThread());
                
                if(foo == foo1) {
                    foo1ExitLatch.countDown();
                } else if(foo == foo2) {
                    foo2ExitLatch.countDown();
                } else {
                    throw new IllegalStateException("wrong foo: " + foo);
                }
            }
        });        
        list.broadcast(foo1);
        list.broadcast(foo2);
        
        Thread.sleep(100);
        assertEmpty(fooList);
        foo1EntryLatch.countDown();
        assertTrue(foo1ExitLatch.await(100, TimeUnit.MILLISECONDS));
        assertEquals(1, fooList.size());
        assertSame(foo1, fooList.get(0));
        assertNotSame(Thread.currentThread(), threadList.get(0));
        
        foo2EntryLatch.countDown();
        assertTrue(foo2ExitLatch.await(100, TimeUnit.MILLISECONDS));
        assertEquals(2, fooList.size());
        assertSame(foo2, fooList.get(1));
        assertSame(threadList.get(0), threadList.get(1));
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
        private final CountDownLatch latch;
        private volatile Thread thread;
        
        public BlockingListener(CountDownLatch latch) {
            this.latch = latch;
        }        
        
        @Override
        @BlockingEvent
        public void handleEvent(Foo event) {
            this.thread = Thread.currentThread();
            try { Thread.sleep(100); } catch(InterruptedException ix) {}
            latch.countDown();
        }        
    }
    
    private static class BlockingListenerQueueNameBar implements EventListener<Foo> {
        private final CountDownLatch latch;
        private volatile Thread thread;
        
        public BlockingListenerQueueNameBar(CountDownLatch latch) {
            this.latch = latch;
        }
        
        @Override
        @BlockingEvent(queueName="bar")
        public void handleEvent(Foo event) {
            this.thread = Thread.currentThread();
            try { Thread.sleep(100); } catch(InterruptedException ix) {}
            latch.countDown();
        }
    }
    
    private static class BlockingListenerQueueNameBob implements EventListener<Foo> {
        private final CountDownLatch latch;
        private volatile Thread thread;
        
        public BlockingListenerQueueNameBob(CountDownLatch latch) {
            this.latch = latch;
        }
        
        @Override
        @BlockingEvent(queueName="bob")
        public void handleEvent(Foo event) {
            this.thread = Thread.currentThread();
            try { Thread.sleep(100); } catch(InterruptedException ix) {}
            latch.countDown();
        }
    }
    
    private static class Foo {}
    private static class SubFoo extends Foo {}
    

}
