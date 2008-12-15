package com.limegroup.gnutella.chat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.IOUtils;
import org.limewire.net.SocketsManager;
import org.limewire.nio.channel.AbstractBufferChannelWriter;
import org.limewire.nio.channel.AbstractChannelInterestReader;
import org.limewire.nio.channel.NIOMultiplexor;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.nio.statemachine.IOState;
import org.limewire.nio.statemachine.IOStateMachine;
import org.limewire.nio.statemachine.IOStateObserver;
import org.limewire.util.BufferUtils;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.SimpleReadHeaderState;
import com.limegroup.gnutella.http.SimpleWriteHeaderState;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * This class implements a simple chat protocol that allows to exchange text
 * messages over a socket connection.
 * 
 * <p>
 * The protocol is similar to a Gnutella handshake:
 * 
 * <pre>
 *       -&gt; CHAT CONNECT/0.1
 *       -&gt; User-Agent: LimeWire @version@
 *       -&gt;
 *       &lt;- CHAT/0.1 200 OK
 *       &lt;-
 *       -&gt; CHAT/0.1 200 OK
 *       -&gt;
 *       -&gt; [message]\r\n
 *       &lt;- [message]\r\n
 *       ...
 * </pre>
 */
public class InstantMessengerImpl implements InstantMessenger {

    private static final Log LOG = LogFactory.getLog(InstantMessengerImpl.class);

    private static final String CHAT_CONNECT = "CHAT CONNECT/0.1";

    private static final String CHAT_OK = "CHAT/0.1 200 OK";

    private static final String CONNECT = "CONNECT/0.1";

    private static final String CHARSET = "UTF-8";

    private final String host;

    private final int port;

    private final boolean outgoing;

    private final ActivityCallback callback;

    private final Object connectionLock = new Object();

    /**
     * The socket used to send and receive data. Can be <code>null</code> if
     * no connection has been established, yet.
     * <p>
     * Note: Acquire {@link #connectionLock} to access.
     */
    private Socket socket;

    /**
     * Note: Acquire {@link #connectionLock} to access.
     */
    private MessageReceiver receiver;

    /**
     * Note: Acquire {@link #connectionLock} to access.
     */
    private MessageSender sender;

    /**
     * If true, the user has closed the chat.
     * <p>
     * Note: Acquire {@link #connectionLock} to access.
     */
    private boolean stopped;

    private final SocketsManager socketsManager;

    /**
     * Constructor for an incoming chat request.
     */
    InstantMessengerImpl(Socket socket, ActivityCallback callback) {
        if (socket == null || callback == null) {
            throw new IllegalArgumentException();
        }

        this.socket = socket;
        this.port = socket.getPort();
        this.host = socket.getInetAddress().getHostAddress();
        this.callback = callback;
        this.socketsManager = null;
        this.outgoing = false;
    }

    /**
     * Constructor for an outgoing chat request
     */
    InstantMessengerImpl(final String host, final int port,
            ActivityCallback callback, SocketsManager socketsManager) {
        if (host == null || callback == null) {
            throw new IllegalArgumentException();
        }

        this.host = host;
        this.port = port;
        this.callback = callback;
        this.socketsManager = socketsManager;
        this.outgoing = true;
    }

    public void start() {
        if (outgoing) {
            try {
                socketsManager.connect(new InetSocketAddress(host, port), Constants.TIMEOUT,
                        new ConnectObserver() {
                            public void handleConnect(Socket socket)
                                    throws IOException {
                                synchronized (connectionLock) {
                                    InstantMessengerImpl.this.socket = socket;
                                    shake(createOutgoingShakeStates());
                                }
                            }

                            public void handleIOException(IOException e) {
                                LOG.error("Unexpected exception", e);
                                handleException(e);
                            }

                            public void shutdown() {
                                LOG.warn("Could not establish chat connection to "
                                                + host + ":" + port);
                                stop();
                            }

                        });
            } catch (IOException e) {
                // should never happen since we are connecting in the background
                LOG.warn("Unexpected exception", e);
                handleException(e);
            }
        } else {
            shake(createIncomingShakeStates());
        }
    }

    /**
     * send a message accross the socket to the other host as with stop, this is
     * alway safe to call, but it is recommended that the gui discourage the
     * user from calling it when a connection is not yet established.
     */
    public boolean send(String message) {
        synchronized (connectionLock) {
            if (stopped || sender == null) {
                return false;
            }
        }

        try {
            sender.sendMessage(message + "\n");
        } catch (BufferOverflowException e) {
            return false;
        } catch (IOException e) {
            stop();
            return false;
        }
        return true;
    }

    /**
     * Returns the host name to which the socket is connected.
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port to which the socket is connected.
     */
    public int getPort() {
        return port;
    }

    private List<IOState> createIncomingShakeStates() {
        List<IOState> states = new ArrayList<IOState>(3);
        List<Header> emptyHeaders = Collections.emptyList();
        states.add(new ReadChatConnectHeaderState());
        states.add(new SimpleWriteHeaderState(CHAT_OK, emptyHeaders, null));
        states.add(new ReadChatHeaderState());
        return states;
    }

