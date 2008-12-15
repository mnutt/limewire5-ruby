package org.limewire.http;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.limewire.nio.NIODispatcher;

public class HttpTestUtils {

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

    public static byte[] writeRandomData(File file, int size)
            throws IOException {
        byte[] data = new byte[size];
        Random r = new Random();
        r.nextBytes(data);
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
            raf.write(data);
        } finally {
            raf.close();
        }
        return data;
    }

    public static void writeData(File file, String data) throws IOException {
        writeData(file, data.getBytes("US-ASCII"));
    }

    public static void writeData(File file, byte[] data) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
            raf.write(data);
        } finally {
            raf.close();
        }
    }
    
}
