package org.limewire.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.service.ErrorCallback;
import org.limewire.service.ErrorCallbackStub;
import org.limewire.service.ErrorService;
import org.limewire.util.BaseTestCase;

public class ExecutorsHelperTest extends BaseTestCase {

    public ExecutorsHelperTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ExecutorsHelperTest.class);
    }
    
    public void testProcessingQueueStarts() throws Exception {
        Runner r1 = new Runner();
        ExecutorService service = ExecutorsHelper.newProcessingQueue("Sam");
        service.execute(r1);
        assertTrue(r1.getRanLatch().await(100, TimeUnit.MILLISECONDS));
        assertEquals("Sam", r1.getName());
    }
    
    public void testProcessingQueueReuseThread() throws Exception {
        Runner r1 = new Runner();
        Runner r2 = new Runner();
        ExecutorService service = ExecutorsHelper.newProcessingQueue("Sammy");
        service.execute(r1);
        assertTrue(r1.getRanLatch().await(100, TimeUnit.MILLISECONDS));
        assertEquals("Sammy", r1.getName());
        Thread.sleep(100); // let any potential renaming happen.
        assertEquals("Sammy", r1.getThread().getName());
        service.execute(r2);
        assertTrue(r2.getRanLatch().await(100, TimeUnit.MILLISECONDS));
        assertSame(r1.getThread(), r2.getThread());
        assertEquals("Sammy", r2.getName());
    }
    
    public void testProcessingQueueThreadLingersThenDies() throws Exception {
        Runner r1 = new Runner();
        ExecutorService service = ExecutorsHelper.newProcessingQueue("SammyB");
        service.execute(r1);
        assertTrue(r1.getRanLatch().await(100, TimeUnit.MILLISECONDS));
        Thread thread = r1.getThread();
        assertEquals("SammyB", r1.getThread().getName());
        assertTrue(thread.isAlive());
        Thread.sleep(3500);
        assertTrue(thread.isAlive());
        Thread.sleep(2000);
        assertFalse(thread.isAlive());
        assertEquals("SammyB", r1.getThread().getName());
    }
    
    public void testProcessingQueueGoesSequentially() throws Exception {
        CountDownLatch runLatch = new CountDownLatch(1);
        Runner r1 = new Runner(runLatch);
        Runner r2 = new Runner();
        ExecutorService service = ExecutorsHelper.newProcessingQueue("SammyB");
        service.execute(r1);
        service.execute(r2);
        assertTrue(r1.getStartedLatch().await(100, TimeUnit.MILLISECONDS));
        assertFalse(r2.getStartedLatch().await(100, TimeUnit.MILLISECONDS));
        assertFalse(r1.getRanLatch().await(100, TimeUnit.MILLISECONDS));
        runLatch.countDown();
        assertTrue(r1.getRanLatch().await(100, TimeUnit.MILLISECONDS));
        assertTrue(r2.getRanLatch().await(100, TimeUnit.MILLISECONDS));
        assertSame(r1.getThread(), r2.getThread());
        assertEquals("SammyB", r1.getName());        
        assertEquals("SammyB", r2.getName());
    }
    
    public void testProcessingQueueUsesDaemonThread() throws Exception {
        Runner r1 = new Runner();
        ExecutorService service = ExecutorsHelper.newProcessingQueue("B");
        service.execute(r1);
        assertTrue(r1.getRanLatch().await(100, TimeUnit.MILLISECONDS));
        assertTrue(r1.getThread().isDaemon());
    }
    
    public void testProcessingQueueExceptionContinues() throws Exception {
        ErrorCallback oldCallback = ErrorService.getErrorCallback();
        try {
            ErrorCallbackStub testCallback = new ErrorCallbackStub();
            ErrorService.setErrorCallback(testCallback);
            assertEquals(0, testCallback.getExceptionCount());
            CountDownLatch runLatch = new CountDownLatch(1);
            Runner r1 = new Runner(runLatch, true);
            Runner r2 = new Runner();
            ExecutorService service = ExecutorsHelper.newProcessingQueue("Exceptionary");
            service.execute(r1);
            service.execute(r2);
            assertTrue(r1.getStartedLatch().await(100, TimeUnit.MILLISECONDS));
            assertFalse(r2.getStartedLatch().await(100, TimeUnit.MILLISECONDS));
            assertFalse(r1.getRanLatch().await(100, TimeUnit.MILLISECONDS));
            assertEquals(0, testCallback.getExceptionCount());
            runLatch.countDown();
            assertTrue(r1.getRanLatch().await(100, TimeUnit.MILLISECONDS));
            Thread.sleep(100); // let the exception propogate.
            assertEquals(1, testCallback.getExceptionCount());
            assertTrue(r2.getRanLatch().await(100, TimeUnit.MILLISECONDS));
            assertEquals(1, testCallback.getExceptionCount());
        } finally {
            ErrorService.setErrorCallback(oldCallback);
        }
    }
    

    public void testNewFixedSizeThreadPoolStartsMoreThanOneThread() throws Exception {
        ErrorCallback oldCallback = ErrorService.getErrorCallback();
        try {
            ErrorCallbackStub testCallback = new ErrorCallbackStub();
            ErrorService.setErrorCallback(testCallback);
            
            ExecutorService service = ExecutorsHelper.newFixedSizeThreadPool(2, "fixedSizePool");
            CountDownLatch runLatch = new CountDownLatch(1);
            Runner blockedRunner = new Runner(runLatch, false);
            Runner secondRunner = new Runner();
            service.execute(blockedRunner);
            service.execute(secondRunner);
            Thread.yield();
            assertTrue(blockedRunner.getStartedLatch().await(100, TimeUnit.MILLISECONDS));
            assertFalse(blockedRunner.getRanLatch().await(100, TimeUnit.MILLISECONDS));
            assertTrue("Second runner not run", secondRunner.getRanLatch().await(500, TimeUnit.MILLISECONDS));
            runLatch.countDown();
            assertTrue(blockedRunner.getRanLatch().await(100, TimeUnit.MILLISECONDS));
            assertEquals(0, testCallback.getExceptionCount());
        }
        finally {
            ErrorService.setErrorCallback(oldCallback);
        }
    }

    private static class Runner implements Runnable {
        private volatile Thread thread;
        private volatile String name;
        private final CountDownLatch startedLatch = new CountDownLatch(1);
        private final CountDownLatch ranLatch = new CountDownLatch(1);
        private final CountDownLatch runLatch;
        private final boolean throwException;
    
        public Runner() {
            this(null);
        }
        
        public Runner(CountDownLatch runLatch) {
            this(runLatch, false);
        }
        
        public Runner(boolean throwException) {
            this(null, throwException);
        }
        
        public Runner(CountDownLatch runLatch, boolean throwException) {
            this.throwException = throwException;
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
           if(throwException)
               throw new RuntimeException("Abandon Hope.");
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
    