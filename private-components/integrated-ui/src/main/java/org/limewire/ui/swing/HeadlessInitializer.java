package org.limewire.ui.swing;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.inject.GuiceUtils;
import org.limewire.net.FirewallService;
import org.limewire.nio.NIODispatcher;
import org.limewire.service.ErrorService;
import org.limewire.service.MessageService;
import org.limewire.ui.support.BugManager;
import org.limewire.ui.support.DeadlockSupport;
import org.limewire.ui.support.ErrorHandler;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.event.ExceptionPublishingSwingEventService;
import org.limewire.ui.swing.mainframe.AppFrame;
import org.limewire.ui.swing.settings.StartupSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.LocaleUtils;
import org.limewire.ui.swing.util.MacOSXUtils;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.Stopwatch;
import org.limewire.util.SystemUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Stage;
import com.limegroup.gnutella.ActiveLimeWireCheck;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.LimeCoreGlue;
import com.limegroup.gnutella.UPnPManager;
import com.limegroup.gnutella.ActiveLimeWireCheck.ActiveLimeWireException;
import com.limegroup.gnutella.LimeCoreGlue.InstallFailedException;
import com.limegroup.gnutella.browser.ExternalControl;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.util.LogUtils;

/** Initializes (creates, starts, & displays) the LimeWire Core & UI. */
public final class HeadlessInitializer {

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
        
    // Providers so we don't guarantee early creation, let it be as lazy as possible.
    @Inject private Provider<ExternalControl> externalControl;
    @Inject private Provider<FirewallService> firewallServices;
    @Inject private Provider<LifecycleManager> lifecycleManager;
    @Inject private Provider<LimeCoreGlue> limeCoreGlue;
    @Inject private Provider<NIODispatcher> nioDispatcher;
    @Inject private Provider<UPnPManager> upnpManager;
    
    HeadlessInitializer() {
        // If Log4J is available then remove the NoOpLog
        if (LogUtils.isLog4JAvailable()) {
            System.getProperties().remove("org.apache.commons.logging.Log");
        }
        
        LOG = LogFactory.getLog(Initializer.class);
        
        if(LOG.isTraceEnabled()) {
            startMemory = Runtime.getRuntime().totalMemory()
                        - Runtime.getRuntime().freeMemory();
            LOG.trace("START Initializer, using: " + startMemory + " memory");
        }
        
        stopwatch = new Stopwatch(LOG);
    }
    
    /**
     * Initializes all of the necessary application classes.
     * 
     * If this throws any exceptions, then LimeWire was not able to construct
     * properly and must be shut down.
     */
    void initialize(String args[]) throws Throwable { 
        // ** THE VERY BEGINNING -- DO NOT ADD THINGS BEFORE THIS **
        preinit();
        
        // Various startup tasks...
        setupCallbacksAndListeners();     
        validateStartup(args);
        
        // Creates LimeWire itself.
        Injector injector = createLimeWire();
       
        // Various tasks that can be done after core is glued & started.
        glueCore();        
        validateEarlyCore();
        
        // Validate any arguments or properties outside of the LW environment.
        runExternalChecks(args, injector);

        // Starts some system monitoring for deadlocks.
        DeadlockSupport.startDeadlockMonitoring();
        stopwatch.resetAndLog("Start deadlock monitor");
        
        // Installs properties.
        installProperties();
        
        startEarlyCore();
        
        SettingsWarningManager.checkTemporaryDirectoryUsage();
        SettingsWarningManager.checkSettingsLoadSaveFailure();        
        
        // Start the core & run any queued control requests, and load DAAP.
        startCore();
        runQueuedRequests();
        
        // Run any after-init tasks.
        postinit();        
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
        if(!StartupSettings.ALLOW_MULTIPLE_INSTANCES.getValue()) {
            ActiveLimeWireCheck activeCheck = ActiveLimeWireCheck.instance();
            stopwatch.resetAndLog("Create ActiveLimeWireCheck");
            try {
                if(activeCheck.checkForActiveLimeWire(args))
                    System.exit(0);
            } catch(ActiveLimeWireException e) {
                LOG.debug(e);
                stopwatch.resetAndLog("Warn user about running instance");
                GuiUtils.hideAndDisposeAllWindows();
                if(!warnAlreadyRunning())
                    System.exit(0);
            }
            stopwatch.resetAndLog("Run ActiveLimeWireCheck");
        }
    }
    
