package org.limewire.listener;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.limewire.concurrent.ManagedThread;
import org.limewire.util.AssertComparisons;

public class PendingEventMulticasterTest extends TestCase {
    
    public void testOrder() throws Exception {
        final PendingEventMulticasterImpl<Object> multicaster = new PendingEventMulticasterImpl<Object>();
        Listener listener = new Listener();
        multicaster.addListener(listener);
        
        final int totalSent = 50000;
        
        final CountDownLatch endLatch = new CountDownLatch(500);
        final CountDownLatch startLatch = new CountDownLatch(1);
        Runnable runner = new Runnable() {
            private int count = 0;
            
            @Override
            public void run() {
                try {
                    assertTrue(startLatch.await(1, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                while(true) {
                    synchronized(this) {
                        if(count > totalSent-1) {
                            break;
                        }
                        multicaster.addPendingEvent(count++);
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    multicaster.firePendingEvents();
                }
                endLatch.countDown();
            }
        };
        
        for(int i = 0; i < endLatch.getCount(); i++) {
            new ManagedThread(runner).start();
        }
        startLatch.countDown();
        assertTrue(endLatch.await(10, TimeUnit.SECONDS));
        
        assertEquals(totalSent, listener.received.size());
        int i = 0;
        for(Object o : listener.received) {
            assertEquals(i, o);
            i++;
        }
        
        // Make sure we received events on more than one thread.
        AssertComparisons.assertGreaterThan(1, listener.threads.size());
    }
    

    private static class Listener implements EventListener<Object> {
        private final Queue<Object> received = new ConcurrentLinkedQueue<Object>();
        private final ConcurrentHashMap<Thread, Thread> threads = new ConcurrentHashMap<Thread, Thread>();

        @Override
        public void handleEvent(Object event) {
            received.add(event);
            threads.put(Thread.currentThread(), Thread.currentThread());
        }
    }
}
