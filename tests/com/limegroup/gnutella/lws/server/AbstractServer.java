package com.limegroup.gnutella.lws.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ManagedThread;
import org.limewire.lws.server.LWSDispatcher;
import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LWSServerUtil;
import org.limewire.lws.server.StringCallback;
import org.limewire.service.ErrorService;

/**
 * Base class for servers, both local and remote.
 */
public abstract class AbstractServer implements Runnable  {

    private static final Log LOG = LogFactory.getLog(AbstractServer.class);
    private static final byte[] NEWLINE = { (byte) '\r', (byte) '\n' };
    
    private final int port;
    private final String name;  
    private final AtomicBoolean hasShutDown = new AtomicBoolean(false);
    
    private boolean done = false;
    private LWSDispatcher dispatcher;
    private Thread runner;
    private ServerSocket serverSocket;

    // --------------------------------------------------------
    // Interface
    // --------------------------------------------------------

    public AbstractServer(int port, String name, LWSDispatcherSupport dispatcher) {
        this.port = port;
        this.name = name;
        this.dispatcher = dispatcher;
        LOG.debug(name + " on port " + port);
    }
    
    public AbstractServer(int port, String name) {
        this(port, name, null);        
    }
    
    public final Thread start() {
        Thread t = new ManagedThread(this);
        t.start();
        this.runner = t;
        return t;
    }

    /**
     * Main entry point.
     */
    public final void run() {
        noteRun();
        go();
    }
    
    public final LWSDispatcherSupport getDispatcher() {
        return (LWSDispatcherSupport)this.dispatcher;
    }

    public final void setDone(final boolean done) {
        this.done = done;
    }

    public final boolean isDone() {
        return this.done;
    }

    public final int getPort() {
        return port;
    }

    public final String getName() {
        return name;
    }
    
    public final String report(String error) {
        return LWSDispatcherSupport.report(error);
    }
    
