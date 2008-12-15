package org.limewire.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.limewire.service.ErrorCallback;
import org.limewire.service.ErrorCallbackStub;
import org.limewire.service.ErrorService;

import junit.framework.Test;

public class ListeningFutureSimpleTimerTest extends ListeningFutureTaskTest {
    
    private ScheduledListeningExecutorService sq;
    private ErrorCallback oldService;
    private ErrorCallbackStub serviceStub;

    public ListeningFutureSimpleTimerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ListeningFutureSimpleTimerTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        sq = new SimpleTimer(true);
        q = sq;
        
        oldService = ErrorService.getErrorCallback();
        serviceStub = new ErrorCallbackStub();
        ErrorService.setErrorCallback(serviceStub);
    }
    
    @Override
    protected void tearDown() throws Exception {
        ErrorService.setErrorCallback(oldService);
        assertEquals(0, serviceStub.getExceptionCount());
    }
    
    public void testIsSimpleTimer() {
        assertInstanceof(SimpleTimer.class, q);
    }
    
    public void testScheduledListensBeforeCompletes() throws Exception {
        RunWaiter waiter = new RunWaiter();
        sq.execute(waiter);
        
        Object result = new Object();
        Caller runner = new Caller(result);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);
        
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
    
    public void testScheduledListensBeforeCompletesWithException() throws Exception {
        RunWaiter waiter = new RunWaiter();
        sq.execute(waiter);
        
        Object result = new Object();
        Caller runner = new Caller(result, true);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);   
        
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
        
        assertEquals(1, serviceStub.getExceptionCount());
        assertEquals("Boo!", serviceStub.getException(0).getMessage());
        serviceStub.clear();
    }
    
    public void testScheduledListensBeforeCompletesCancelsWithoutRun() throws Exception {
        RunWaiter waiter = new RunWaiter();
        sq.execute(waiter);
        
        Object result = new Object();
        Caller runner = new Caller(result);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);       
        
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
    
    public void testScheduledListensBeforeCompletesCancelsDuringRun() throws Exception {
        Object result = new Object();
        CallWaiter runner = new CallWaiter(result);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);    
        
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
        
        assertEquals(1, serviceStub.getExceptionCount());
        assertInstanceof(InterruptedException.class, serviceStub.getException(0).getCause());
        serviceStub.clear();
    }
    
    public void testScheduledListensBeforeCompletesCancelsDuringRunWithoutAllowedCausesCancelToo() throws Exception {
        Object result = new Object();
        CallWaiter runner = new CallWaiter(result);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);    
        
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
    
    public void testScheduledListensAfterCompletes() throws Exception {
        Object result = new Object();
        Caller runner = new Caller(result);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);
        waitForFuture(task); 
        
        Listener listener = new Listener();
        task.addFutureListener(listener);
        assertNotNull(listener.event);
        assertSame(result, listener.event.getResult());
        assertEquals(FutureEvent.Type.SUCCESS, listener.event.getType());
        assertNull(listener.event.getException());
        
        assertEquals(Thread.currentThread(), listener.thread);
    }
    
    public void testScheduledListensAfterCompletesWithException() throws Exception {
        Object result = new Object();
        Caller runner = new Caller(result, true);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);
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
        
        assertEquals(1, serviceStub.getExceptionCount());
        assertEquals("Boo!", serviceStub.getException(0).getMessage());
        serviceStub.clear();
    }
    
    public void testScheduledListenAfterCompletesCancelsWithoutRun() throws Exception {
        RunWaiter waiter = new RunWaiter();
        sq.execute(waiter);
        
        Object result = new Object();
        Caller runner = new Caller(result);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);
        
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
    
    public void testScheduledListensAfterCompletesCancelsDuringRun() throws Exception {
        Object result = new Object();
        CallWaiter runner = new CallWaiter(result);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS); 
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
        
        assertEquals(1, serviceStub.getExceptionCount());
        assertInstanceof(InterruptedException.class, serviceStub.getException(0).getCause());
        serviceStub.clear();
    }
    
    public void testScheduledListensAfterCompletesCancelsDuringRunWithoutAllowedCausesCancelToo() throws Exception {
        Object result = new Object();
        CallWaiter runner = new CallWaiter(result);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS); 
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
    
    public void testScheduledAnnotatedListensBeforeCompletes() throws Exception {
        RunWaiter waiter = new RunWaiter();
        sq.execute(waiter);
        
        Object result = new Object();
        Caller runner = new Caller(result);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);      
        
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
    
    public void testScheduledAnnotatedListensBeforeCompletesWithException() throws Exception {
        RunWaiter waiter = new RunWaiter();
        sq.execute(waiter);
        
        Object result = new Object();
        Caller runner = new Caller(result, true);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 100, TimeUnit.MILLISECONDS);  
        
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
        
        assertEquals(1, serviceStub.getExceptionCount());
        assertEquals("Boo!", serviceStub.getException(0).getMessage());
        serviceStub.clear();
    }
    
    public void testScheduledAnnotatedListensBeforeCompletesCancelsWithoutRun() throws Exception {
        RunWaiter waiter = new RunWaiter();
        sq.execute(waiter);
        
        Object result = new Object();
        Caller runner = new Caller(result);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);  
        
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
    
    public void testScheduledAnnotatedListensBeforeCompletesCancelsDuringRun() throws Exception {
        Object result = new Object();
        CallWaiter runner = new CallWaiter(result);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);
        
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
        
        assertEquals(1, serviceStub.getExceptionCount());
        assertInstanceof(InterruptedException.class, serviceStub.getException(0).getCause());
        serviceStub.clear();
    }
    
    public void testScheduledAnnotatedListensBeforeCompletesCancelsDuringRunWithoutAllowedCausesCancelToo() throws Exception {
        Object result = new Object();
        CallWaiter runner = new CallWaiter(result);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);  
        
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
    
    public void testScheduledAnnotatedListensAfterCompletes() throws Exception {
        Object result = new Object();
        Caller runner = new Caller(result);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);
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
    
    public void testScheduledAnnotatedListensAfterCompletesWithException() throws Exception {
        Object result = new Object();
        Caller runner = new Caller(result, true);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);
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
        
        assertEquals(1, serviceStub.getExceptionCount());
        assertEquals("Boo!", serviceStub.getException(0).getMessage());
        serviceStub.clear();
    }
    
    public void testScheduledAnnotatedListenAfterCompletesCancelsWithoutRun() throws Exception {
        RunWaiter waiter = new RunWaiter();
        sq.execute(waiter);
        
        Object result = new Object();
        Caller runner = new Caller(result);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);
        
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
    
    public void testScheduledAnnotatedListensAfterCompletesCancelsDuringRun() throws Exception {
        Object result = new Object();
        CallWaiter runner = new CallWaiter(result);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);
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
        
        assertEquals(1, serviceStub.getExceptionCount());
        assertInstanceof(InterruptedException.class, serviceStub.getException(0).getCause());
        serviceStub.clear();
    }
    
    public void testScheduledAnnotatedListensAfterCompletesCancelsDuringRunWithoutAllowedCausesCancelToo() throws Exception {
        Object result = new Object();
        CallWaiter runner = new CallWaiter(result);
        ListeningFuture<Object> task = sq.schedule((Callable<Object>)runner, 500, TimeUnit.MILLISECONDS);
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
    
    private class CallWaiter extends RunWaiter implements Callable<Object> {
        private final Object result;

        public CallWaiter(Object result) {
            this.result = result;
        }

        @Override
        public Object call() throws Exception {
            run();
            return result;
        }
    }
    
    private class Caller extends Runner implements Callable<Object> {
        private final Object result;
        
        public Caller(Object result) {
            this(result, false);
        }
        
        public Caller(Object result, boolean throwException) {
            super(throwException);
            this.result = result;
        }
        
        @Override
        public Object call() throws Exception {
            run();
            return result;
        }
    }    
}
