package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.service.ErrorCallback;
import org.limewire.service.ErrorService;
import org.limewire.util.TestUtils;

import com.google.inject.Guice;
import com.google.inject.Stage;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

/**
 * Utility class that constructs a LimeWire backend for testing
 * purposes.  This creates a backend with a true <tt>FileManager</tt>,
 * creating a temporary shared directory that files are copied into.
 * The only component of this backend that is a stub is 
 * <tt>ActivityCallbackStub</tt>.  Otherwise, all classes are 
 * constructed as they normally would be in the client.
 */

/**
 * This isn't really a JUNIT test class, but subclassing BastTestCase gives us
 * access to a lot of neat support routines and does no harm. Side benefit, as
 * soon as the constructor is called the internal save and user preference
 * directories will be switched to harmless test directories, saving us from
 * having to save and restore important files.
 */
@SuppressWarnings("all")
public class Backend extends com.limegroup.gnutella.util.LimeTestCase {

    /** Extensions of files that the backend automatically shares */
    public static final String SHARED_EXTENSION = "tmp";

    /** Port that normal backend will listen on */
    public static final int BACKEND_PORT = 6300;

    /** Port that the reject backend will listen on */
    public static final int REJECT_PORT = 6301;

    /** Port used by the leaf */
    private static final int LEAF_PORT = 6302;

    /** Port that will shutdown the normal backend process */
    private static final int SHUTDOWN_PORT = 6310;

    /** Port that will shutdown the reject backend process */
    private static final int SHUTDOWN_REJECT_PORT = 6311;

    /** Port that will shutdown the leaf backend process */
    private static final int SHUTDOWN_LEAF_PORT = 6312;

    /** Port used to pass error reports to reporting JVM */
    private static final int ERROR_PORT = 6399;

    /**
     * The <tt>RouterService</tt> instance the constructs the backend.
     */
    // private RouterService ROUTER_SERVICE;
    /**
     * The thread which is catching the error reports from the various backend
     * servers.
     */
    private static ErrorMonitor errorMonitor = null;

    /**
     * Return the linstening port number
     */
    public static int getPort() {
        return BACKEND_PORT;
    }

    /**
     * erturn the reject server listening port number
     */
    public static int getRejectPort() {
        return REJECT_PORT;
    }

    /**
     * Launches a standard backend on port 6300.
     */
    public synchronized static boolean launch() throws IOException {
        return launch(BACKEND_PORT);
    }

    /**
     * Launch normal backend process if it isn't already running
     * 
     * @return true if we have launched a new backend process false if one was
     *         already running
     * @throws IOException if backend launch was unsucessful
     */
    public synchronized static boolean launchReject() throws IOException {
        return launch(REJECT_PORT);
    }

    /**
     * Creates a new leaf that connects to the "primary" backend, running on
     * 6300.
     */
    public synchronized static boolean launchLeaf() throws IOException {
        return launch(LEAF_PORT);
    }

    /**
     * Launch backend process if it isn't already running
     * 
     * @param reject true to launch the reject backend server, false to launch
     *        the regular backend server
     * @return true if we have launched a new backend process false if one was
     *         already running
     * @throws IOException if backend launch was unsucessful
     */
    public synchronized static boolean launch(int port) throws IOException {

        // Try to open a connection to our listening port. If it works, we
        // will assume the backend is up and running
        if (isPortInUse(port))
            return false;

        String[] args = new String[5];
        args[0] = "java";
        args[1] = "-classpath";
        args[2] = System.getProperty("java.class.path", ".");
        args[3] = Backend.class.getName();
        args[4] = Integer.toString(port);
        Process proc = Runtime.getRuntime().exec(args);
        new CopyThread(proc.getErrorStream(), System.err);
        new CopyThread(proc.getInputStream(), System.out);
        try {
            Thread.sleep(15000);
        } catch (InterruptedException ex) {
        }
        if (!isPortInUse(port)) {
            proc.destroy();
            throw new IOException("Backend process failed to open port");
        }
        return true;
    }

    /** Simple thread to copy backend stdout and stderr */
    private static class CopyThread extends Thread {
        private InputStream is;

        private PrintStream os;

