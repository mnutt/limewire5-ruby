package com.limegroup.gnutella.gui.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.limewire.concurrent.ExecutorsHelper;

/**
 * Static helper class that allows background tasks to be scheduled from the GUI.
 */
public class BackgroundExecutorService {

    /**
     * Queue for items to be run in the background.
     */
    private static final ExecutorService QUEUE = ExecutorsHelper.newProcessingQueue("DelayedGUI");
    
    private BackgroundExecutorService() {
    }

    /**
     * Runs the specified runnable in a different thread when it can.
     */
    public static void schedule(Runnable r) {
        QUEUE.execute(r);
    }
    
    public static <T> Future<T> submit(Callable<T> c) {
        return QUEUE.submit(c);
    }

}
