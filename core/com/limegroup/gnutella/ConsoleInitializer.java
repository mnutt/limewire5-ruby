package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.Vector;

import javax.swing.UIManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.net.SocketsManager.ConnectType;
import org.limewire.service.ErrorService;
import org.limewire.service.MessageService;
import org.limewire.ui.support.BugManager;
import org.limewire.ui.support.DeadlockSupport;
import org.limewire.ui.support.ErrorHandler;
import org.limewire.ui.swing.DefaultErrorCatcher;
import org.limewire.ui.swing.GURLHandler;
import org.limewire.ui.swing.LimeWireModule;
import org.limewire.ui.swing.MacEventHandler;
import org.limewire.ui.swing.MessageHandler;
import org.limewire.ui.swing.SettingsWarningManager;
import org.limewire.ui.swing.UncaughtExceptionHandlerImpl;
import org.limewire.ui.swing.event.ExceptionPublishingSwingEventService;
import org.limewire.ui.swing.mainframe.AppFrame;
import org.limewire.ui.swing.settings.StartupSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.LocaleUtils;
import org.limewire.ui.swing.util.MacOSXUtils;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.Stopwatch;
import org.limewire.util.SystemUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.gnutella.LimeCoreGlue.InstallFailedException;
import com.limegroup.gnutella.browser.ExternalControl;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.util.LogUtils;
import com.limegroup.gnutella.version.UpdateInformation;

public class ConsoleInitializer {
    
    /** The log -- set only after Log4J can be determined. */
    private final Log LOG;
    
    /** Refuse to start after this date */
    private final long EXPIRATION_DATE = Long.MAX_VALUE;
    
    /** True if is running from a system startup. */
    private volatile boolean isStartup = false;
    
    /** The start memory -- only set if debugging. */
    private long startMemory;
    
    /** A stopwatch for debug logging. */
    private final Stopwatch stopwatch;
  
    
    ConsoleInitializer() {
        // If Log4J is available then remove the NoOpLog
        if (LogUtils.isLog4JAvailable()) {
            System.getProperties().remove("org.apache.commons.logging.Log");
        }
        
        LOG = LogFactory.getLog(ConsoleInitializer.class);
        
        if(LOG.isTraceEnabled()) {
            startMemory = Runtime.getRuntime().totalMemory()
                        - Runtime.getRuntime().freeMemory();
            LOG.trace("START Initializer, using: " + startMemory + " memory");
        }
        
        stopwatch = new Stopwatch(LOG);
            }
    