        public CopyThread(InputStream is, PrintStream os) {
            this.is = is;
            this.os = os;
            start();
        }

        public void run() {
            try {
                while (true) {
                    int chr = is.read();
                    if (chr < 0)
                        break;
                    os.print((char) chr);
                }
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Determine if specfied port is in use
     * 
     * @param port the port number
     * @return true if port is in use
     */
    private static boolean isPortInUse(int port) {
        try {
            Socket sock = new Socket("127.0.0.1", port);
            try {
                sock.close();
            } catch (IOException ex) {
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Sets the <tt>ErrorCallback</tt> class to which errors detected backend
     * the backend servers will be reported. Only one of these an be active at
     * any time.
     * 
     * @param callback Callback interface to be called to report errors, or null
     *        to release the interface.
     */
    public static void setErrorCallback(ErrorCallback callback) {
        // A null callback means we want to stop listening.
        if (callback == null) {
            if (errorMonitor != null) {
                errorMonitor.stopRunning();
            }
            errorMonitor = null;
            // If errorMonitor is null, we need to make one.
        } else if (errorMonitor == null) {
            ServerSocket errorServer = null;
            try {
                errorServer = new ServerSocket(ERROR_PORT);
            } catch (IOException e) {
                callback.error(e);
                return;
            }
            errorMonitor = new ErrorMonitor(callback, errorServer);
            Thread errorThread = new Thread(errorMonitor, "ErrorMonitorThread");
            errorThread.setDaemon(true);
            errorThread.start();
            Thread.yield(); // let it start up.
            // The errorMonitor is already running,
            // just redirect the error callback.
        } else {
            errorMonitor.setErrorCallback(callback);
        }
    }

    /**
     * Inner class that listens for errors reported from the backends and
     * redirects them to the correct error callback.
     */
    public static class ErrorMonitor implements Runnable {
        private volatile ErrorCallback callback;

        private volatile boolean isStopped = true;

        private ServerSocket listenSocket = null;

        ErrorMonitor(ErrorCallback cb, ServerSocket listen) {
            callback = cb;
            listenSocket = listen;
        }

        public void setErrorCallback(ErrorCallback cb) {
            callback = cb;
        }

        public void stopRunning() {
            isStopped = true;
            try {
                listenSocket.close();
            } catch (IOException ignored) {
            }
            listenSocket = null;
        }

        public void run() {
            try {
                isStopped = false;
                while (!isStopped) {
                    Socket sock = listenSocket.accept();
                    try {
                        sock.setSoTimeout(1000);
                        ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
                        Throwable error = (Throwable) ois.readObject();
                        callback.error(error);
                        sock.close();
                    } catch (Exception ex) {
                        callback.error(ex);
                    }
                }
            } catch (IOException ex) {
                if (!isStopped) {
                    callback.error(ex);
                    try {
                        listenSocket.close();
                    } catch (IOException ignored) {
                    }
                }
                isStopped = true;
            }
        }
    }

    /**
     * Shutdown normal backend process
     */
    public static void shutdown() {
        shutdown(false);
    }
    
    /**
     * Shutdown backend process
     * 
     * @param reject true to shut down the reject backend process false to shut
     *        down the normal reject process
     */
    public static void shutdown(boolean reject) {
        /*
         * This might be called from an entirely separate JVM than the one
         * running the backend, so we can't trust any of the mutable data
         * members.
         * 
         * Just opening a cnonection to the shutdown port should do it
         */
        isPortInUse(reject ? SHUTDOWN_REJECT_PORT : SHUTDOWN_PORT);
    }

    /**
     * Main method is necessary to run a stand-alone server that tests can be
     * run off of.
     */
    public static void main(String[] args) throws IOException {
        boolean shutdown = false;

        int port = Integer.parseInt(args[args.length - 1]);
        if (shutdown) {
            shutdown(port == REJECT_PORT);
        } else {
            new Backend(port);
        }
    }

    /**
     * Constructs and launches a new <tt>Backend</tt>.
     * 
     * @param reject true to launch a reject server
     */
    // private Backend(boolean reject) throws IOException {
    private Backend(int port) throws IOException {
        super("Backend");
       
        System.getProperties().setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        
        
        LimeWireCore limeWireCore = null;
        int shutdownPort = (port == BACKEND_PORT ? SHUTDOWN_PORT
                : port == REJECT_PORT ? SHUTDOWN_REJECT_PORT
                        : port == LEAF_PORT ? SHUTDOWN_LEAF_PORT : -1);

        ServerSocket shutdownSocket = null;
        boolean reject = (port == REJECT_PORT);
        try {
            shutdownSocket = new ServerSocket(shutdownPort);

            preSetUp();
            setStandardSettings(port);
            limeWireCore = Guice.createInjector(Stage.PRODUCTION,
                                                new LimeWireCoreModule(ActivityCallbackStub.class))
                                .getInstance(LimeWireCore.class);
            limeWireCore.getLifecycleManager().start();
            populateSharedDirectory(limeWireCore.getFileManager());
            if (!reject)
                limeWireCore.getConnectionServices().connect();

            try {
                // sleep to let the file manager initialize
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            if (limeWireCore.getNetworkManager().getPort() != port) {
                throw new IOException("Opened wrong port (wanted: " + port + ", was: "
                        + limeWireCore.getNetworkManager().getPort() + ")");
            }

            waitForShutdown(shutdownSocket);
            doShutdown(limeWireCore, reject, "");
        } catch (Exception ex) {
            ErrorService.error(ex);
            doShutdown(limeWireCore, reject, "Exception thrown by backend shutdown monitor");
        } finally {
            try {
                if (shutdownSocket != null)
                    shutdownSocket.close();
            } catch (IOException ex2) {
            }
            try {
                postTearDown();
            } finally {
                cleanFiles(_baseDir, true); // get rid of tmp dirs.
            }
        }

    }

    private void waitForShutdown(ServerSocket shutdownSocket) throws IOException {

        Socket sock = null;
        try {
            while (true) {
                sock = shutdownSocket.accept();
                String host = sock.getInetAddress().getHostAddress();
                sock.close();
                if (host.equals("127.0.0.1"))
                    break;
            }
        } catch (IOException ex) {
            try {
                if (sock != null)
                    sock.close();
            } catch (IOException ex2) {
            }
            throw ex;
        }
    }

    /**
     * Creates a temporary shared directory for testing purposes.
     */
    private void populateSharedDirectory(FileManager fileManager) {
        File coreDir;
        coreDir = TestUtils.getResourceFile("com/limegroup/gnutella");
        File[] files = coreDir.listFiles();

        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (!files[i].isFile())
                    continue;
                fileManager.getGnutellaFileList().add(files[i]);
            }
        }
    }

    /**
     * Sets the standard settings for a test backend, such as the ports, the
     * number of connections to maintain, etc.
     */
    private void setStandardSettings(int port) {
        SearchSettings.GUESS_ENABLED.setValue(true);

        UltrapeerSettings.DISABLE_ULTRAPEER_MODE.setValue(false);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);

        NetworkSettings.PORT.setValue(port);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.ACCEPT_DEFLATE.setValue(true);
        ConnectionSettings.ENCODE_DEFLATE.setValue(true);

        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(new String[] { "*.*.*.*" });
        try {
            FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(new String[] { "127.*.*.*",
                    InetAddress.getLocalHost().getHostAddress() });
        } catch (UnknownHostException bad) {
            fail(bad);
        }
    }

    /**
     * Notifies <tt>RouterService</tt> that the backend should be shut down.
     */
    private void doShutdown(LimeWireCore limeWireCore, boolean reject, String msg) {
        if(limeWireCore != null)
            limeWireCore.getLifecycleManager().shutdown();
        System.exit(0);
    }

    /** Handles throwable error report from the backend */
    public void error(Throwable ex) {
        // First try to serialize the exception to the error port
        try {
            Socket sock = new Socket("127.0.0.1", ERROR_PORT);
            ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
            oos.writeObject(ex);
            oos.flush();
            sock.close();
            return;
        } catch (IOException ex2) {
        }

        // If that didn't work, print a stack tracke and throw a runtime
        // exception
        ex.printStackTrace();
        throw new RuntimeException(ex);
    }
}
