package org.limewire.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Random;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

/**
 * Tests {@link BandwidthThrottle} and {@link ThrottledOutputStream}.
 */
// TODO: test fairness when sharing one throttle among multiple streams
public class BandwidthThrottleTest extends BaseTestCase {

    /** Time per test. */
    private final int TIME = 1000; // 1 second

    /** bytesSent should be the desired value + or - FUDGE_FACTOR */
    private static final float FUDGE_FACTOR = 0.12f; // 12 percent

    private BandwidthThrottle throttle;

    private Random random;

    private long startTime;

    private long stopTime;

    private int bytesSent;

    private long expectedBytes;

    private PipedOutputStream pout;

    private PipedInputStream pin;

    private OutputStream out;

    public BandwidthThrottleTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BandwidthThrottleTest.class);
    }

    @Override
    protected void setUp() throws IOException {
        // each test sets rate
        throttle = new BandwidthThrottle(0);
        random = new Random();
        bytesSent = 0;

        pout = new PipedOutputStream();
        pin = new PipedInputStream(pout);
        out = new ThrottledOutputStream(pout, throttle);
    }

    /**
     * @param rate bytes / second
     */
    public void initExpectedBytes(int rate) {
        throttle.setRate(rate);
        expectedBytes = TIME / 1000 * rate;
        startTime = System.currentTimeMillis();
        stopTime = startTime + TIME; 
    }
    
    public void testBandwidthThrottleSlow() {
        initExpectedBytes(100);
        while (System.currentTimeMillis() < stopTime) {
            int bytesToSend = random.nextInt(12);
            bytesSent += throttle.request(bytesToSend);
        }
        assertLessThan("Sent " + bytesSent + " of " + expectedBytes, FUDGE_FACTOR
                * expectedBytes, Math.abs(bytesSent - expectedBytes));
    }

    public void testThrottledOutputStreamFast() throws Exception {
        initExpectedBytes(20 * 1000);
        while (System.currentTimeMillis() < stopTime) {
            byte b = (byte) random.nextInt();
            out.write(b);
            assertTrue("Bad byte", (byte) pin.read() == b);
            assertEquals("Too many bytes in stream", 0, pin.available());
            bytesSent++;
        }
        assertLessThan("Sent " + bytesSent + " of " + expectedBytes, FUDGE_FACTOR
                * expectedBytes, Math.abs(bytesSent - expectedBytes));
    }

    public void testThrottledOutputStreamWritePartialByteArray() throws Exception {
        initExpectedBytes(1000);
        byte[] buf = new byte[120];
        while (System.currentTimeMillis() < stopTime) {
            random.nextBytes(buf);
            int n = random.nextInt(buf.length);
            out.write(buf, 0, n);
            for (int i = 0; i < n; i++)
                assertTrue("Bad byte", (byte) pin.read() == buf[i]);
            assertEquals("Too many bytes in stream", 0, pin.available());
            bytesSent += n;
        }
        assertLessThan("Sent " + bytesSent + " of " + expectedBytes, FUDGE_FACTOR
                * expectedBytes, Math.abs(bytesSent - expectedBytes));
    }

    public void testThrottledOutputStreamWriteFullByteArray() throws Exception {
        initExpectedBytes(1000);
        byte[] buf = new byte[120];
        while (System.currentTimeMillis() < stopTime) {
            random.nextBytes(buf);
            out.write(buf);
            for (int i = 0; i < buf.length; i++)
                assertTrue("Bad byte", (byte) pin.read() == buf[i]);
            assertEquals("Too many bytes in stream", 0, pin.available());
            bytesSent += buf.length;
        }
        assertLessThan("Sent " + bytesSent + " of " + expectedBytes, FUDGE_FACTOR
                * expectedBytes, Math.abs(bytesSent - expectedBytes));
    }

}
