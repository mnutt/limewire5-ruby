package com.limegroup.gnutella.util;

import java.io.File;
import java.io.IOException;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.ContentSettings;
import org.limewire.core.settings.DaapSettings;
import org.limewire.core.settings.FilterSettings;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.LimeProps;
import org.limewire.core.settings.SearchSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.core.settings.UltrapeerSettings;
import org.limewire.mojito.settings.MojitoProps;
import org.limewire.service.ErrorCallback;
import org.limewire.setting.SettingsGroupManager;
import org.limewire.util.BaseTestCase;
import org.limewire.util.PrivilegedAccessor;
import org.limewire.util.SystemUtils;
import org.limewire.util.TestUtils;

import com.limegroup.gnutella.Backend;
import com.limegroup.gnutella.LimeCoreGlue;

/**
 * Should be used when the test case requires to change settings.
 */
public abstract class LimeTestCase extends BaseTestCase implements ErrorCallback {
    
    protected static File _baseDir;
    protected static File _savedDir;
    protected static File _storeDir;
    protected static File _incompleteDir;
    protected static File _settingsDir;
    protected static File _scratchDir;
    
	/**
	 * Unassigned port for tests to use.
	 */
	protected static final int TEST_PORT = 49000;
    
    /* Flag indicate that we have launched the backend process and should
     * shut it down when we are finished.
     * NOTE. this has to be declared static because a separate TestCase is
     * insantiated for each test.
     */
    private static boolean[] shutdownBackend = new boolean[]{false, false};
    
    /**
     * The base constructor.
     * Nothing should ever be initialized in the constructor.
     * This is because of the way JUnit sets up tests --
     * It first builds a new instance of the class for every possible test,
     * then it runs through those instances, calling the appropriate test.
     * All pre & post initializations that are necessary for every test
     * should be in the new 'preSetUp' and 'postTearDown' methods.
     */    
    public LimeTestCase(String name) {
        super(name);
    }
    
    /**
     * Get test save directory
     */
    protected File getSaveDirectory() {
        return _savedDir;
    }
    
    /**
     * Get store directory
     */
    protected File getStoreDirectory() {
        return _storeDir;
    }
    
    /**
     * Launches all backend servers.
     *
     * @throws <tt>IOException</tt> if the launching of either
     *  backend fails
     */
    protected static void launchAllBackends() throws IOException {
        launchBackend(Backend.BACKEND_PORT);
        launchBackend(Backend.REJECT_PORT);
    }

    /**
     * Launch backend server if it is not running already
     * @throws IOException if attempt to launch backend server fails
     */
    protected static void launchBackend() throws IOException {
        launchBackend(Backend.BACKEND_PORT);
    }
    
    /**
     * Launch backend server if it is not running already
     * @throws IOException if attempt to launch backend server fails
     */
    private static void launchBackend(int port) throws IOException {
        
        /* If we've already launched the backend, don't try it again */
        int index = (port == Backend.REJECT_PORT ? 1 : 0);
        if (shutdownBackend[index]) return;
        
        /* Otherwise launch one if needed */
        shutdownBackend[index] = Backend.launch(port);
    }
    
    /**
     * Shutdown any backend servers that we started
     * This must be static so LimeTestSuite can call it.
     * (Which implicitly means that shutdownBackend[] must
     *  stay static also.)
     */
    protected static void shutdownBackends() {
        for (int ii = 0; ii < 2; ii++) {
            if (shutdownBackend[ii]) Backend.shutdown(ii == 1);
        }
        // Wait a couople seconds for any shutdown error reports.
        try { Thread.sleep(2000); } catch (InterruptedException ignore) {}
        Backend.setErrorCallback(null);
    }
    
    /**
     * Called before each test's setUp.
     * Used to determine which thread the test is running in,
     * set up the testing directories, and possibly print
     * debugging information (such as the current test being run)
     * This must also set the ErrorService's callback, so it
     * associates with the correct test object.
     */
    @Override
    protected void preSetUp() throws Exception {
        super.preSetUp();        
        setupUniqueDirectories();
        setupSettings();
        
        // The backend must also have its error callback reset
        // for each test, otherwise it could send errors to a stale
        // TestResult object (one whose test hasn't started or already ended).
        // But, we don't want to let the actual Backend class set it,
        // because that could provide an infinite loop of writing
        // to the socket, then reading it and rewriting it,
        // then reading it and rewriting it, etc...
        if (!(this instanceof Backend) )
            Backend.setErrorCallback(this);
    }
    
