package com.limegroup.gnutella.guess;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.Test;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.Message.Network;

/** Provides primitives for contacting GUESS nodes on the network.
 *  THIS TEST SHOULD NOT BE INCLUDED IN ALL TESTS!  It is very specifically 
 *  tuned and will now work in general.
 */
public class GUESSTester extends com.limegroup.gnutella.util.LimeTestCase {

	/**
	 * Constant for the size of UDP messages to accept -- dependent upon
	 * IP-layer fragmentation.
	 */
	private final int BUFFER_SIZE = 8192;

    private final int WAIT_TIME   = 500; // 1/2 a second...

    private DatagramSocket _socket = null;

    private QueryReply _qr = null;
    private final Object _qrLock = new Object();

    private PingReply _pong = null;
    private final Object _pongLock = new Object();

    private QueryRequestFactory queryRequestFactory;

    private PingRequestFactory pingRequestFactory;

    private MessageFactory messageFactory;

	/**
	 * Constructs a new <tt>GUESSTester</tt> instance.
	 */
	public GUESSTester(String name) {
		super(name);
        initUDPPort();
	}


    private void initUDPPort() {
        try {
            _socket = new DatagramSocket();
            _socket.setSoTimeout(1000);
            Thread listener = new Thread() {
                    @Override
                    public void run() {
                        listenLoop();
                    }
                };
            listener.start();
        }
        catch (Exception ignored) {
            System.out.println("Could not get a UDP Socket!!");
        }
    }

	@Override
	public void setUp() throws Exception {
		Injector injector = LimeTestUtils.createInjector();
		queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
		pingRequestFactory = injector.getInstance(PingRequestFactory.class);
		messageFactory = injector.getInstance(MessageFactory.class);
	}

    private void listenLoop() {
		byte[] datagramBytes = new byte[BUFFER_SIZE];
		DatagramPacket datagram = new DatagramPacket(datagramBytes, 
                                                     BUFFER_SIZE);
        while (true) {
            try {				
                _socket.receive(datagram);
                byte[] data = datagram.getData();
                int length = datagram.getLength();
                try {
                    // construct a message out of it...
                    InputStream in = new ByteArrayInputStream(data, 0, length);
                    Message message = messageFactory.read(in, Network.TCP);		
                    if (message == null) continue;
                    if (message instanceof QueryReply) {
                        synchronized (_qrLock) {
                            _qr = (QueryReply) message;
                            _qrLock.notify();
                        }
                    }
                    else if (message instanceof PingReply) {
                        synchronized (_pongLock) {
                            _pong = (PingReply) message;
                            _pongLock.notify();
                        }
                    }
                } 
                catch(BadPacketException e) {
                    continue;
                }
            } 
            catch (InterruptedIOException e) {
                continue;
            } 
            catch (IOException e) {
                continue;
            }
		}
    }


	/**
	 * Run this suite of tests.
	 */
	public static Test suite() {
		return buildTestSuite(GUESSTester.class);
	}

    public void testSmall() {
        try {
            assertTrue(testAck("10.254.0.19", 6346) > 0);
            assertNotNull(testQuery("10.254.0.19", 6346,
									queryRequestFactory.createQuery("morrissey", (byte)1)));
        }
        catch (Exception whatever) {
            assertTrue(false);
        }
    }

    /** This method blocks for possibly several seconds.
     *  @return a non-negative value if the ack was recieved.  else 0...
     */
    public synchronized long testAck(String host, int port) 
        throws UnknownHostException, IOException {
        synchronized (_pongLock) {
            _pong = null;
        }
		QueryRequest qr = queryRequestFactory.createQuery("susheel", (byte)1);
        InetAddress addr = InetAddress.getByName(host);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        qr.write(baos);
        DatagramPacket toSend = new DatagramPacket(baos.toByteArray(),
                                                   baos.toByteArray().length,
                                                   addr, port);
        long startTime = 0, endTime = 0;
        synchronized (_pongLock) {
            startTime = System.currentTimeMillis();
            _socket.send(toSend);
            try {
                // wait up to 2.5 seconds for an ack....
                _pongLock.wait(WAIT_TIME);
            }
            catch (InterruptedException ignored) {}
            endTime = System.currentTimeMillis();
        }
        if (_pong == null)
            return 0;
        else return (endTime - startTime);
    }

    /** This method blocks for possibly several seconds.
     *  @return a non-negative value if the ack was recieved.  else 0...
     */
    public synchronized long testPing(String host, int port) 
        throws UnknownHostException, IOException {
        synchronized (_pongLock) {
            _pong = null;
        }
        PingRequest pr = pingRequestFactory.createPingRequest((byte)1);
        InetAddress addr = InetAddress.getByName(host);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        pr.write(baos);
        DatagramPacket toSend = new DatagramPacket(baos.toByteArray(),
                                                   baos.toByteArray().length,
                                                   addr, port);
        long startTime = 0, endTime = 0;
        synchronized (_pongLock) {
            startTime = System.currentTimeMillis();
            _socket.send(toSend);
            try {
                // wait up to WAIT_TIME seconds for an ack....
                _pongLock.wait(WAIT_TIME);
            }
            catch (InterruptedException ignored) {}
            endTime = System.currentTimeMillis();
        }
        if (_pong == null)
            return 0;
        else return (endTime - startTime);
    }

    /** This method blocks for possibly several seconds.
     *  @return A QueryReply to your input Query.  May be null.
     */ 
    public synchronized QueryReply testQuery(String host, int port, 
                                             QueryRequest qr) 
        throws UnknownHostException, IOException {
        synchronized (_qrLock) {
            _qr = null;
        }
        synchronized (_pongLock) {
            _pong = null;
        }
        InetAddress addr = InetAddress.getByName(host);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        qr.write(baos);
        DatagramPacket toSend = new DatagramPacket(baos.toByteArray(),
                                                   baos.toByteArray().length,
                                                   addr, port);
        synchronized (_qrLock) {
            _socket.send(toSend);
            try {
                // wait up to 2.5 seconds for an ack....
                _qrLock.wait(WAIT_TIME);
            }
            catch (InterruptedException ignored) {}
        }
        return _qr;
    }


    public static final boolean debugOn = true;
    public static final void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }


}
