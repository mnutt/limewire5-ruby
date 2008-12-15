package org.limewire.nio;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class NIOTestUtils {

    /**
     * Waits for the NIO dispatcher to complete a processing pending events.
     */
    public static void waitForNIO() throws InterruptedException {
        Future<?> future = NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
            }
        });
        try {
            future.get();
        } catch(ExecutionException ee) {
            throw new IllegalStateException(ee);
        }
        
        // the runnable is run at the beginning of the processing cycle so 
        // we need a second runnable to make sure the cycle has been completed
        future = NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
            }
        });
        try {
            future.get();
        } catch(ExecutionException ee) {
            throw new IllegalStateException(ee);
        }
        
    }

}
