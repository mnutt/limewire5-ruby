package org.limewire.security;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.concurrent.SimpleTimer;
import org.limewire.util.BaseTestCase;

public class MACCalculatorRotatorTest extends BaseTestCase {

    public MACCalculatorRotatorTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(MACCalculatorRotatorTest.class);
    }
    
    public void testKeyGeneratorsAreRotatedAndExpired() {
        
        SchedulingTestThreadPool pool = new SchedulingTestThreadPool();
        MACCalculatorRotator rotator = new MACCalculatorRotator(pool, new TEAMACCalculatorFactory(),
                new SettingsProvider() {
                    public long getChangePeriod() {
                        return 1;
                    }
                    public long getGracePeriod() {
                        return 0;
                    }
        });
        
        MACCalculator generator = rotator.getCurrentMACCalculator();
        assertEquals(1, rotator.getValidMACCalculators().length);
        assertContains(Arrays.asList(rotator.getValidMACCalculators()), generator);
        
        // run rotate
        Runnable r = pool.r;
        pool.r = null;
        r.run();
        
        assertEquals(2, rotator.getValidMACCalculators().length);
        MACCalculator generatorNew = rotator.getCurrentMACCalculator();
        assertNotSame(generator, generatorNew);
        assertContains(Arrays.asList(rotator.getValidMACCalculators()), generator);
        assertContains(Arrays.asList(rotator.getValidMACCalculators()), generatorNew);
        
        // expire old key
        r = pool.r2;
        pool.r2 = null;
        r.run();
        
        assertEquals(1, rotator.getValidMACCalculators().length);
        MACCalculator generator3 = rotator.getCurrentMACCalculator();
        assertSame(generatorNew, generator3);
        assertContains(Arrays.asList(rotator.getValidMACCalculators()), generator3);
        assertNotContains(Arrays.asList(rotator.getValidMACCalculators()), generator);
    }
    
    public void testGracePeriodIsHonored() throws Exception {
        
        WrappingSchedulingTestThreadPool pool = new WrappingSchedulingTestThreadPool();
        
        MACCalculatorRotator rotator = new MACCalculatorRotator(pool, new TEAMACCalculatorFactory(),
                new SettingsProvider() {
                    public long getChangePeriod() {
                        return 500;
                    }
                    public long getGracePeriod() {
                        return 250;
                    }
        });
        
        assertEquals(1, rotator.getValidMACCalculators().length);
        
        pool.r.waitForRunnable();
        
        assertEquals(2, rotator.getValidMACCalculators().length);
        
        pool.r2.waitForRunnable();
        
        assertEquals(1, rotator.getValidMACCalculators().length);
        
        pool.r.waitForRunnable();
        
        assertEquals(2, rotator.getValidMACCalculators().length);
    }
    
    public void testInvalidSettings() throws Exception {
        try {
            new MACCalculatorRotator(new SchedulingTestThreadPool(), 
                    new TEAMACCalculatorFactory(),
                    new SettingsProvider() {
                public long getChangePeriod() {
                    return 500;
                }
                public long getGracePeriod() {
                    return 2500;
                }
            });
            fail("constructed rotator with grace > expiry");
        } catch (IllegalArgumentException expected ){}
       
    }

    private static class WrappingSchedulingTestThreadPool extends AbstractExecutorService implements ScheduledExecutorService {
        
        private ScheduledExecutorService pool = new SimpleTimer(true);
        
        private NotifyinRunnable r;
        
        private NotifyinRunnable r2;
        
        private int modCounter = 0;
        
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            if ((modCounter++ % 2) == 0) {
                this.r = new NotifyinRunnable(command);
                return pool.schedule(this.r, delay, unit);
            }
            else {
                this.r2 = new NotifyinRunnable(command);
                return pool.schedule(this.r2, delay, unit);
            }
        }

        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        public boolean isShutdown() {
            throw new UnsupportedOperationException();
        }

        public boolean isTerminated() {
            throw new UnsupportedOperationException();
        }

        public void shutdown() {
            throw new UnsupportedOperationException();
        }

        public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<?> submit(Runnable task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            throw new UnsupportedOperationException();
        }

        public void execute(Runnable command) {
            throw new UnsupportedOperationException();
        }
                
    }
    
    private static class NotifyinRunnable implements Runnable {

        private Runnable r;
        
        public NotifyinRunnable(Runnable r) {
            this.r = r;
        }
        
        public synchronized void run() {
            r.run();
            r = null;
            notify();
        }
        
        public synchronized void waitForRunnable() throws InterruptedException {
            while (r != null) {
                wait();
            }
        }
        
    }
    
    private static class SchedulingTestThreadPool extends AbstractExecutorService implements ScheduledExecutorService {

        Runnable r;
        Runnable r2;
        
        
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            if (this.r == null) {
                this.r = command;
            }
            else {
                r2 = command;
            }
            return null;
        }

        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            throw new UnsupportedOperationException();
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        public boolean isShutdown() {
            throw new UnsupportedOperationException();
        }

        public boolean isTerminated() {
            throw new UnsupportedOperationException();
        }

        public void shutdown() {
            throw new UnsupportedOperationException();
        }

        public List<Runnable> shutdownNow() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Future<?> submit(Runnable task) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            throw new UnsupportedOperationException();
        }

        public void execute(Runnable command) {
            throw new UnsupportedOperationException();
        }
        
    }

}