    /**
     * Warns the user that another instance of LimeWire appears to be
     * running and that it should be shut down before proceeding.
     * 
     * @return true if the user chooses to proceed.
     */
    private boolean warnAlreadyRunning() {
        final AtomicInteger response =
            new AtomicInteger(JOptionPane.CANCEL_OPTION);
        final String message = I18n.tr("Another instance of LimeWire " +
                "appears to be running. Please completely shut down all " +
                "other instances of LimeWire before continuing. If this " +
                "problem persists, please restart your computer.");
        SwingUtils.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                response.set(JOptionPane.showConfirmDialog(null,
                        new MultiLineLabel(message, 300),
                        I18n.tr("LimeWire is already running"),
                        JOptionPane.OK_CANCEL_OPTION));
            }
        });
        return response.get() == JOptionPane.OK_OPTION;
    }
    
    /** Wires together LimeWire. */
    private Injector createLimeWire() {
        stopwatch.reset();
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new LimeWireModule(), new AbstractModule() {
            @Override
            protected void configure() {
                requestStaticInjection(AppFrame.class);
                requestInjection(HeadlessInitializer.this);
            }
        });
        GuiceUtils.loadEagerSingletons(injector);
        stopwatch.resetAndLog("Create injector");
        return injector;
    }
    
    /** Wires together remaining non-Guiced pieces. */
    private void glueCore() {
        limeCoreGlue.get().install();
        stopwatch.resetAndLog("Install core glue");
    }
    
    /** Tasks that can be done after core is created, before it's started. */
    private void validateEarlyCore() {        
        // See if our NIODispatcher clunked out.
        if(!nioDispatcher.get().isRunning()) {
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
    private void runExternalChecks(String[] args, Injector injector) {        
        
        // Test for preexisting LimeWire and pass it a magnet URL if one
        // has been passed in.
        if (args.length > 0 && !args[0].equals("-startup")) {
            String arg = ExternalControl.preprocessArgs(args);
            stopwatch.resetAndLog("Preprocess args");
            externalControl.get().enqueueControlRequest(arg);
            stopwatch.resetAndLog("Enqueue control req");
        }
    }
    
    /** Installs any system properties. */
    private void installProperties() {        
        System.setProperty("http.agent", LimeWireUtils.getHttpServer());
        stopwatch.resetAndLog("set system properties");
        
        if (OSUtils.isMacOSX()) {
            System.setProperty("user.fullname", MacOSXUtils.getUserName()); // for DAAP
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
    private void startEarlyCore() {        
        // Add this running program to the Windows Firewall Exceptions list
        boolean inFirewallException = firewallServices.get().addToFirewall();
        stopwatch.resetAndLog("add firewall exception");
        
        if(!inFirewallException) {
            lifecycleManager.get().loadBackgroundTasks();
            stopwatch.resetAndLog("load background tasks");
        }
    }
    
    /** Starts the core. */
    private void startCore() {
        // Start the backend threads.  Note that the GUI is not yet visible,
        // but it needs to be constructed at this point  
        lifecycleManager.get().start();
        stopwatch.resetAndLog("lifecycle manager start");
        
        if (!ConnectionSettings.DISABLE_UPNP.getValue()) {
            upnpManager.get().start();
            stopwatch.resetAndLog("start UPnPManager");
        }
    }
    
    /** Runs control requests that we queued early in initializing. */
    private void runQueuedRequests() {        
        // Activate a download for magnet URL locally if one exists
        externalControl.get().runQueuedControlRequest();
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
    
    /** Fails because alpha expired. */
    private void failExpired() {
        fail(I18n.tr("This Alpha version has expired.  Press Ok to exit. "));
    }
    
    /** Fails because internet is blocked. */
    private void failInternetBlocked() {
        fail(I18n
                .tr("LimeWire was unable to initialize and start. This is usually due to a firewall program blocking LimeWire\'s access to the internet or loopback connections on the local machine. Please allow LimeWire access to the internet and restart LimeWire."));
    }
    
    /** Fails because preferences can't be set. */
    private void failPreferencesPermissions() {
        fail(I18n.tr("LimeWire could not create a temporary preferences folder.\n\nThis is generally caused by a lack of permissions.  Please make sure that LimeWire (and you) have access to create files/folders on your computer.  If the problem persists, please visit www.limewire.com and click the \'Support\' link.\n\nLimeWire will now exit.  Thank You."));
    }
    
    /** Shows a msg & fails. */
    private void fail(final String msgKey) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(null,
                            new MultiLineLabel(msgKey, 300),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        } catch (InterruptedException ignored) {
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if(cause instanceof RuntimeException)
                throw (RuntimeException)cause;
            if(cause instanceof Error)
                throw (Error)cause;
            throw new RuntimeException(cause);
        }
        System.exit(1);
    }
    
}

