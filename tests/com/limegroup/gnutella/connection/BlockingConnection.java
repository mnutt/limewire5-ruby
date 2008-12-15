package com.limegroup.gnutella.connection;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.CompressingOutputStream;
import org.limewire.io.IOUtils;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.io.UncompressingInputStream;
import org.limewire.net.SocketsManager;
import org.limewire.net.SocketsManager.ConnectType;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.handshaking.BadHandshakeException;
import com.limegroup.gnutella.handshaking.BlockingIncomingHandshaker;
import com.limegroup.gnutella.handshaking.BlockingOutgoingHandshaker;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.Handshaker;
import com.limegroup.gnutella.handshaking.NoGnutellaOkException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;

/**
 * A connection for use with tests that blocks for reading/writing.
 */
public class BlockingConnection extends AbstractConnection {

    private static final Log LOG = LogFactory.getLog(BlockingConnection.class);

    private volatile InputStream _in;

    private volatile OutputStream _out;

    /**
     * The Inflater to use for inflating read streams, initialized in
     * initialize() if the connection told us it's sending with a
     * Content-Encoding of deflate. Definitions: Inflater.getTotalOut -- The
     * number of UNCOMPRESSED bytes Inflater.getTotalIn -- The number of
     * COMPRESSED bytes
     */
    private volatile Inflater _inflater;

    /**
     * The Deflater to use for deflating written streams, initialized in
     * initialize() if we told the connection we're sending with a
     * Content-Encoding of deflate. Note that this is the same as '_out', but is
     * assigned here as the appropriate type so we don't have to cast when we
     * want to measure the compression savings. Definitions:
     * Deflater.getTotalOut -- The number of COMPRESSED bytes
     * Deflater.getTotalIn -- The number of UNCOMPRESSED bytes
     */
    private volatile Deflater _deflater;

    private final SocketsManager socketsManager;

    private final MessageFactory messageFactory;

    /**
     * Creates an uninitialized outgoing Gnutella 0.6 connection with the
     * desired outgoing properties that may use TLS.
     * 
     * @param host the name of the host to connect to
     * @param port the port of the remote host
     * @param connectType the type of connection that should be made
     * @throws <tt>NullPointerException</tt> if any of the arguments are
     *         <tt>null</tt>
     * @throws <tt>IllegalArgumentException</tt> if the port is invalid
     */
    BlockingConnection(String host, int port, ConnectType connectType,
            CapabilitiesVMFactory capabilitiesVMFactory, SocketsManager socketsManager,
            Acceptor acceptor, MessagesSupportedVendorMessage supportedVendorMessage,
            MessageFactory messageFactory, NetworkManager networkManager,
            NetworkInstanceUtils networkInstanceUtils) {
        super(host, port, connectType, capabilitiesVMFactory, supportedVendorMessage,
                networkManager, acceptor, networkInstanceUtils);
        this.messageFactory = messageFactory;
        this.socketsManager = socketsManager;
    }

    /**
     * Creates an uninitialized incoming 0.6 Gnutella connection. If the client
     * is attempting to connect using an 0.4 handshake, it is rejected.
     * 
     * @param socket the socket accepted by a ServerSocket. The word "GNUTELLA "
     *        and nothing else must have been read from the socket.
     * @param responder the headers to be sent in response to the client's
     *        "GNUTELLA CONNECT".
     * @throws <tt>NullPointerException</tt> if any of the arguments are
     *         <tt>null</tt>
     */
    BlockingConnection(Socket socket, CapabilitiesVMFactory capabilitiesVMFactory,
            Acceptor acceptor, MessagesSupportedVendorMessage supportedVendorMessage,
            MessageFactory messageFactory, NetworkManager networkManager,
            NetworkInstanceUtils networkInstanceUtils) {
        super(socket, capabilitiesVMFactory, supportedVendorMessage, networkManager, acceptor,
                networkInstanceUtils);
        this.socketsManager = null;
        this.messageFactory = messageFactory;
    }

    /**
     * Initialize the connection by doing the handshake. Throws IOException if
     * we were unable to establish a normal messaging connection for any reason.
     * Do not call send or receive if this happens.
     * 
     * @param timeout for outgoing connections, the timeout in milliseconds to
     *        use in establishing the socket, or 0 for no timeout. If the
     *        platform does not support native timeouts, it will be emulated
     *        with threads.
     * @exception IOException we were unable to connect to the host
     * @exception NoGnutellaOkException one of the participants responded with
     *            an error code other than 200 OK (possibly after several rounds
     *            of 401's)
     * @exception BadHandshakeException some other problem establishing the
     *            connection, e.g., the server responded with HTTP, closed the
     *            the connection during handshaking, etc.
     */
    public void initialize(Properties requestHeaders, HandshakeResponder responder, int timeout) throws IOException, NoGnutellaOkException,
            BadHandshakeException {
        if (isOutgoing()) {
            setSocket(socketsManager.connect(new InetSocketAddress(getAddress(), getPort()), timeout, getConnectType()));
        }
        
        initializeHandshake();
        
        Handshaker shaker = createHandshaker(requestHeaders, responder);
        try {
            shaker.shake();
        } catch (NoGnutellaOkException e) {
            setHeaders(shaker.getReadHeaders(), shaker.getWrittenHeaders());
            close();
            throw e;
        } catch (IOException e) {
            setHeaders(shaker.getReadHeaders(), shaker.getWrittenHeaders());
            close();
            throw new BadHandshakeException(e);
        }

        handshakeInitialized(shaker);

        // wrap the streams with inflater/deflater
        // These calls must be delayed until absolutely necessary (here)
        // because the native construction for Deflater & Inflater
        // allocate buffers outside of Java's memory heap, preventing
        // Java from fully knowing when/how to GC. The call to end()
        // (done explicitly in the close() method of this class, and
        // implicitly in the finalization of the Deflater & Inflater)
        // releases these buffers.
        if (isWriteDeflated()) {
            _deflater = new Deflater();
            _out = new CompressingOutputStream(_out, _deflater);
        }

        if (isReadDeflated()) {
            _inflater = new Inflater();
            _in = new UncompressingInputStream(_in, _inflater);
        }
        
        getConnectionBandwidthStatistics().setCompressionOption(isWriteDeflated(), isReadDeflated(), new CompressionBandwidthTrackerImpl(_inflater, _deflater));
    }