    /**
     * Attempts to join this thread and then set Done to <code>true</code>.
     * 
     * @param millis milliseconds to wait for a join
     */
    public final void shutDown(final long millis) {
        if (hasShutDown.getAndSet(true)) return;
        setDone(true);
        stop(runner, millis);
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                handle(e, getClass() + " on port " + port);
            }
        }   
        runner = null;
    }    
    
    @Override
    public final String toString() {
        return name;
    }

    // --------------------------------------------------------
    // Interface to subclasses
    // --------------------------------------------------------
    
    protected abstract boolean sendIPToHandlers();

    protected final void handle(final Throwable t) {
        ErrorService.error(t);
    }

    protected final void handle(final Throwable t, final String msg) {
        ErrorService.error(t, msg);
    }
    
    /**
     * If subclasses can't pass a dispatcher in to the super constructor
     * because it needs to know about the server, you <b>must</b> call this
     * in the constructor.
     * 
     * @param dispatcher new {@link LWSDispatcherSupport}
     */
    protected final void setDispatcher(LWSDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Set by {@link #start(AbstractServer)}.
     */
    protected final Thread getRunner() {
        return runner;
    }

    /**
     * Override this to know when we're starting.
     */
    protected void noteRun() {
    }

    // --------------------------------------------------------
    // Private
    // --------------------------------------------------------

    private void go() {
        int threadCounter = 0;
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                shutDown();
            }
        }));       
        try {
            int tmpPort = port;
            for (; tmpPort < port+10; tmpPort++) {
                try {
                    serverSocket = new ServerSocket(tmpPort);
                } catch (IOException ignored) { }
                if (serverSocket != null) break;
            }
            while (!isDone()) {
                Socket s = serverSocket.accept();
                Worker ws = new Worker("lws worker #" + (threadCounter++));
                ws.setSocket(s, tmpPort);
                addNewThread(ws);
            }
        } catch (IOException e) {
            //
            // We are going to interupt this socket when we're
            // done, so don't complain about SocketExceptions
            //
            if (!(e instanceof SocketException)) handle(e, getClass() + " on port " + port);
            if (e instanceof java.net.BindException) setDone(true);
        } finally {
            shutDown();
        }
        shutDown();
    }

    private void addNewThread(Worker w) {
        Thread t = new ManagedThread(w, w.getName());
        t.start();
    }

    class Worker implements Runnable {

        final static int BUF_SIZE = 2048;

        /**
         * buffer to use for requests
         */
        private final byte[] buf = new byte[BUF_SIZE];

        /**
         * Socket to client we're handling
         */
        private Socket s;
        
        private final String name;
        Worker(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        synchronized void setSocket(Socket s, int port) {
            this.s = s;
            notify();
        }
        
        @Override
        public String toString() {
            return name;
        }

        public void run() {
            try {
                handleClient();
            } catch (IOException e) {
                LOG.debug("handleClient", e);
            }
        }
        
        @SuppressWarnings("deprecation")
        private void handleClient() throws IOException {
            InputStream is = new BufferedInputStream(s.getInputStream());
            final PrintStream ps = new PrintStream(s.getOutputStream(), true);
            /*
             * we will only block in read for this many milliseconds before we
             * fail with java.io.InterruptedIOException, at which point we will
             * abandon the connection.
             */
            /* zero out the buffer from last time */
            for (int i = 0; i < BUF_SIZE; i++) {
                buf[i] = 0;
            }
            try {
                /*
                 * We only support HTTP GET/HEAD, and don't support any fancy
                 * HTTP options, so we're only interested really in the first
                 * line.
                 */
                int nread = 0, r = 0;

                outerloop: while (nread < BUF_SIZE) {
                    r = is.read(buf, nread, BUF_SIZE - nread);
                    if (r == -1) {
                        /* EOF */
                        return;
                    }
                    int i = nread;
                    nread += r;
                    for (; i < nread; i++) {
                        if (buf[i] == (byte) '\n' || buf[i] == (byte) '\r') {
                            /* read one line */
                            break outerloop;
                        }
                    }
                }

                /* are we doing a GET or just a HEAD */
                /* beginning of file name */
                int index;
                if (buf[0] == (byte) 'G' && buf[1] == (byte) 'E'
                        && buf[2] == (byte) 'T' && buf[3] == (byte) ' ') {
                    index = 4;
                } else if (buf[0] == (byte) 'H' && buf[1] == (byte) 'E'
                        && buf[2] == (byte) 'A' && buf[3] == (byte) 'D'
                        && buf[4] == (byte) ' ') {
                    index = 5;
                } else {
                    /* we don't support this method */
                    ps.print("HTTP/1.1 " + HttpURLConnection.HTTP_BAD_METHOD + " unsupported method type: ");
                    ps.write(buf, 0, 5);
                    ps.write(NEWLINE);
                    ps.flush();
                    return;
                }

                int i = 0;
                /*
                 * find the file name, from: GET /foo/bar.html HTTP/1.0 extract
                 * "/foo/bar.html"
                 */
                for (i = index; i < nread; i++) {
                    if (buf[i] == (byte) ' ') {
                        break;
                    }
                }
                String request = (new String(buf, 0, index, i - index)).replace('/', File.separatorChar);
                if (request.startsWith(File.separator)) {
                    request = request.substring(1);
                }
                //
                // Only add the IP address if we're a remote server
                //
                if (sendIPToHandlers()) {
                    //
                    // We use Wicket-style arguments, because only remote servers want this
                    //
                    String ip = LWSServerUtil.getIPAddress(s.getInetAddress());
                    request += "/ip/" + ip;
                }
                getDispatcher().handle(request, ps, new StringCallback() {

                    public void process(String res) {
                        ps.print("HTTP/1.1 ");
                        ps.print(HttpURLConnection.HTTP_OK);
                        ps.print(" OK");
                        try {
                            ps.write(NEWLINE);
                            ps.print("Last-modified: ");
                            println(ps, LWSServerUtil.createCookieDate());
                            ps.print("Server: ");
                            ps.print(getName());
                            ps.write(NEWLINE);
                            ps.print("Date: " + new Date());
                            ps.write(NEWLINE);
                            ps.print("Content-length: ");
                            println(ps, String.valueOf(res.length()));
                            ps.write(NEWLINE);
                            ps.print(res);
                            ps.flush();
                        } catch (IOException e) {
                            ErrorService.error(e);
                        }
                    }
                    
                });

            } finally {
                s.close();
                is.close();
            }
        }

    }

    private void println(final PrintStream o, final String line)
            throws IOException {
        o.print(line);
        o.write(NEWLINE);
    }
    
    private void stop(final Thread t, final long millis) {
        if (t != null) {
            synchronized (t) {
                try {
                    t.join(millis);
                } catch (InterruptedException e) {
                    LOG.debug("trying to stop: " + t, e);
                }
            }
        }    
    }

    /**
     * Calls {@link #shutDown(long)} with <code>100</code>.
     */
    public final void shutDown() {
        shutDown(100);
    }

}
