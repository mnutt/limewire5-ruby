package org.limewire.nio.timeout;

import java.util.concurrent.ScheduledExecutorService;

import org.limewire.nio.NIODispatcher;
import org.limewire.nio.NIOTestUtils;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.util.BaseTestCase;

public class NIOWatchdogTest extends BaseTestCase {

    public NIOWatchdogTest(String name) {
        super(name);
    }

    private static ScheduledExecutorService executor = NIODispatcher.instance().getScheduledExecutorService();
    public void testActivate() throws Exception {
        MyShutdownable shutdownable = new MyShutdownable();
        StalledUploadWatchdog watchdog = new StalledUploadWatchdog(50, executor);
        assertFalse(shutdownable.shutdown);
        
        Thread.sleep(50);
        NIOTestUtils.waitForNIO();
        assertFalse(shutdownable.shutdown);
        
        watchdog.activate(shutdownable);
        Thread.sleep(30);
        assertFalse(shutdownable.shutdown);
        Thread.sleep(20);
        NIOTestUtils.waitForNIO();
        assertTrue(shutdownable.shutdown);
        
        // reactivate
        shutdownable.shutdown = false;
        watchdog.activate(shutdownable);
        Thread.sleep(50);
        NIOTestUtils.waitForNIO();
        assertTrue(shutdownable.shutdown);
        
        // activate for never
        watchdog = new StalledUploadWatchdog(Long.MAX_VALUE, executor);
        shutdownable.shutdown = false;
        watchdog.activate(shutdownable);
        Thread.sleep(100); // if it were broken would overflow and execute right away
        NIOTestUtils.waitForNIO();
        assertFalse(shutdownable.shutdown);
        watchdog.deactivate();
    }
    
    public void testDeactivate() throws Exception {
        MyShutdownable shutdownable = new MyShutdownable();
        StalledUploadWatchdog watchdog = new StalledUploadWatchdog(50, executor);

        watchdog.activate(shutdownable);
        watchdog.deactivate();
        Thread.sleep(50);
        NIOTestUtils.waitForNIO();
        assertFalse(shutdownable.shutdown);
        
        watchdog.deactivate();
        assertFalse(shutdownable.shutdown);
    }

    public void testActivateOnNIOThread() throws Exception {
        final MyShutdownable shutdownable = new MyShutdownable();
        final StalledUploadWatchdog watchdog = new StalledUploadWatchdog(1000, executor);

        NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
                watchdog.activate(shutdownable);
            }            
        });
        NIOTestUtils.waitForNIO();
        Thread.sleep(50);
        assertFalse(shutdownable.shutdown);
        NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
                watchdog.deactivate();
            }            
        });
        NIOTestUtils.waitForNIO();
        assertFalse(shutdownable.shutdown);
    }
    
    private class MyShutdownable implements Shutdownable {
        private volatile boolean shutdown;

        public void shutdown() {
            this.shutdown = true;
        }

    }

}
