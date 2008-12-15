package org.limewire.http.entity;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import junit.framework.Test;

import org.limewire.concurrent.ManagedThread;
import org.limewire.http.HttpTestUtils;
import org.limewire.nio.ByteBufferCache;
import org.limewire.util.BaseTestCase;

public class FilePieceReaderTest extends BaseTestCase {

    private File file;

    private MyPieceListener listener;

    private byte[] data;

    private int read;

    private FilePieceReader reader;

    public FilePieceReaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FilePieceReaderTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        listener = new MyPieceListener();
        read = 0;
    }

    @Override
    protected void tearDown() throws Exception {
        if (reader != null) {
            if (!reader.isShutdown()) {
                reader.shutdownAndWait(5000);
            }
            reader = null;
        }
    }

    public void testRead() throws Exception {
        createFile(50000);

        reader = new FilePieceReader(new ByteBufferCache(), file, 0, (int) file
                .length(), listener);
        reader.start();

        while (read < file.length()) {
            assertGreaterThan(0, listener.waitForNotification());
            Piece piece = reader.next();
            if (read == 0) {
                assertNotNull("Notification should not have been sent", piece);
            }
            while (piece != null) {
                assertEquals(piece.getBuffer().limit(), piece.getLength());
                assertEquals(piece.getBuffer().limit(), piece.getBuffer()
                        .remaining());
                assertEquals(read, piece.getOffset());
                assertEqualsToData(data, piece);
                read += piece.getBuffer().limit();
                reader.release(piece);
                if (read == file.length()) {
                    break;
                }
                piece = reader.next();
            }
        }
    }

    public void testRelease() throws Exception {
        int filesize = FilePieceReader.BUFFER_SIZE * 3;
        createFile(filesize);

        MockByteBufferCache cache = new MockByteBufferCache();
        assertEquals(0, cache.getHeapCacheSize());

        reader = new FilePieceReader(cache, file, 0, filesize, listener);
        assertEquals(0, cache.buffers);

        reader.start();

        Piece piece1 = getNext();
        assertGreaterThanOrEquals(1, cache.buffers);
        assertGreaterThanOrEquals(piece1.getLength(), cache.bytes);
        Piece piece2 = getNext();
        Piece piece3 = getNext();
        assertEquals(3, cache.buffers);
        assertEquals(filesize, cache.bytes);
        reader.release(piece1);
        assertEquals(3, cache.buffers);
        reader.release(piece3);
        assertEquals(3, cache.buffers);
        reader.shutdown();
        assertEquals(1, cache.buffers);
        assertEquals(FilePieceReader.BUFFER_SIZE, cache.bytes);
        reader.release(piece2);
        assertEquals(0, cache.buffers);
        assertEquals(0, cache.bytes);
    }

    public void testReleaseVerySmall() throws Exception {
        int filesize = FilePieceReader.BUFFER_SIZE - 1;
        createFile(filesize);

        MockByteBufferCache cache = new MockByteBufferCache();
        reader = new FilePieceReader(cache, file, 0, filesize, listener);
        reader.start();
        assertEquals(1, cache.buffers);
    }

    public void testReleaseSmall() throws Exception {
        int filesize = FilePieceReader.BUFFER_SIZE + 1;
        createFile(filesize);

        MockByteBufferCache cache = new MockByteBufferCache();
        reader = new FilePieceReader(cache, file, 0, filesize, listener);
        reader.start();
        assertEquals(2, cache.buffers);
    }

    public void testReadTooMuch() throws Exception {
        int filesize = FilePieceReader.BUFFER_SIZE;
        createFile(filesize);

        MockByteBufferCache cache = new MockByteBufferCache();
        reader = new FilePieceReader(cache, file, 0, filesize, listener);
        reader.start();
        getNext();
        try {
            Piece piece = getNext();
            fail("Expected EOFException, got: " + piece);
        } catch (EOFException e) {
        }
    }

    public void testReadException() throws Exception {
        int filesize = 20000;
        createFile(filesize);

        MockByteBufferCache cache = new MockByteBufferCache();
        reader = new FilePieceReader(cache, file, 0, filesize, listener);
        reader.start();
        Piece piece1 = getNext();
        reader.failed(new IOException());
        reader.waitForShutdown(1000);
        assertEquals(1, cache.buffers);
        try {
            Piece piece2 = getNext();
            fail("Expected EOFException, got: " + piece2);
        } catch (EOFException e) {
        }
        assertEquals(1, cache.buffers);
        reader.release(piece1);
        assertEquals(0, cache.buffers);
    }

    public void testMultipleConcurrentReads() throws Exception {
        MockByteBufferCache cache = new MockByteBufferCache();
        class Runner implements Runnable {

            private File file;

            private byte[] data;

            private FilePieceReader reader;

            private IOException exception;

            Runner(ByteBufferCache cache) throws IOException {
                this.file = FilePieceReaderTest.this.file;
                this.data = FilePieceReaderTest.this.data;
                this.reader = new FilePieceReader(cache, file, 0, (int) file
                        .length(), listener);

            }

            public Runner(MockByteBufferCache cache, IOException exception)
                    throws IOException {
                this(cache);

                this.exception = exception;
            }

            public void run() {
                reader.start();
                try {
                    int read = 0;
                    Piece piece;
                    while ((piece = getNext(reader, file)) != null) {
                        assertEqualsToData(data, piece);
                        read += piece.getLength();
                        reader.release(piece);
                        if (exception != null) {
                            reader.failed(exception);
                            return;
                        }
                    }
                    assertEquals(file.length(), read);
                } finally {
                    try {
                        reader.shutdownAndWait(1000);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

        }
        ;

        createFile(500000);
        Runner runner1 = new Runner(cache);
        createFile(100000);
        Runner runner2 = new Runner(cache);
        createFile(1000000);
        Runner runner3 = new Runner(cache, new IOException());
        Runner runner4 = new Runner(cache);
        Runner runner5 = new Runner(cache);

        Thread t1 = new ManagedThread(runner1);
        Thread t2 = new ManagedThread(runner2);
        Thread t3 = new ManagedThread(runner3);
        Thread t4 = new ManagedThread(runner4);
        Thread t5 = new ManagedThread(runner5);

        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();

        t1.join(5000);
        t2.join(5000);
        t3.join(5000);
        t4.join(5000);
        t5.join(5000);

        assertFalse(t1.isAlive());
        assertFalse(t2.isAlive());
        assertFalse(t3.isAlive());
        assertFalse(t4.isAlive());
        assertFalse(t5.isAlive());
    }

    private Piece getNext() throws Exception {
        while (read < file.length()) {
            Piece piece = reader.next();
            if (piece != null) {
                read += piece.getBuffer().limit();
                return piece;
            }
        }
        throw new EOFException();
    }

    private Piece getNext(FilePieceReader reader, File file) {
        while (true) {
            Piece piece;
            try {
                piece = reader.next();
            } catch (EOFException e) {
                return null;
            }
            if (piece != null) {
                read += piece.getBuffer().limit();
                return piece;
            }
        }
    }

    private void assertEqualsToData(byte[] data, Piece piece) {
        byte[] expectedContent = new byte[piece.getBuffer().remaining()];
        System.arraycopy(data, (int) piece.getOffset(), expectedContent, 0,
                expectedContent.length);
        byte[] readContent = new byte[piece.getBuffer().remaining()];
        piece.getBuffer().get(readContent);
        assertEquals("Unexpected data in piece at offset: " + piece.getOffset()
                + ", length: " + expectedContent.length, //
                expectedContent, readContent);
    }

    private void createFile(int size) throws IOException {
        file = File.createTempFile("limewire", "");
        file.deleteOnExit();
        data = HttpTestUtils.writeRandomData(file, size);
    }

    private class MyPieceListener implements PieceListener {

        // private List<Piece> read = new ArrayList<Piece>();
        IOException exception;

        int notificationCount;

        public synchronized void readSuccessful() {
            notificationCount++;
            this.notify();
        }

        public synchronized void readFailed(IOException e) {
            exception = e;
            this.notify();
        }

        public synchronized int waitForNotification()
                throws InterruptedException, IOException {
            while (notificationCount == 0 && exception == null) {
                this.wait();
            }
            if (exception != null) {
                throw exception;
            }
            return notificationCount--;
        }

    }

}
