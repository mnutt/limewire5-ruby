package com.limegroup.gnutella;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;

import junit.framework.TestCase;

import org.limewire.concurrent.ManagedThread;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.io.GUID;
import org.limewire.service.ErrorService;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.routing.RouteTableMessage;

/**
 * Utilities for BlockingConnection.
 * Because BlockingConnection is now a strictly test-only class,
 * many of these can be moved to instance methods in BlockingConnection itself.
 */
public class BlockingConnectionUtils {

    public static void failIfAnyArrive(final BlockingConnection[] connections,
            final Class<? extends Message> type) throws Exception {
        Thread[] drainers = new ManagedThread[connections.length];
        for (int i = 0; i < connections.length; i++) {
            final int index = i;
            drainers[i] = ThreadExecutor.newManagedThread(new Runnable() {
                public void run() {
                    try {
                        Message m = getFirstInstanceOfMessageType(connections[index], type);
                        TestCase.assertNull(m);
                    } catch (BadPacketException bad) {
                        BaseTestCase.fail(bad);
                    }
                }
            });
            drainers[i].start();
        }
        for (int i = 0; i < drainers.length; i++)
            drainers[i].join();
    }

    public static QueryReply getFirstQueryReply(BlockingConnection c, int tout) {
        return BlockingConnectionUtils.getFirstMessageOfType(c, QueryReply.class, tout);
    }

    public static QueryReply getFirstQueryReply(BlockingConnection c) {
        return getFirstQueryReply(c, BlockingConnectionUtils.TIMEOUT);
    }

    public static QueryRequest getFirstQueryRequest(BlockingConnection c, int tout) {
        return BlockingConnectionUtils.getFirstMessageOfType(c, QueryRequest.class, tout);
    }

    public static QueryRequest getFirstQueryRequest(BlockingConnection c) {
        return getFirstQueryRequest(c, BlockingConnectionUtils.TIMEOUT);
    }

    /**
     * @return the first message of type <pre>type</pre>.  Read messages within
     * the time out, so it's possible to wait upto almost 2 * timeout for this
     * method to return
     */
    public static <T extends Message> T getFirstInstanceOfMessage(Socket socket, Class<T> type,
            int timeout, MessageFactory messageFactory) throws IOException, BadPacketException {
        int oldTimeout = socket.getSoTimeout();
        try {
            for (int i = 0; i < 200; i++) {
                if (socket.isClosed())
                    return null;
                try {
                    socket.setSoTimeout(timeout);
                    Message m = messageFactory.read(socket.getInputStream(), Network.TCP);
                    if (type.isInstance(m))
                        return type.cast(m);
                    else if (m == null) //interruptedIOException thrown
                        return null;
                    i = 0;
                } catch (InterruptedIOException iiox) {
                    return null;
                }
            }
        } finally { //before we return reset the so-timeout
            socket.setSoTimeout(oldTimeout);
        }
        return null;
    }

    public static <T extends Message> T getFirstInstanceOfMessageType(BlockingConnection c,
            Class<T> type, int timeout) throws BadPacketException {
        for (int i = 0; i < 200; i++) {
            if (!c.isOpen()) {
                //System.out.println(c + " is not open");
                return null;
            }

            try {
                Message m = c.receive(timeout);
                //System.out.println("m: " + m + ", class: " + m.getClass());
                if (type.isInstance(m))
                    return type.cast(m);
                i = 0;
            } catch (InterruptedIOException ie) {
                //                ie.printStackTrace();
                return null;
            } catch (IOException iox) {
                //ignore iox
            }
        }
        throw new RuntimeException("No IIOE or Message after 100 iterations");
    }

    public static <T extends Message> T getFirstInstanceOfMessageType(BlockingConnection c,
            Class<T> type) throws BadPacketException {
        return getFirstInstanceOfMessageType(c, type, BlockingConnectionUtils.TIMEOUT);
    }

