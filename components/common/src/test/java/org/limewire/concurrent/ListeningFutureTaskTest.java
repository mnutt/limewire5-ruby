package org.limewire.concurrent;

import java.awt.EventQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;

import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.util.BaseTestCase;

public class ListeningFutureTaskTest  extends BaseTestCase {
    
    protected ListeningExecutorService q;
    

    public ListeningFutureTaskTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ListeningFutureTaskTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        q = ExecutorsHelper.newProcessingQueue("PQ");
    }
    
    public void testListensBeforeCompletes() throws Exception {
        RunWaiter waiter = new RunWaiter();
        q.execute(waiter);
        
        Runner runner = new Runner();
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);
        
        Listener listener = new Listener();
        task.addFutureListener(listener);
        
        waiter.latch.countDown();
        assertTrue(listener.latch.await(1, TimeUnit.SECONDS));
        assertNotNull(listener.event);
        assertSame(result, listener.event.getResult());
        assertEquals(FutureEvent.Type.SUCCESS, listener.event.getType());
        assertNull(listener.event.getException());
        
        assertEquals(runner.thread, listener.thread);
    }
    
    public void testListensBeforeCompletesWithException() throws Exception {
        RunWaiter waiter = new RunWaiter();
        q.execute(waiter);
        
        Runner runner = new Runner(true);
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);      
        
        Listener listener = new Listener();
        task.addFutureListener(listener);
        
        waiter.latch.countDown();
        assertTrue(listener.latch.await(1, TimeUnit.SECONDS));
        assertNotNull(listener.event);
        assertNull(listener.event.getResult());
        assertEquals(FutureEvent.Type.EXCEPTION, listener.event.getType());
        ExecutionException ee = listener.event.getException();
        assertNotNull(ee);
        assertInstanceof(RuntimeException.class, ee.getCause());
        assertEquals("Boo!", ee.getCause().getMessage());
        
        assertEquals(runner.thread, listener.thread);
    }
    
    public void testListensBeforeCompletesCancelsWithoutRun() throws Exception {
        RunWaiter waiter = new RunWaiter();
        q.execute(waiter);
        
        Runner runner = new Runner();
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);        
        
        Listener listener = new Listener();
        task.addFutureListener(listener);
        
        task.cancel(false);
        assertTrue(listener.latch.await(1, TimeUnit.SECONDS));
        assertNotNull(listener.event);
        assertNull(listener.event.getResult());
        assertEquals(FutureEvent.Type.CANCELLED, listener.event.getType());
        assertNull(listener.event.getException());
        
        waiter.latch.countDown();
        
        assertEquals(Thread.currentThread(), listener.thread); // from cancel thread.
    }
    
    public void testListensBeforeCompletesCancelsDuringRun() throws Exception {
        RunWaiter runner = new RunWaiter();
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);      
        
        Listener listener = new Listener();
        task.addFutureListener(listener);
        
        runner.enter.await(1, TimeUnit.SECONDS);
        task.cancel(true);
        assertTrue(listener.latch.await(1, TimeUnit.SECONDS));
        assertNotNull(listener.event);
        assertNull(listener.event.getResult());
        assertEquals(FutureEvent.Type.CANCELLED, listener.event.getType());
        assertNull(listener.event.getException());
        
        runner.latch.countDown();
        Thread.sleep(100);
        assertTrue(runner.interrupted);
        
        assertEquals(Thread.currentThread(), listener.thread); // from cancel thread.
    }
    
    public void testListensBeforeCompletesCancelsDuringRunWithoutAllowedCausesCancelToo() throws Exception {
        RunWaiter runner = new RunWaiter();
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);      
        
        Listener listener = new Listener();
        task.addFutureListener(listener);
        
        runner.enter.await(1, TimeUnit.SECONDS);
        task.cancel(false);
        assertTrue(listener.latch.await(1, TimeUnit.SECONDS));
        assertNotNull(listener.event);
        assertNull(listener.event.getResult());
        assertEquals(FutureEvent.Type.CANCELLED, listener.event.getType());
        assertNull(listener.event.getException());
        
        runner.latch.countDown();
        Thread.sleep(100);
        assertFalse(runner.interrupted);
        
        assertEquals(Thread.currentThread(), listener.thread); // from cancel thread.
    }
    
    public void testListensAfterCompletes() throws Exception {
        Runner runner = new Runner();
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);
        waitForFuture(task); 
        
        Listener listener = new Listener();
        task.addFutureListener(listener);
        assertNotNull(listener.event);
        assertSame(result, listener.event.getResult());
        assertEquals(FutureEvent.Type.SUCCESS, listener.event.getType());
        assertNull(listener.event.getException());
        
        assertEquals(Thread.currentThread(), listener.thread);
    }
    
    public void testListensAfterCompletesWithException() throws Exception {
        Runner runner = new Runner(true);
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result); 
        waitForFuture(task);       
        
        Listener listener = new Listener();
        task.addFutureListener(listener);
        assertNotNull(listener.event);
        assertNull(listener.event.getResult());
        assertEquals(FutureEvent.Type.EXCEPTION, listener.event.getType());
        ExecutionException ee = listener.event.getException();
        assertNotNull(ee);
        assertInstanceof(RuntimeException.class, ee.getCause());
        assertEquals("Boo!", ee.getCause().getMessage());
        
        assertEquals(Thread.currentThread(), listener.thread);
    }
    
    public void testListenAfterCompletesCancelsWithoutRun() throws Exception {
        RunWaiter waiter = new RunWaiter();
        q.execute(waiter);
        
        Runner runner = new Runner();
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);
        
        task.cancel(false);
        waitForFuture(task);
        
        Listener listener = new Listener();
        task.addFutureListener(listener);
        assertNotNull(listener.event);
        assertNull(listener.event.getResult());
        assertEquals(FutureEvent.Type.CANCELLED, listener.event.getType());
        assertNull(listener.event.getException());
        
        waiter.latch.countDown();
        
        assertEquals(Thread.currentThread(), listener.thread);
    }
    
    public void testListensAfterCompletesCancelsDuringRun() throws Exception {
        RunWaiter runner = new RunWaiter();
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);
        runner.enter.await(1, TimeUnit.SECONDS);
        task.cancel(true);
        waitForFuture(task);
        Listener listener = new Listener();
        task.addFutureListener(listener);
        assertNotNull(listener.event);
        assertNull(listener.event.getResult());
        assertEquals(FutureEvent.Type.CANCELLED, listener.event.getType());
        assertNull(listener.event.getException()); 
        Thread.sleep(100);
        assertTrue(runner.interrupted);
        
        assertEquals(Thread.currentThread(), listener.thread);
    }
    
    public void testListensAfterCompletesCancelsDuringRunWithoutAllowedCausesCancelToo() throws Exception {
        RunWaiter runner = new RunWaiter();
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);      
        runner.enter.await(1, TimeUnit.SECONDS);
        task.cancel(false);
        waitForFuture(task);        
        Listener listener = new Listener();
        task.addFutureListener(listener);
        assertNotNull(listener.event);
        assertNull(listener.event.getResult());
        assertEquals(FutureEvent.Type.CANCELLED, listener.event.getType());
        assertNull(listener.event.getException());
        Thread.sleep(100);
        assertFalse(runner.interrupted);
        
        assertEquals(Thread.currentThread(), listener.thread);
    }
    
    public void testAnnotatedListensBeforeCompletes() throws Exception {
        RunWaiter waiter = new RunWaiter();
        q.execute(waiter);
        
        Runner runner = new Runner();
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);     
        
        AnnotatedListener listener = new AnnotatedListener();
        task.addFutureListener(listener);
        
        waiter.latch.countDown();
        assertTrue(listener.latch.await(1, TimeUnit.SECONDS));
        assertNotNull(listener.event);
        assertSame(result, listener.event.getResult());
        assertEquals(FutureEvent.Type.SUCCESS, listener.event.getType());
        assertNull(listener.event.getException());
        
        assertEquals(dispatchThread(), listener.thread);
    }
    
    public void testAnnotatedListensBeforeCompletesWithException() throws Exception {
        RunWaiter waiter = new RunWaiter();
        q.execute(waiter);
        
        Runner runner = new Runner(true);
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);       
        
        AnnotatedListener listener = new AnnotatedListener();
        task.addFutureListener(listener);
        
        waiter.latch.countDown();
        assertTrue(listener.latch.await(1, TimeUnit.SECONDS));
        assertNotNull(listener.event);
        assertNull(listener.event.getResult());
        assertEquals(FutureEvent.Type.EXCEPTION, listener.event.getType());
        ExecutionException ee = listener.event.getException();
        assertNotNull(ee);
        assertInstanceof(RuntimeException.class, ee.getCause());
        assertEquals("Boo!", ee.getCause().getMessage());
        
        assertEquals(dispatchThread(), listener.thread);
    }
    
    public void testAnnotatedListensBeforeCompletesCancelsWithoutRun() throws Exception {
        RunWaiter waiter = new RunWaiter();
        q.execute(waiter);
        
        Runner runner = new Runner();
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);        
        
        AnnotatedListener listener = new AnnotatedListener();
        task.addFutureListener(listener);
        
        task.cancel(false);
        assertTrue(listener.latch.await(1, TimeUnit.SECONDS));
        assertNotNull(listener.event);
        assertNull(listener.event.getResult());
        assertEquals(FutureEvent.Type.CANCELLED, listener.event.getType());
        assertNull(listener.event.getException());
        
        waiter.latch.countDown();
        
        assertEquals(dispatchThread(), listener.thread); // from cancel thread.
    }
    
    public void testAnnotatedListensBeforeCompletesCancelsDuringRun() throws Exception {
        RunWaiter runner = new RunWaiter();
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);     
        
        AnnotatedListener listener = new AnnotatedListener();
        task.addFutureListener(listener);
        
        runner.enter.await(1, TimeUnit.SECONDS);
        task.cancel(true);
        assertTrue(listener.latch.await(1, TimeUnit.SECONDS));
        assertNotNull(listener.event);
        assertNull(listener.event.getResult());
        assertEquals(FutureEvent.Type.CANCELLED, listener.event.getType());
        assertNull(listener.event.getException());
        
        runner.latch.countDown();
        Thread.sleep(100);
        assertTrue(runner.interrupted);
        
        assertEquals(dispatchThread(), listener.thread); // from cancel thread.
    }
    
    public void testAnnotatedListensBeforeCompletesCancelsDuringRunWithoutAllowedCausesCancelToo() throws Exception {
        RunWaiter runner = new RunWaiter();
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);       
        
        AnnotatedListener listener = new AnnotatedListener();
        task.addFutureListener(listener);
        
        runner.enter.await(1, TimeUnit.SECONDS);
        task.cancel(false);
        assertTrue(listener.latch.await(1, TimeUnit.SECONDS));
        assertNotNull(listener.event);
        assertNull(listener.event.getResult());
        assertEquals(FutureEvent.Type.CANCELLED, listener.event.getType());
        assertNull(listener.event.getException());
        
        runner.latch.countDown();
        Thread.sleep(100);
        assertFalse(runner.interrupted);
        
        assertEquals(dispatchThread(), listener.thread); // from cancel thread.
    }
    
    public void testAnnotatedListensAfterCompletes() throws Exception {
        Runner runner = new Runner();
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);
        waitForFuture(task); 
        
        AnnotatedListener listener = new AnnotatedListener();
        task.addFutureListener(listener);
        assertTrue(listener.latch.await(1, TimeUnit.SECONDS));
        assertNotNull(listener.event);
        assertSame(result, listener.event.getResult());
        assertEquals(FutureEvent.Type.SUCCESS, listener.event.getType());
        assertNull(listener.event.getException());
        
        assertEquals(dispatchThread(), listener.thread);
    }
    
    public void testAnnotatedListensAfterCompletesWithException() throws Exception {
        Runner runner = new Runner(true);
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);  
        waitForFuture(task);       
        
        AnnotatedListener listener = new AnnotatedListener();
        task.addFutureListener(listener);
        assertTrue(listener.latch.await(1, TimeUnit.SECONDS));
        assertNotNull(listener.event);
        assertNull(listener.event.getResult());
        assertEquals(FutureEvent.Type.EXCEPTION, listener.event.getType());
        ExecutionException ee = listener.event.getException();
        assertNotNull(ee);
        assertInstanceof(RuntimeException.class, ee.getCause());
        assertEquals("Boo!", ee.getCause().getMessage());
        
        assertEquals(dispatchThread(), listener.thread);
    }
    
    public void testAnnotatedListenAfterCompletesCancelsWithoutRun() throws Exception {
        RunWaiter waiter = new RunWaiter();
        q.execute(waiter);
        
        Runner runner = new Runner();
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);
        
        task.cancel(false);
        waitForFuture(task);
        
        AnnotatedListener listener = new AnnotatedListener();
        task.addFutureListener(listener);
        assertTrue(listener.latch.await(1, TimeUnit.SECONDS));
        assertNotNull(listener.event);
        assertNull(listener.event.getResult());
        assertEquals(FutureEvent.Type.CANCELLED, listener.event.getType());
        assertNull(listener.event.getException());
        
        waiter.latch.countDown();
        
        assertEquals(dispatchThread(), listener.thread);
    }
    
    public void testAnnotatedListensAfterCompletesCancelsDuringRun() throws Exception {
        RunWaiter runner = new RunWaiter();
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);
        runner.enter.await(1, TimeUnit.SECONDS);
        task.cancel(true);
        waitForFuture(task);
        AnnotatedListener listener = new AnnotatedListener();
        task.addFutureListener(listener);
        assertTrue(listener.latch.await(1, TimeUnit.SECONDS));
        assertNotNull(listener.event);
        assertNull(listener.event.getResult());
        assertEquals(FutureEvent.Type.CANCELLED, listener.event.getType());
        assertNull(listener.event.getException());       
        Thread.sleep(100);
        assertTrue(runner.interrupted);
        
        assertEquals(dispatchThread(), listener.thread);
    }
    
    public void testAnnotatedListensAfterCompletesCancelsDuringRunWithoutAllowedCausesCancelToo() throws Exception {
        RunWaiter runner = new RunWaiter();
        Object result = new Object();
        ListeningFuture<Object> task = q.submit(runner, result);    
        runner.enter.await(1, TimeUnit.SECONDS);
        task.cancel(false);
        waitForFuture(task);        
        AnnotatedListener listener = new AnnotatedListener();
        task.addFutureListener(listener);
        assertTrue(listener.latch.await(1, TimeUnit.SECONDS));
        assertNotNull(listener.event);
        assertNull(listener.event.getResult());
        assertEquals(FutureEvent.Type.CANCELLED, listener.event.getType());
        assertNull(listener.event.getException());
        Thread.sleep(100);
        assertFalse(runner.interrupted);
        
        assertEquals(dispatchThread(), listener.thread);
    }
    
    protected Thread dispatchThread() throws Exception {
        final AtomicReference<Thread> threadRef = new AtomicReference<Thread>();
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                threadRef.set(Thread.currentThread());
            }
        });
        assertNotNull(threadRef.get());
        return threadRef.get();
    }
    
    protected void waitForFuture(Future<?> future) {
        try {
            future.get(1, TimeUnit.SECONDS);
        } catch(Throwable ignored) {}
    }
    
    protected static class AnnotatedListener implements EventListener<FutureEvent<Object>> {
        protected final CountDownLatch latch = new CountDownLatch(1);
        protected volatile FutureEvent<Object> event;
        protected volatile Thread thread;
        
        @Override
        @SwingEDTEvent
        public void handleEvent(FutureEvent<Object> event) {
            assert this.event == null;
            this.thread = Thread.currentThread();
            this.event = event;
            latch.countDown();
        }
    }
    
    protected static class Listener implements EventListener<FutureEvent<Object>> {
        protected final CountDownLatch latch = new CountDownLatch(1);
        protected volatile FutureEvent<Object> event;
        protected volatile Thread thread;
        
        @Override
        public void handleEvent(FutureEvent<Object> event) {
            assert this.event == null;
            this.event = event;
            this.thread = Thread.currentThread();
            latch.countDown();
        }
    }
    
    protected static class RunWaiter implements Runnable {
        protected CountDownLatch enter = new CountDownLatch(1);
        protected CountDownLatch latch = new CountDownLatch(1);
        protected volatile boolean interrupted;
        
        @Override
        public void run() {
            enter.countDown();
            
            try {
                latch.await();
            } catch(InterruptedException ie) {
                interrupted = true;
                throw new RuntimeException(ie);
            }
        }
    }
    
    protected static class Runner implements Runnable {
        private final boolean throwException;
        protected volatile Thread thread;
        
        public Runner() {
            this(false);
        }
        
        public Runner(boolean throwException) {
            this.throwException = throwException;
        }        
        
        @Override
        public void run() {
            thread = Thread.currentThread();
            if(throwException) {
                throw new RuntimeException("Boo!");
            }
        }
    }
    
}