    public void initialize(String args[]) {
        preinit();
        
        // Various startup tasks...
        setupCallbacksAndListeners();     
        validateStartup(args);
        
        // Creates LimeWire itself.
        LimeWireCore limeWireCore = createLimeWire();
        Injector injector = limeWireCore.getInjector();
       
        // Various tasks that can be done after core is glued & started.
        glueCore(limeWireCore);        
        validateEarlyCore(limeWireCore);
        
        // Validate any arguments or properties outside of the LW environment.
        runExternalChecks(limeWireCore, args, injector);

        // Starts some system monitoring for deadlocks.
        DeadlockSupport.startDeadlockMonitoring();
        stopwatch.resetAndLog("Start deadlock monitor");
        
        // Installs properties.
        installProperties();
        
        enablePreferences();
        
        startEarlyCore(limeWireCore);
        
        SettingsWarningManager.checkTemporaryDirectoryUsage();
        SettingsWarningManager.checkSettingsLoadSaveFailure();        
        
        // Start the core & run any queued control requests, and load DAAP.
        startCore(limeWireCore);
        runQueuedRequests(limeWireCore);
        
        // Run any after-init tasks.
        postinit();
        System.out.println("For a command list type help.");
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        for ( ; ;) {
            System.out.print("LimeRouter> ");
            try {
                String command=in.readLine();
                if (command==null)
                    break;
                else if (command.equals("help")) {
                    System.out.println("catcher                  "+
                                       "Print host catcher.");
                    System.out.println("connect <host> [<port>]  "+
                                       "Connect to a host[:port].");
                    System.out.println("help                     "+
                                       "Print this message.");
                    System.out.println("listen <port>            "+
                                       "Set the port you are listening on.");
                    //              System.out.println("push                     "+
                    //                "Print push routes.");
                    System.out.println("query <string>           "+
                                       "Send a query to the network.");
                    System.out.println("quit                     "+
                                       "Quit the application.");
                    //              System.out.println("route                    "+
                    //                "Print routing tables.");
                    //              System.out.println("stat                     "+
                    //                "Print statistics.");
                    System.out.println("update                   "+
                                       "Send pings to update the statistics.");
                }
                else if (command.equals("quit"))
                    break;
                //          //Print routing tables
                //          else if (command.equals("route"))
                //              RouterService.dumpRouteTable();
                //          //Print connections
                //          else if (command.equals("push"))
                //              RouterService.dumpPushRouteTable();
                //Print push route
            
                String[] commands=split(command);
                //Connect to remote host (establish outgoing connection)
                if (commands.length>=2 && commands[0].equals("connect")) {
                    try {
                        int port=6346;
                        if (commands.length>=3)
                            port=Integer.parseInt(commands[2]);
                        System.out.println("Connecting...");
                        limeWireCore.getConnectionServices().connectToHostAsynchronously(commands[1], port, ConnectType.PLAIN);
                    } catch (NumberFormatException e) {
                        System.out.println("Please specify a valid port.");
                    }
                } else if (commands.length>=2 && commands[0].equals("query")) {
                    //Get query string from command (possibly multiple words)
                    int i=command.indexOf(' ');
                    assert(i!=-1 && i<command.length());
                    String query=command.substring(i+1);
                    SearchServices searchServices = limeWireCore.getSearchServices();
                    searchServices.query(searchServices.newQueryGUID(), query);
                } else if (commands.length==2 && commands[0].equals("listen")) {
                    try {
                        int port=Integer.parseInt(commands[1]);
                        limeWireCore.getNetworkManager().setListeningPort(port);
                    } catch (NumberFormatException e) {
                        System.out.println("Please specify a valid port.");
                    } catch (IOException e) {
                        System.out.println("Couldn't change port.  Try another value.");
                    }
                }
            } catch (IOException e) {
                System.exit(1);
            }
        }
        System.out.println("Good bye.");
        limeWireCore.getLifecycleManager().shutdown(); //write gnutella.net
    }
    
    
    
    
    
    
    /** Initializes the very early things. */
    /*
     * DO NOT CHANGE THIS WITHOUT KNOWING WHAT YOU'RE DOING.
     * PREINSTALL MUST BE DONE BEFORE ANYTHING ELSE IS REFERENCED.
     * (Because it sets the preference directory in CommonUtils.)
     */
    private void preinit() {
        // Make sure the settings directory is set.
        try {
            LimeCoreGlue.preinstall();
            stopwatch.resetAndLog("Preinstall");
        } catch(InstallFailedException ife) {
            failPreferencesPermissions();
        }

        // Before anything, set a default L&F, so that
        // if an error occurs, we can display the error
        // message with the right L&F.
        SwingUtils.invokeLater(new Runnable() {
            public void run() {
                String name = UIManager.getSystemLookAndFeelClassName();                
                if(OSUtils.isLinux()) {
                    //mozswing on linux is not compatible with the gtklook and feel in jvms less than 1.7
                    //forcing cross platform look and feel for linux.
                    name = UIManager.getCrossPlatformLookAndFeelClassName();
                }           
                try {
                    UIManager.setLookAndFeel(name);
                } catch(Throwable ignored) {}
            }
        });        
    }
    
