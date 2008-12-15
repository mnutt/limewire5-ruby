package org.limewire.ui.swing.images;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.limewire.concurrent.ExecutorsHelper;

public class ImageExecutorService {
    /**
     * Queue for items to be run in the background.
     */
    private static final ExecutorService QUEUE = 
        ExecutorsHelper.newFixedSizeThreadPool(Runtime.getRuntime().availableProcessors(), "ImageLoader");
    
    private ImageExecutorService() {
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
