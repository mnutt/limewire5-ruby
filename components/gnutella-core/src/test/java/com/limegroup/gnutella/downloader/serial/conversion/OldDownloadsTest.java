package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.gnutella.tests.ActivityCallbackStub;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.CommonUtils;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.downloader.DownloadSerialSettingsStub;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.downloader.serial.DownloadSerializeSettings;
import com.limegroup.gnutella.downloader.serial.DownloadSerializer;
import com.limegroup.gnutella.downloader.serial.DownloadSerializerImpl;
import com.limegroup.gnutella.downloader.serial.OldDownloadConverter;

/**
 * Tests backwards compatibility with old downloads.dat files.
 */
public class OldDownloadsTest extends org.limewire.gnutella.tests.LimeTestCase {
        
    private static final Log LOG = LogFactory.getLog(OldDownloadsTest.class);
    private Injector injector;
    
    public OldDownloadsTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        injector = LimeTestUtils.createInjector();
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return buildTestSuite(OldDownloadsTest.class);
    }
    
    public void testLime3x0() throws Exception {
        doTest("downloads_30.dat", "mpg4_golem160x90first120.avi", 2777638);
    }
    
    public void testLime4x12x7() throws Exception {
        doTest("downloads_4.12.7.dat", "LimeWireWin4.12.6.exe", 3064200);
    }
    
    private void doTest(String downloadName, String fileName, long length) throws Exception {
        final DownloadSerializer serializer = getSerializerFor(downloadName);
        final TestActivityCallback callback = new TestActivityCallback();
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(DownloadSerializer.class).toInstance(serializer);
                bind(ActivityCallback.class).toInstance(callback);
            }
        });
        DownloadManager manager = injector.getInstance(DownloadManager.class);
        manager.loadSavedDownloadsAndScheduleWriting();
        assertEquals("unexpected amount of downloaders added to gui", 1, callback.downloaders.size());
        ManagedDownloader md = (ManagedDownloader)callback.downloaders.get(0);
        assertEquals("unexpected filename", fileName, md.getSaveFile().getName());
        assertEquals("unexpected content length!", length, md.getContentLength());
    }

    private DownloadSerializer getSerializerFor(String file) throws Exception {
        LOG.debug("-Trying to read downloads.dat from \""+file+"\"");
        File downloadDat = TestUtils.getResourceInPackage(file, OldDownloadsTest.class);
        
        File oldSaveLocation = File.createTempFile("lwc", "oldSaveFile");
        CommonUtils.copyFile(downloadDat, oldSaveLocation);
        
        File newSaveLocation = File.createTempFile("lwc", "newSaveFile");
        newSaveLocation.delete();
        
        DownloadSerializeSettings oldSettings = new DownloadSerialSettingsStub(oldSaveLocation, oldSaveLocation);
        DownloadSerializeSettings newSettings = new DownloadSerialSettingsStub(newSaveLocation, newSaveLocation);
        DownloadSerializer downloadSerializer = new DownloadSerializerImpl(newSettings);
        
        DownloadUpgradeTask downloadUpgradeTask = new DownloadUpgradeTask(injector.getInstance(OldDownloadConverter.class), oldSettings, newSettings, downloadSerializer); 
        downloadUpgradeTask.upgrade();
        
        return downloadSerializer;
    }
            
    private static class TestActivityCallback extends ActivityCallbackStub {
        private List<Downloader> downloaders=new LinkedList<Downloader>();
    
        @Override
        public void addDownload(Downloader d) {
            downloaders.add(d);
        }
    
        @Override
        public void removeDownload(Downloader d) {
            downloaders.remove(d);
        }
        
        public void clearDownloaders() {
            downloaders.clear();
        }
    }
}