    /** Installs all callbacks & listeners. */
    private void setupCallbacksAndListeners() {
        SwingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                BugManager.instance();
            }
        });
        // Set the error handler so we can receive core errors.
        ErrorService.setErrorCallback(new ErrorHandler());

        // set error handler for uncaught exceptions originating from non-LW
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandlerImpl());
        stopwatch.resetAndLog("ErrorHandler install");
        
        // Set the messaging handler so we can receive core messages
        MessageService.setCallback(new MessageHandler());
        stopwatch.resetAndLog("MessageHandler install");
        
        // Set the default event error handler so we can receive uncaught
        // AWT errors.
        DefaultErrorCatcher.install();
        stopwatch.resetAndLog("DefaultErrorCatcher install");

        //Enable the EDT event service (used by the EventBus library) that publishes to LW error handling
        ExceptionPublishingSwingEventService.install();
        stopwatch.resetAndLog("DefaultErrorCatcher install");
        
        if (OSUtils.isMacOSX()) {
            // Raise the number of allowed concurrent open files to 1024.
            SystemUtils.setOpenFileLimit(1024);
            stopwatch.resetAndLog("Open file limit raise");     

            MacEventHandler.instance();
            stopwatch.resetAndLog("MacEventHandler instance");
        }
    }
    
    /**
     * Ensures this should continue running, by checking
     * for expiration failures or startup settings. 
     */
    private void validateStartup(String[] args) {        
        // check if this version has expired.
        if (System.currentTimeMillis() > EXPIRATION_DATE) 
            failExpired();
        
        // Yield so any other events can be run to determine
        // startup status, but only if we're going to possibly
        // be starting...
        if(StartupSettings.RUN_ON_STARTUP.getValue()) {
            stopwatch.reset();
            Thread.yield();
            stopwatch.resetAndLog("Thread yield");
        }
        
        if (args.length >= 1 && "-startup".equals(args[0]))
            isStartup = true;
        
        if (isStartup) {
            args = null; // reset for later Active check
            // if the user doesn't want to start on system startup, exit the
            // JVM immediately
            if(!StartupSettings.RUN_ON_STARTUP.getValue())
                System.exit(0);
        }
        
        // Exit if another LimeWire is already running...
        ActiveLimeWireCheck activeLimeWireCheck = new ActiveLimeWireCheck(args, StartupSettings.ALLOW_MULTIPLE_INSTANCES.getValue());
        stopwatch.resetAndLog("Create ActiveLimeWireCheck");
        if (activeLimeWireCheck.checkForActiveLimeWire()) {
            System.exit(0);
        }
        stopwatch.resetAndLog("Run ActiveLimeWireCheck");
    }
    
    /** Wires together LimeWire. */
    private LimeWireCore createLimeWire() {
        stopwatch.reset();
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new LimeWireModule(), new AbstractModule() {
            @Override
            protected void configure() {
                requestStaticInjection(AppFrame.class);
            }
        });
        stopwatch.resetAndLog("Create injector");
        return injector.getInstance(LimeWireCore.class);
    }
    
    /** Wires together remaining non-Guiced pieces. */
    private void glueCore(LimeWireCore limeWireCore) {
        limeWireCore.getLimeCoreGlue().install();
        stopwatch.resetAndLog("Install core glue");
    }
    
    /** Tasks that can be done after core is created, before it's started. */
    private void validateEarlyCore(LimeWireCore limeWireCore) {        
        // See if our NIODispatcher clunked out.
        if(!limeWireCore.getNIODispatcher().isRunning()) {
            failInternetBlocked();
        }
        stopwatch.resetAndLog("Check for NIO dispatcher");
    }    

    /**
     * Initializes any code that is dependent on external controls.
     * Specifically, GURLHandler & MacEventHandler on OS X,
     * ensuring that multiple LimeWire's can't run at once,
     * and processing any arguments that were passed to LimeWire.
     */ 
    private void runExternalChecks(LimeWireCore limeWireCore, String[] args, Injector injector) {        
        ExternalControl externalControl = limeWireCore.getExternalControl();
        stopwatch.resetAndLog("Get externalControl");
        if(OSUtils.isMacOSX()) {
            GURLHandler.getInstance().enable(externalControl);
            stopwatch.resetAndLog("Enable GURL");
            injector.injectMembers(MacEventHandler.instance());
            stopwatch.resetAndLog("Enable macEventHandler");
        }
        
        // Test for preexisting LimeWire and pass it a magnet URL if one
        // has been passed in.
        if (args.length > 0 && !args[0].equals("-startup")) {
            String arg = ExternalControl.preprocessArgs(args);
            stopwatch.resetAndLog("Preprocess args");
            externalControl.enqueueControlRequest(arg);
            stopwatch.resetAndLog("Enqueue control req");
        }
    }
    
    /** Installs any system properties. */
    private void installProperties() {        
        System.setProperty("http.agent", LimeWireUtils.getHttpServer());
        stopwatch.resetAndLog("set system properties");
        
        if (OSUtils.isMacOSX()) {
            System.setProperty("user.fullname", MacOSXUtils.getUserName()); // for DAAP
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            stopwatch.resetAndLog("set OSX properties");
        }

        SwingUtils.invokeAndWait(new Runnable() {
            public void run() {
                LocaleUtils.setLocaleFromPreferences();                
                LocaleUtils.validateLocaleAndFonts();
            }
        });
        stopwatch.resetAndLog("set locale");
    }
    
    /** Starts any early core-related functionality. */
    private void startEarlyCore(LimeWireCore limeWireCore) {        
        // Add this running program to the Windows Firewall Exceptions list
        boolean inFirewallException = limeWireCore.getFirewallService().addToFirewall();
        stopwatch.resetAndLog("add firewall exception");
        
        if(!inFirewallException) {
            limeWireCore.getLifecycleManager().loadBackgroundTasks();
            stopwatch.resetAndLog("load background tasks");
        }
    }
    
    /** Starts the core. */
    private void startCore(LimeWireCore limeWireCore) {
        // Start the backend threads.  Note that the GUI is not yet visible,
        // but it needs to be constructed at this point  
        limeWireCore.getLifecycleManager().start();
        stopwatch.resetAndLog("lifecycle manager start");
        
        if (!ConnectionSettings.DISABLE_UPNP.getValue()) {
            limeWireCore.getUPnPManager().start();
            stopwatch.resetAndLog("start UPnPManager");
        }
    }
    
    private void enablePreferences() {        
        if (OSUtils.isMacOSX()) {
            MacEventHandler.instance().enablePreferences();
        }
    }
    
    /** Runs control requests that we queued early in initializing. */
    private void runQueuedRequests(LimeWireCore limeWireCore) {        
        // Activate a download for magnet URL locally if one exists
        limeWireCore.getExternalControl().runQueuedControlRequest();
        stopwatch.resetAndLog("run queued control req");
    }
    
    /** Runs post initialization tasks. */
    private void postinit() {
        if(LOG.isTraceEnabled()) {
            long stopMemory = Runtime.getRuntime().totalMemory()
                            - Runtime.getRuntime().freeMemory();
            LOG.trace("STOP Initializer, using: " + stopMemory +
                      " memory, consumed: " + (stopMemory - startMemory));
        }
    }
    
    /**
     * Sets the startup property to be true.
     */
    void setStartup() {
        isStartup = true;
    }
    
    
    /** Fails because preferences can't be set. */
    private void failPreferencesPermissions() {
        fail(I18n.tr("LimeWire could not create a temporary preferences folder.\n\nThis is generally caused by a lack of permissions.  Please make sure that LimeWire (and you) have access to create files/folders on your computer.  If the problem persists, please visit www.limewire.com and click the \'Support\' link.\n\nLimeWire will now exit.  Thank You."));
    }
    
    /** Fails because alpha expired. */
    private void failExpired() {
        fail(I18n.tr("This Alpha version has expired.  Press Ok to exit. "));
    }
    
    /** Fails because internet is blocked. */
    private void failInternetBlocked() {
        fail(I18n
                .tr("LimeWire was unable to initialize and start. This is usually due to a firewall program blocking LimeWire\'s access to the internet or loopback connections on the local machine. Please allow LimeWire access to the internet and restart LimeWire."));
    }
    
    /** Shows a msg & fails. */
    private void fail(final String msgKey) {
        System.out.println(msgKey);
        System.exit(1);
    }
    
    
    
    
    /** Returns an array of strings containing the words of s, where
     *  a word is any sequence of characters not containing a space.
     */
    public static String[] split(String s) {
        s=s.trim();
        int n=s.length();
        if (n==0)
            return new String[0];
        Vector<String> buf=new Vector<String>();

        //s[i] is the start of the word to add to buf
        //s[j] is just past the end of the word
        for (int i=0; i<n; ) {
            assert(s.charAt(i)!=' ');
            int j=s.indexOf(' ',i+1);
            if (j==-1)
                j=n;
            buf.add(s.substring(i,j));
            //Skip past whitespace (if any) following s[j]
            for (i=j+1; j<n ; ) {
                if (s.charAt(i)!=' ')
                    break;
                i++;
            }
        }
        String[] ret=new String[buf.size()];
        for (int i=0; i<ret.length; i++)
            ret[i]= buf.get(i);
        return ret;
    }

    
    
    @Singleton
    private static class MainCallback implements ActivityCallback {

        /////////////////////////// ActivityCallback methods //////////////////////
    
        public void connectionInitializing(RoutedConnection c) {
        }
    
        public void connectionInitialized(RoutedConnection c) {
    //      String host = c.getOrigHost();
    //      int    port = c.getOrigPort();
            ;//System.out.println("Connected to "+host+":"+port+".");
        }
    
        public void connectionClosed(RoutedConnection c) {
    //      String host = c.getOrigHost();
    //      int    port = c.getOrigPort();
            //System.out.println("Connection to "+host+":"+port+" closed.");
        }
    
        public void knownHost(Endpoint e) {
            //Do nothing.
        }
    
    //     public void handleQueryReply( QueryReply qr ) {
    //      synchronized(System.out) {
    //          System.out.println("Query reply from "+qr.getIP()+":"+qr.getPort()+":");
    //          try {
    //              for (Iterator iter=qr.getResults(); iter.hasNext(); )
    //                  System.out.println("   "+((Response)iter.next()).getName());
    //          } catch (BadPacketException e) { }
    //      }
    //     }
    
        public void handleQueryResult(RemoteFileDesc rfd , QueryReply queryReply, Set<? extends IpPort> loc) {
            synchronized(System.out) {
                System.out.println("Query hit from "+rfd.getAddress() + ":");
                System.out.println("   "+rfd.getFileName());
            }
        }
    
        /**
         *  Add a query string to the monitor screen
         */
        public void handleQueryString( String query ) {
        }
    
    
        public void error(int errorCode) {
            error(errorCode, null);
        }
        
        public void error(Throwable problem, String msg) {
            problem.printStackTrace();
            System.out.println(msg);
        }
    
        /**
         * Implements ActivityCallback.
         */
        public void error(Throwable problem) {
            problem.printStackTrace();
        }
    
        public void error(int message, Throwable t) {
            System.out.println("Error: "+message);
            t.printStackTrace();
        }
    
        ///////////////////////////////////////////////////////////////////////////

    
        public void addDownload(Downloader mgr) {}
    
        public void downloadCompleted(Downloader mgr) {}
    
        public void addUpload(Uploader mgr) {}
    
        public void removeUpload(Uploader mgr) {}
    
        public boolean warnAboutSharingSensitiveDirectory(final File dir) { return false; }
        
        public void handleSharedFileUpdate(File file) {}
    
        public void downloadsComplete() {}    
        
        public void uploadsComplete() {}
    
        public void promptAboutCorruptDownload(Downloader dloader) {
            dloader.discardCorruptDownload(false);
        }
    
        public void restoreApplication() {}
    
        public void showDownloads() {}
    
        public String getHostValue(String key){
            return null;
        }
        public void browseHostFailed(GUID guid) {}
        
        public void updateAvailable(UpdateInformation update) {
            if (update.getUpdateCommand() != null)
                System.out.println("there's a new version out "+update.getUpdateVersion()+
                        ", to get it shutdown limewire and run "+update.getUpdateCommand());
            else
                System.out.println("You're running an older version.  Get " +
                             update.getUpdateVersion() + ", from " + update.getUpdateURL());
        }  
    
        public boolean isQueryAlive(GUID guid) {
            return false;
        }
        
        public void componentLoading(String state, String component) {
            System.out.println("Loading component: " + component);
        }
        
        public void handleMagnets(final MagnetOptions[] magnets) {
        }
    
        public void handleTorrent(File torrentFile){}

        public void handleAddressStateChanged() {
        }
        
        public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        }
        public void installationCorrupted() {
            
        }
        public void handleDAAPConnectionError(Throwable t) {  }
        public String translate(String s) { return s;}

        @Override
        public void handleSaveLocationException(DownloadAction downLoadAction,
                SaveLocationException sle, boolean supportsNewSaveDir) {
            
        }

        @Override
        public void promptTorrentUploadCancel(ManagedTorrent torrent) {
            
        }
    }
}