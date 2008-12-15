package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Properties;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.ByteReader;

import com.limegroup.gnutella.Constants;

/**
 * Contains some convenience methods for handshaking.
 */
class BlockingHandshakeSupport extends HandshakeSupport {
    
    /** Socket we're basing this I/O on. */
    private Socket socket;
    /** InputStream we're reading from */
    private InputStream in;
    /** OutputStream we're writing to. */
    private OutputStream out;
    
    /** Constructs a new Support object based on the given Socket, InputStream & OutputStream. */
    BlockingHandshakeSupport(Socket socket, InputStream in, OutputStream out) {
        super(socket.getInetAddress().getHostAddress());
        this.socket = socket;
        this.in = in;
        this.out = out;
    }
    
    /**
     * Reads and returns one line from the network.  A line is defined as a
     * maximal sequence of characters without '\n', with '\r''s removed.  If the
     * characters cannot be read within TIMEOUT milliseconds (as defined by the
     * property manager), throws IOException.  This includes EOF.
     * @return The line of characters read
     * @exception IOException if the characters cannot be read within 
     *            the specified timeout
     */
    String readLine() throws IOException {
        return readLine(Constants.TIMEOUT);
    }

    /**
     * Reads and returns one line from the network.  A line is defined as a
     * maximal sequence of characters without '\n', with '\r''s removed.  If the
     * characters cannot be read within the specified timeout milliseconds,
     * throws IOException.  This includes EOF.
     * @param timeout The time to wait on the socket to read data before 
     *                  IOException is thrown
     * @return The line of characters read
     * @exception IOException if the characters cannot be read within 
     *          the specified timeout
     */
    String readLine(int timeout) throws IOException {
        int oldTimeout=socket.getSoTimeout();
        // _in.read can throw an NPE if we closed the connection,
        // so we must catch NPE and throw the CONNECTION_CLOSED.
        try {
            socket.setSoTimeout(timeout);
            // TODO: don't read over max line size.
            String line=(new ByteReader(in)).readLine();
            if (line==null)
                throw new IOException("read null line");
            return line;
        } catch(NullPointerException npe) {
            throw new IOException();
        } finally {
            //Restore socket timeout.
            socket.setSoTimeout(oldTimeout);
        }
    }

    /**
     * Reads all headers.
     */
    void readHeaders() throws IOException {
        readHeaders(Constants.TIMEOUT);
    }
    
    /**
     * Reads the properties from the network and stores them locally.
     * 
     * @param timeout The time to wait on the socket to read data before 
     *                IOException is thrown
     * @exception IOException if the characters cannot be read within 
     * the specified timeout
     */
    void readHeaders(int timeout) throws IOException {
        while (true) {
            // This doesn't distinguish between \r and \n. That's fine.
            String line = readLine(timeout);
            if (line == null)
                throw new IOException("unexpected end of file"); // unexpected EOF
            if(!processReadHeader(line))
                break;
            if(getHeadersReadSize() > ConnectionSettings.MAX_HANDSHAKE_HEADERS.getValue())
                throw new IOException("too many headers");
        }
    }
    
    /** Writes the initial connection line. */
    void writeConnectLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        appendConnectLine(sb);
        writeLine(sb.toString());
    }
    
    /** Writes a response using the given HandshakeResponse. */
    void writeResponse(HandshakeResponse response) throws IOException {       
        StringBuilder sb = new StringBuilder();
        appendResponse(response, sb);
        writeLine(sb.toString());
    }

    /**
     * Writes s to out, with no trailing linefeeds. Called only from initialize().
     */
    void writeLine(String s) throws IOException {
        if (s == null || s.equals("")) {
            throw new NullPointerException("null or empty string: " + s);
        }

        // TODO: character encodings?
        byte[] bytes = s.getBytes();
        out.write(bytes);
        out.flush();
    }
    
    /**
     * Writes the properties in props to network, including the blank line at the end.
     * Throws IOException if there are any problems.
     * 
     * @param props The headers to be sent. Note: null argument is acceptable,
     *  if no headers need to be sent the trailer will be sent.
     */
    void sendHeaders(Properties props) throws IOException {
        StringBuilder sb = new StringBuilder();
        appendHeaders(props, sb);
        writeLine(sb.toString());
    }
    
}