    public static <T extends Message> T getFirstMessageOfType(BlockingConnection c, Class<T> type,
            int timeout) {
        for (int i = 0; i < 100; i++) {
            if (!c.isOpen()) {
                //System.out.println(c + " is not open");
                return null;
            }

            try {
                Message m = c.receive(timeout);
                //System.out.println("m: " + m + ", class: " + m.getClass());
                if (m instanceof RouteTableMessage)
                    ;
                else if (m instanceof PingRequest)
                    ;
                else if (type.isInstance(m))
                    return type.cast(m);
                else
                    return null; // this is usually an error....
                i = 0;
            } catch (InterruptedIOException ie) {
                //ie.printStackTrace();
                return null;
            } catch (BadPacketException e) {
                // e.printStackTrace();
                // ignore...
            } catch (IOException ioe) {
                //ioe.printStackTrace();
                // ignore....
            }
        }
        throw new RuntimeException("No IIOE or Message after 100 iterations");
    }

    /**
     * Returns the first message of the expected type, ignoring
     * RouteTableMessages and PingRequests.
     */
    public static <T extends Message> T getFirstMessageOfType(BlockingConnection c, Class<T> type) {
        return getFirstMessageOfType(c, type, BlockingConnectionUtils.TIMEOUT);
    }

    public static boolean noUnexpectedMessages(BlockingConnection c, int timeout) {
        for (int i = 0; i < 100; i++) {
            if (!c.isOpen())
                return true;
            try {
                Message m = c.receive(timeout);
                if (m instanceof RouteTableMessage)
                    ;
                else if (m instanceof PingRequest)
                    ;
                else
                    // we should never get any other sort of message...
                    return false;
                i = 0;
            } catch (InterruptedIOException ie) {
                return true;
            } catch (BadPacketException e) {
                // ignore....
            } catch (IOException ioe) {
                // ignore....
            }
        }
        throw new RuntimeException("No IIOE or Message after 100 iterations");
    }

    /**
     * Returns true if no messages beside expected ones (such as QRP, Pings)
     * were received.
     */
    public static boolean noUnexpectedMessages(BlockingConnection c) {
        return noUnexpectedMessages(c, BlockingConnectionUtils.TIMEOUT);
    }

    public static void drainAll(BlockingConnection[] cs, int tout) throws IOException {
        for (int i = 0; i < cs.length; i++) {
            if (cs[i].isOpen())
                BlockingConnectionUtils.drain(cs[i], tout);
        }
    }

    /**
     * drains all messages from the given connections simultaneously.
     */
    public static void drainAllParallel(final BlockingConnection[] conns) {
        Thread[] r = new Thread[conns.length];
        for (int i = 0; i < conns.length; i++) {
            final int index = i;
            r[i] = ThreadExecutor.newManagedThread(new Runnable() {
                public void run() {
                    try {
                        BlockingConnectionUtils
                                .drain(conns[index], BlockingConnectionUtils.TIMEOUT);
                    } catch (Exception bad) {
                        ErrorService.error(bad);
                    }
                }
            });
            r[i].start();
        }

        for (int i = 0; i < r.length; i++) {
            try {
                r[i].join();
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Tries to drain all messages from the array of connections.
     */
    public static void drainAll(BlockingConnection[] conns) throws Exception {
        drainAll(conns, BlockingConnectionUtils.TIMEOUT);
    }

    public static boolean drain(BlockingConnection c, int timeout) throws IOException {
        if (!c.isOpen())
            return false;

        boolean ret = false;
        for (int i = 0; i < 100; i++) {
            try {
                c.receive(timeout);
                ret = true;
                i = 0;
            } catch (InterruptedIOException e) {
                // we read a null message or received another 
                // InterruptedIOException, which means a messages was not 
                // received
                return ret;
            } catch (BadPacketException e) {
                // ignore...
            }
        }
        return ret;
    }

    /** 
     * Tries to receive any outstanding messages on c 
     *
     * @return <tt>true</tt> if this got a message, otherwise <tt>false</tt>
     */
    public static boolean drain(BlockingConnection c) throws IOException {
        return drain(c, BlockingConnectionUtils.TIMEOUT);
    }

    /**
     * Sends a pong through all connections to keep them alive.
     * @param pingReplyFactory 
     */
    public static void keepAllAlive(BlockingConnection[] cs, PingReplyFactory pingReplyFactory)
            throws IOException {
        for (int i = 0; i < cs.length; i++) {
            PingReply pr = pingReplyFactory.create(GUID.makeGuid(), (byte) 1);
            cs[i].send(pr);
            cs[i].flush();
        }
    }

    public static final int TIMEOUT = 2000;

}
