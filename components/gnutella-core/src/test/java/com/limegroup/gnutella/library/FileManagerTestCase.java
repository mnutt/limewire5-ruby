package com.limegroup.gnutella.library;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.LocalSocketAddressProvider;
import org.limewire.io.LocalSocketAddressProviderStub;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.util.I18NConvert;
import org.limewire.util.StringUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.routing.QRPUpdater;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;

/**
 * Tests for FileManager and handling of queries.
 */
public class FileManagerTestCase extends LimeTestCase {

    public static final String SHARE_EXTENSION = "XYZ";
    public static final String EXTENSION = SHARE_EXTENSION + ";mp3";

    protected File f1 = null;
    protected File f2 = null;
    protected File f3 = null;
    protected File f4 = null;
    protected File f5 = null;
    protected File f6 = null;

    protected File store1 = null;
    protected File store2 = null;
    protected File store3 = null;

    // protected so that subclasses can
    // use these variables as well.
    @Inject protected Library library;
    @Inject protected FileViewManager fileViewManager;
    @Inject @GnutellaFiles protected FileView gnutellaFileView;
    @Inject protected QRPUpdater qrpUpdater = null;
    protected Object loaded = new Object();
    protected Response[] responses;
    protected List<FileDesc> sharedFiles;
    @Inject protected Injector injector;
    @Inject protected SharedFilesKeywordIndex keywordIndex;
    @Inject protected LimeXMLDocumentFactory limeXMLDocumentFactory;
    @Inject protected CreationTimeCache creationTimeCache;
    @Inject protected QueryRequestFactory queryRequestFactory;
    @Inject @GnutellaFiles protected FileCollection gnutellaFileCollection;

    public FileManagerTestCase(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);

        cleanFiles(_incompleteDir, false);
        cleanFiles(_storeDir, false);


        injector = LimeTestUtils.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                bind(LocalSocketAddressProvider.class).to(LocalSocketAddressProviderStub.class);
            }
        }, LimeTestUtils.createModule(this));
        injector.getInstance(ServiceRegistry.class).initialize();
        injector.getInstance(FileManager.class).start();
    }

    @Override
    protected void tearDown() {
        if (f1!=null) f1.delete();
        if (f2!=null) f2.delete();
        if (f3!=null) f3.delete();
        if (f4!=null) f4.delete();
        if (f5!=null) f5.delete();
        if (f6!=null) f6.delete();

        if(store1!=null) store1.delete();
        if(store2!=null) store2.delete();
        if(store3!=null) store3.delete();
    }


    ////////////////////////////////////////////////////////////////////////////////////
    // helper methods for file creation
    ////////////////////////////////////////////////////////////////////////////////////

    /**
     * Helper function to create a new temporary file of the given size,
     * with the given name, in the default shared directory.
     */
    protected File createNewNamedTestFile(int size, String name) throws Exception {
        return createNewNamedTestFile(size, name, _scratchDir);
    }

    protected File createNewNamedTestFile(int size, String name, File directory) throws Exception {
        return FileManagerTestUtils.createNewNamedTestFile(size, name, SHARE_EXTENSION, directory);
    }

    protected QueryRequest get_qr(String xml) {
        return queryRequestFactory.createQuery("", xml);
    }



    ////////////////////////////////////////////////////////////////////////////////////
    // additional helper methods
    ////////////////////////////////////////////////////////////////////////////////////

    //helper function to create queryrequest with I18N
    protected QueryRequest get_qr(File f) {
        QueryRequestFactory queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        String norm = I18NConvert.instance().getNorm(f.getName());
        norm = StringUtils.replace(norm, "_", " ");
        return queryRequestFactory.createQuery(norm);
    }

//    protected void addFilesToLibrary() throws Exception {
//        String dirString = "com/limegroup/gnutella";
//        File testDir = TestUtils.getResourceFile(dirString);
//        testDir = testDir.getCanonicalFile();
//        assertTrue("could not find the gnutella directory",
//            testDir.isDirectory());
//
//        File[] testFiles = testDir.listFiles(new FileFilter() {
//            public boolean accept(File file) {
//                // use files with a $ because they'll generally
//                // trigger a single-response return, which is
//                // easier to check
//                return !file.isDirectory() && file.getName().indexOf("$")!=-1;
//            }
//        });
//        assertNotNull("no files to test against", testFiles);
//        assertNotEquals("no files to test against", 0, testFiles.length);
//
//        for(int i=0; i<testFiles.length; i++) {
//            if(!testFiles[i].isFile()) continue;
//            File shared = new File(
//                _sharedDir, testFiles[i].getName() + "." + SHARE_EXTENSION);
//            assertTrue("unable to get file", FileUtils.copy( testFiles[i], shared));
//        }
//
//        waitForLoad();
//
//
//        // the below test depends on the filemanager loading shared files in
//        // alphabetical order, and listFiles returning them in alphabetical
//        // order since neither of these must be true, a length check can
//        // suffice instead.
//        //for(int i=0; i<files.length; i++)
//        //    assertEquals(files[i].getName()+".tmp",
//        //                 fman.get(i).getFile().getName());
//
//        assertEquals("unexpected number of shared files",
//            testFiles.length, fman.getGnutellaSharedFileList().size() );
//    }


    protected void waitForLoad() throws Exception {
        FileManagerTestUtils.waitForLoad(library, 10000);
    }

    protected boolean responsesContain(LimeXMLDocument... doc) {
        for (LimeXMLDocument myDoc : doc) {
            boolean cuurentDocExistsInResponses = false;
            for (Response response : responses) {
                LimeXMLDocument respDoc = response.getDocument();
                if ((respDoc != null) &&
                    (myDoc.getXMLString().equals(respDoc.getXMLString()))) {
                    cuurentDocExistsInResponses = true;
                }
            }
            if (!cuurentDocExistsInResponses) {
                return false;
            }
        }
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // classes and methods related to adding files to file manager
    ////////////////////////////////////////////////////////////////////////////////////

    public static class Listener implements EventListener<FileViewChangeEvent> {
        private final CountDownLatch latch = new CountDownLatch(1);
        public FileViewChangeEvent evt;
        public void handleEvent(FileViewChangeEvent fme) {
            evt = fme;
            latch.countDown();
            fme.getFileView().removeListener(this);
        }
        
        void await(long timeout) throws Exception {
            assertTrue(latch.await(timeout, TimeUnit.MILLISECONDS));
        }
    }
}
