package org.limewire.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class AtomicLazyReferenceTest extends BaseTestCase {
    public AtomicLazyReferenceTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AtomicLazyReferenceTest.class);
    }
    
    public void testSingleton() {
        AbstractLazySingletonProvider<Object> o = new AbstractLazySingletonProvider<Object>() {
            @Override
            protected Object createObject() {
                return new Object();
            }
        };
        Object gotten = o.get();
        assertSame(gotten, o.get());
    }
    
    public void testLaziness() {
        StubAtomicLazyReference o = new StubAtomicLazyReference();
        assertNull(o.obj);
        Object gotten = o.get();
        assertNotNull(o.obj);
        assertSame(gotten, o.obj);
        assertSame(gotten, o.get());
        assertSame(o.get(), o.obj);
    }
    
    public void testSynchronization() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch runLatch = new CountDownLatch(2);
        final SwitchedAtomicLazyReference ref = new SwitchedAtomicLazyReference(latch);
        ExecutorService service = ExecutorsHelper.newThreadPool("SyncTester");
        
        Runnable r = new Runnable() {
            public void run() {
                runLatch.countDown();
                ref.get();
            }
        };
        
        assertEquals(0, ref.getCreates());
        assertNull(ref.obj);
        service.execute(r);
        service.execute(r);
        runLatch.await();
        Thread.sleep(500);
        latch.countDown();
        Thread.sleep(500);
        assertNotNull(ref.obj);
        assertEquals(1, ref.getCreates());
        assertSame(ref.obj, ref.get());
        assertEquals(1, ref.getCreates());
        
    }
    

    private static class StubAtomicLazyReference extends AbstractLazySingletonProvider<Object> {
        Object obj;

        @Override
        protected Object createObject() {
            obj = new Object();
            return obj;
        }
    }
    
    private static class SwitchedAtomicLazyReference extends StubAtomicLazyReference {
        private AtomicInteger creates = new AtomicInteger(); 
        private final CountDownLatch latch;
        
        public SwitchedAtomicLazyReference(CountDownLatch latch) {
            this.latch = latch;
        }
        
        @Override
        protected Object createObject() {
            creates.getAndIncrement();
            
            try {
                latch.await();
            } catch(InterruptedException failed) {
                fail(failed);
            }
            
            return super.createObject();
        }
        
        public int getCreates() {
            return creates.get();
        }
    }

}
