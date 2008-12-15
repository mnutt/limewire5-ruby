package org.limewire.concurrent;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;


public class ThreadExecutorTest extends BaseTestCase {

    public ThreadExecutorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ThreadExecutorTest.class);
    }
    
    public void testThreadStarts() throws Exception {
        Runner r1 = new Runner();
        ThreadExecutor.startThread(r1, "Name");
        assertTrue(r1.getRanLatch().await(100, TimeUnit.MILLISECONDS));
        assertEquals("Name", r1.getName());
    }
    
    public void testReuseThreads() throws Exception {
        Runner r1 = new Runner();
        ThreadExecutor.startThread(r1, "Name1");
        assertTrue(r1.getRanLatch().await(100, TimeUnit.MILLISECONDS));
        assertEquals("Name1", r1.getName());
        Thread.sleep(100); // let the thread rename happen.
        assertEquals("IdleThread", r1.getThread().getName());
        
        Runner r2 = new Runner();
        ThreadExecutor.startThread(r2, "Name2");
        assertTrue(r2.getRanLatch().await(100, TimeUnit.MILLISECONDS));
        assertSame(r1.getThread(), r2.getThread());
        assertEquals("Name2", r2.getName());
    }
    
    public void testThreadLingers() throws Exception {
        Runner r1 = new Runner();
        ThreadExecutor.startThread(r1, "Name");
        assertTrue(r1.getRanLatch().await(100, TimeUnit.MILLISECONDS));
        Thread thread = r1.getThread();
        Thread.sleep(100); // let the thread rename happen.
        assertEquals("IdleThread", r1.getThread().getName());
        assertTrue(thread.isAlive());
        Thread.sleep(3500);
        assertTrue(thread.isAlive());
        Thread.sleep(2000);
        assertFalse(thread.isAlive());
        assertEquals("IdleThread", r1.getThread().getName());
    }
    
    public void testManyThreadsAtOnce() throws Exception {
        CountDownLatch runLatch = new CountDownLatch(1);
        Runner r1 = new Runner(runLatch);
        Runner r2 = new Runner(runLatch);
        ThreadExecutor.startThread(r1, "Name1");
        ThreadExecutor.startThread(r2, "Name2");
        assertTrue(r1.getStartedLatch().await(100, TimeUnit.MILLISECONDS));
        assertTrue(r2.getStartedLatch().await(100, TimeUnit.MILLISECONDS));
        assertFalse(r1.getRanLatch().await(100, TimeUnit.MILLISECONDS));
        assertFalse(r2.getRanLatch().await(100, TimeUnit.MILLISECONDS));
        assertNotSame(r1.getThread(), r2.getThread());
        assertEquals("Name1", r1.getName());        
        assertEquals("Name2", r2.getName());
        runLatch.countDown();
        assertTrue(r1.getRanLatch().await(100, TimeUnit.MILLISECONDS));
        assertTrue(r2.getRanLatch().await(100, TimeUnit.MILLISECONDS));
    }
    
    public void testDaemonThread() throws Exception {
        Runner r1 = new Runner();
        ThreadExecutor.startThread(r1, "Name");
        assertTrue(r1.getRanLatch().await(100, TimeUnit.MILLISECONDS));
        assertTrue(r1.getThread().isDaemon());
    }    
    
    private static class Runner implements Runnable {
        private volatile Thread thread;
        private volatile String name;
        private final CountDownLatch startedLatch = new CountDownLatch(1);
        private final CountDownLatch ranLatch = new CountDownLatch(1);
        private final CountDownLatch runLatch;
    
        public Runner() {
            runLatch = null;
        }
        
        public Runner(CountDownLatch runLatch) {
            this.runLatch = runLatch;
        }
        
        public void run() {
            thread = Thread.currentThread();
            name = thread.getName();
            startedLatch.countDown();
            if(runLatch != null) {
                try {   
                    if(!runLatch.await(10, TimeUnit.SECONDS))
                        fail("never got notified!");
                } catch(InterruptedException ignore) {}
            }
            ranLatch.countDown();
        }
        
        public CountDownLatch getRanLatch() {
            return ranLatch;
        }

        public CountDownLatch getStartedLatch() {
            return startedLatch;
        }

        public Thread getThread() {
            return thread;
        }
        
        public String getName() {
            return name;
        }
    }    
}