    /** Constructs the Handshaker object. */
    protected Handshaker createHandshaker(Properties requestHeaders, HandshakeResponder responder)
            throws IOException {
        try {
            _in = new BufferedInputStream(getSocket().getInputStream());
            _out = new BufferedOutputStream(getSocket().getOutputStream());
            if (_in == null)
                throw new IOException("null input stream");
            else if (_out == null)
                throw new IOException("null output stream");
        } catch (Exception e) {
            // Apparently Socket.getInput/OutputStream throws
            // NullPointerException if the socket is closed. (See Sun bug
            // 4091706.) Unfortunately the socket may have been closed after the
            // the check above, e.g., if the user pressed disconnect. So we
            // catch NullPointerException here--and any other weird possible
            // exceptions. An alternative is to obtain a lock before doing these
            // calls, but we are afraid that getInput/OutputStream may be a
            // blocking operation. Just to be safe, we also check that in/out
            // are not null.
            close();
            throw new IOException("could not establish connection");
        }

        if (isOutgoing())
            return new BlockingOutgoingHandshaker(requestHeaders, responder, getSocket(), _in, _out);
        else
            return new BlockingIncomingHandshaker(responder, getSocket(), _in, _out);
    }

    // ///////////////////////////////////////////////////////////////////////

    /** A tiny allocation optimization; see Message.read(InputStream,byte[]). */
    private final byte[] HEADER_BUF = new byte[23];

    /**
     * Receives a message. This method is NOT thread-safe. Behavior is undefined
     * if two threads are in a receive call at the same time for a given
     * connection.
     * 
     * @requires this is fully initialized
     * @effects exactly like Message.read(), but blocks until a message is
     *          available. A half-completed message results in
     *          InterruptedIOException.
     */
    public Message receive() throws IOException, BadPacketException {
        // On the Macintosh, sockets *appear* to return the same ping reply
        // repeatedly if the connection has been closed remotely. This prevents
        // connections from dying. The following works around the problem. Note
        // that Message.read may still throw IOException below.
        // See note on _closed for more information.
        if (!isOpen())
            throw CONNECTION_CLOSED;

        Message m = null;
        while (m == null) {
            m = readAndUpdateStatistics();
        }
        return m;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#receive(int)
     */
    public Message receive(int timeout) throws IOException, BadPacketException,
            InterruptedIOException {
        // See note in receive().
        if (!isOpen())
            throw CONNECTION_CLOSED;

        // temporarily change socket timeout.
        int oldTimeout = getSocket().getSoTimeout();
        getSocket().setSoTimeout(timeout);
        try {
            Message m = readAndUpdateStatistics();
            if (m == null) {
                throw new InterruptedIOException("null message read");
            }
            return m;
        } finally {
            getSocket().setSoTimeout(oldTimeout);
        }
    }

    /**
     * Reads a message from the network and updates the appropriate statistics.
     */
    private Message readAndUpdateStatistics() throws IOException, BadPacketException {
        Message msg = messageFactory.read(_in, Network.TCP, HEADER_BUF, getSoftMax(), null);
        if (msg != null)
            processReadMessage(msg);
        return msg;
    }

    /**
     * Optimization -- reuse the header buffer since sending will only be done
     * on one thread.
     */
    private final byte[] OUT_HEADER_BUF = new byte[23];

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#send(com.limegroup.gnutella.messages.Message)
     */
    public void send(Message m) throws IOException {
        if (LOG.isTraceEnabled())
            LOG.trace("Connection (" + toString() + ") is sending message: " + m);

        // The try/catch block is necessary for two reasons...
        // See the notes in Connection.close above the calls
        // to end() on the Inflater/Deflater and close()
        // on the Input/OutputStreams for the details.
        try {
            m.write(_out, OUT_HEADER_BUF);
            processWrittenMessage(m);
        } catch (NullPointerException e) {
            throw CONNECTION_CLOSED;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#flush()
     */
    public void flush() throws IOException {
        // The try/catch block is necessary for two reasons...
        // See the notes in Connection.close above the calls
        // to end() on the Inflater/Deflater and close()
        // on the Input/OutputStreams for the details.
        try {
            _out.flush();
        } catch (NullPointerException npe) {
            throw CONNECTION_CLOSED;
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.limegroup.gnutella.Connection#close()
     */
    @Override
    protected void closeImpl() {
        IOUtils.close(_deflater);
        IOUtils.close(_inflater);
        IOUtils.close(_in);
        IOUtils.close(_out);
    }
}