    private List<IOState> createOutgoingShakeStates() {
        List<Header> headers = new ArrayList<Header>();
        headers.add(HTTPHeaderName.USER_AGENT.create(LimeWireUtils.getVendor()));
        List<Header> emptyHeaders = Collections.emptyList();

        List<IOState> states = new ArrayList<IOState>(3);
        states.add(new SimpleWriteHeaderState(CHAT_CONNECT, headers, null));
        states.add(new ReadChatHeaderState());
        states.add(new SimpleWriteHeaderState(CHAT_OK, emptyHeaders, null));
        return states;
    }

    /**
     * Invoked when the handshake completes.
     */
    private void handshakeCompleted() {
        synchronized (connectionLock) {
            try {
                socket.setSoTimeout(0);
            } catch (SocketException e) {
                LOG.warn("Could not set socket timeout", e);
            }

            receiver = new MessageReceiver();
            sender = new MessageSender();

            ((NIOMultiplexor) socket).setReadObserver(receiver);
            ((NIOMultiplexor) socket).setWriteObserver(sender);
        }

        callback.acceptChat(this);
        
        synchronized (connectionLock) {
            connectionLock.notifyAll();
        }
    }

    private void handleException(IOException e) {
        callback.chatErrorMessage(this, e.getMessage());
        stop();
    }

    private void shake(List<IOState> states) {
        IOStateMachine shaker = new IOStateMachine(new IOStateObserver() {
            public void handleStatesFinished() {
                handshakeCompleted();
            }

            public void handleIOException(IOException e) {
                handleException(e);
            }

            public void shutdown() {
                stop();
            }
        }, states);

        synchronized (connectionLock) {
            if (stopped) {
                return;
            }

            try {
                socket.setSoTimeout(Constants.TIMEOUT);
            } catch (SocketException e) {
                LOG.warn("Could not set socket timeout", e);
            }
            ((NIOMultiplexor) socket).setReadObserver(shaker);
            ((NIOMultiplexor) socket).setWriteObserver(shaker);
        }
    }

    /* For testing. */
    public void waitForConnect(long timeout)
            throws InterruptedException {
        synchronized (connectionLock) {
            if (!stopped && (socket == null || !socket.isConnected())) {
                connectionLock.wait(timeout);
            }
        }
    }

    private class MessageReceiver extends AbstractChannelInterestReader {

        public MessageReceiver() {
            super(1024);
        }

        @Override
        public void handleIOException(IOException e) {
            handleException(e);
        }

        public void handleRead() throws IOException {
            int read = 0;
            while (buffer.hasRemaining() && (read = source.read(buffer)) > 0)
                ;

            flushBuffer();

            if (read == -1) {
                stop();
            }
        }

        private void flushBuffer() {
            buffer.flip();
            StringBuilder sb = new StringBuilder();
            while (BufferUtils.readLine(buffer, sb)) {
                callback.receiveMessage(InstantMessengerImpl.this, sb.toString());
            }

            if (buffer.hasRemaining()) {
                buffer.compact();
            } else {
                buffer.clear();
            }
        }

        @Override
        public void shutdown() {
            super.shutdown();

            stop();
        }
    }

    private class MessageSender extends AbstractBufferChannelWriter {

        public MessageSender() {
            super(1024);
        }

        @Override
        public void handleIOException(IOException e) {
            InstantMessengerImpl.this.handleException(e);
        }

        public void sendMessage(String message) throws IOException {
            put(message.getBytes(CHARSET));
        }

        @Override
        public void shutdown() {
            super.shutdown();

            stop();
        }

    }

    private class ReadChatConnectHeaderState extends SimpleReadHeaderState {

        public ReadChatConnectHeaderState() {
            super(null, ConnectionSettings.MAX_HANDSHAKE_HEADERS.getValue(),
                    ConnectionSettings.MAX_HANDSHAKE_LINE_SIZE.getValue());
        }

        @Override
        protected void processConnectLine() throws IOException {
            if (!CONNECT.equals(connectLine)) {
                throw new IOException("Invalid handshake: " + connectLine);
            }
        }
    }

    private class ReadChatHeaderState extends SimpleReadHeaderState {

        public ReadChatHeaderState() {
            super(null, ConnectionSettings.MAX_HANDSHAKE_HEADERS.getValue(),
                    ConnectionSettings.MAX_HANDSHAKE_LINE_SIZE.getValue());
        }

        @Override
        protected void processConnectLine() throws IOException {
            if (!CHAT_OK.equals(connectLine)) {
                throw new IOException("Invalid handshake: " + connectLine);
            }
        }
    }

    /**
     * Stop the chat, and close the connections this is always safe to call.
     */
    public void stop() {
        synchronized (connectionLock) {
            if (stopped) {
                return;
            }
            stopped = true;

            if (socket != null && !socket.isClosed()) {
                IOUtils.close(socket);
            }
            
            connectionLock.notifyAll();
        }
        callback.chatUnavailable(this);
    }

    public boolean isOutgoing() {
        return outgoing;
    }

    public boolean isConnected() {
        synchronized (connectionLock) {
            return !stopped;
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "[host=" + host + ":" + port
                + ",outgoing=" + outgoing + "]";
    }

}