    /**
     * Called statically before any settings.
     */
    public static void beforeAllTestsSetUp() throws Throwable {        
        setupUniqueDirectories();
        setupSettings();
        // SystemUtils must pretend to not be loaded, so the idle
        // time isn't counted.
        // For tests that are testing SystemUtils specifically, they can
        // set loaded to true.
        SystemUtils.getIdleTime(); // make it loaded.
        // then unload it.
        PrivilegedAccessor.setValue(SystemUtils.class, "isLoaded", Boolean.FALSE);
    }
    
    /**
     * Called after each test's tearDown.
     * Used to remove directories and possibly other things.
     */
    @Override
    protected void postTearDown() {
        cleanFiles(_baseDir, false);
        super.postTearDown();
    }
    
    /**
     * Runs after all tests are completed.
     */
    public static void afterAllTestsTearDown() throws Throwable {
        cleanFiles(_baseDir, true);
        shutdownBackends();
    }
    
    /**
     * Sets up settings to a pristine environment for this test.
     * Ensures that no settings are saved.
     */
    protected static void setupSettings() throws Exception{
        SettingsGroupManager.instance().setShouldSave(false);
        SettingsGroupManager.instance().revertToDefault();
        LimeProps.instance().getFactory().getRevertSetting().setValue(false);
        MojitoProps.instance().getFactory().getRevertSetting().setValue(false);
        SharingSettings.FRIENDLY_HASHING.setValue(false);
        LibrarySettings.VERSION.setValue(LibrarySettings.LibraryVersion.FIVE_0_0.name());        
        LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.setValue(true);
        ApplicationSettings.INITIALIZE_SIMPP.setValue(false);
        ConnectionSettings.FILTER_CLASS_C.setValue(false);
        ConnectionSettings.DISABLE_UPNP.setValue(true);
        ConnectionSettings.ALLOW_DUPLICATE.setValue(true);
        FilterSettings.MAX_RESPONSES_PER_REPLY.setValue(256);
        ConnectionSettings.DO_NOT_MULTICAST_BOOTSTRAP.setValue(true);
        // TODO
        // SSLSettings.TLS_OUTGOING.setValue(false);
        UltrapeerSettings.NEED_MIN_CONNECT_TIME.setValue(false);
        SearchSettings.ENABLE_SPAM_FILTER.setValue(false);
        SharingSettings.setSaveDirectory(_savedDir);
        SharingSettings.setSaveLWSDirectory(_storeDir);
        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(false);
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(false);
        DaapSettings.DAAP_ENABLED.setValue(false);
        _incompleteDir = SharingSettings.INCOMPLETE_DIRECTORY.getValue();
    }
    
    /**
     * Creates a new directory prepended by the given name.
     */
    protected static File createNewBaseDirectory(String name) throws Exception {
        File t = getTestDirectory();
        File f = new File(t, name);
        
        int append = 1;
        while ( f.exists() ) {
            f = new File(t, name + "_" + append);
            append++;
        }
        
        return f.getCanonicalFile();
    }
    
    /**
     * Sets this test up to have unique directories.
     */
    protected static void setupUniqueDirectories() throws Exception {
        
        if( _baseDir == null ) {
            _baseDir = createNewBaseDirectory( _testClass.getName() );
        }
        _savedDir = new File(_baseDir, "saved");
        _settingsDir = new File(_baseDir, "settings");
        _storeDir = new File(_baseDir, "store");
        _scratchDir = new File(_baseDir, "scratch");

        _baseDir.mkdirs();
        _savedDir.mkdirs();
        _storeDir.mkdirs();
        _settingsDir.mkdirs();
        _scratchDir.mkdirs();
        
        // set the settings directory, then immediately change it.
        LimeCoreGlue.preinstall(_settingsDir);

        _baseDir.deleteOnExit();
    }
    
    /**
     * Get tests directory from a marker resource file.
     */
    protected static File getTestDirectory() throws Exception {
        return new File(getRootDir(), "testData");
    }   
    
    protected static File getGUIDir() throws Exception {
        return new File(getRootDir(), "gui");
    }
    
    protected static File getCoreDir() throws Exception {
        return new File(getRootDir(), "core");        
    }
    
    protected static File getRootDir() throws Exception {
        // Get a marker file.
        File f = TestUtils.getResourceFile("com/limegroup/gnutella/Backend.java");
        f = f.getCanonicalFile();
                 //gnutella       // limegroup    // com         // tests       // .
        return f.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();        
    }
}       

