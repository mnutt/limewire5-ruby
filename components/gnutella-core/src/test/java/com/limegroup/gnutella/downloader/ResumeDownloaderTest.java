package com.limegroup.gnutella.downloader;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.limewire.collection.Range;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.ConnectableImpl;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.DownloadManagerImpl;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.downloader.serial.DownloadSerializeSettings;
import com.limegroup.gnutella.downloader.serial.DownloadSerializer;
import com.limegroup.gnutella.downloader.serial.DownloadSerializerImpl;
import com.limegroup.gnutella.downloader.serial.OldDownloadConverter;

/** Unit tests small parts of ResumeDownloader. */
public class ResumeDownloaderTest extends LimeTestCase {

    private static final String name = "filename.txt";

    private static final int size = 1111;

    private static final int amountDownloaded = 500;

    private URN hash;

    private RemoteFileDesc rfd;

    private IncompleteFileManager ifm;

    private File incompleteFile;

    private Injector injector;

    public ResumeDownloaderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ResumeDownloaderTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        injector = LimeTestUtils.createInjector();
        
        hash = TestFile.hash();
        rfd = newRFD(name, size, hash);
        ifm = injector.getInstance(IncompleteFileManager.class);
        
        incompleteFile = ifm.getFile(rfd);
        VerifyingFile vf = injector.getInstance(VerifyingFileFactory.class).createVerifyingFile(size);
        vf.addInterval(Range.createRange(0, amountDownloaded - 1)); // inclusive
        ifm.addEntry(incompleteFile, vf, true);
        // make sure that we don't wait for network on re-query
        RequeryManager.NO_DELAY = true;

        DownloadManagerImpl dm = (DownloadManagerImpl) injector.getInstance(DownloadManager.class);
        dm.start();
    }
    
    /** Returns a new ResumeDownloader with stubbed-out DownloadManager, etc. */
    private ResumeDownloader newResumeDownloader() throws Exception {
        ResumeDownloader downloader = injector.getInstance(CoreDownloaderFactory.class).createResumeDownloader(
                incompleteFile, name, size);
        downloader.initialize();
        downloader.startDownload();
        return downloader;
    }

    private RemoteFileDesc newRFD(String name, int size, URN hash) throws Exception {
        Set<URN> urns = new HashSet<URN>(1);
        if (hash != null)
            urns.add(hash);
        return injector.getInstance(RemoteFileDescFactory.class).createRemoteFileDesc(new ConnectableImpl("1.2.3.4", 6346, false), 13l, name, size, new byte[16],
                56, 4, true, null, urns, false, "", -1);
    }

    public void testLoads32Bit() throws Exception {
        OldDownloadConverter oldDownloadConverter = injector.getInstance(OldDownloadConverter.class);
        File downloadDat = TestUtils.getResourceInPackage("resume_4.1.1-32bit-size.dat", ResumeDownloaderTest.class);
        List<DownloadMemento> mementos = oldDownloadConverter.readAndConvertOldDownloads(downloadDat);
        assertEquals(1, mementos.size());
        
        CoreDownloaderFactory coreDownloaderFactory = injector.getInstance(CoreDownloaderFactory.class);
        CoreDownloader downloader = coreDownloaderFactory.createFromMemento(mementos.get(0));
        assertEquals(1111, downloader.getContentLength());
        
        DownloadMemento memento = downloader.toMemento();
        File tmp = File.createTempFile("lwc", "save");
        tmp.delete();
        tmp.deleteOnExit();
        DownloadSerializeSettings downloadSerializeSettings = new DownloadSerialSettingsStub(tmp, tmp);
        DownloadSerializer serializer = new DownloadSerializerImpl(downloadSerializeSettings);
        serializer.writeToDisk(Collections.singletonList(memento));
        
        mementos = serializer.readFromDisk();
        coreDownloaderFactory = injector.getInstance(CoreDownloaderFactory.class);
        downloader = coreDownloaderFactory.createFromMemento(mementos.get(0));
        assertEquals(1111, downloader.getContentLength());
    }

    /**
     * Tests that the progress is not 0% while requerying. This issue was
     * reported by Sam Berlin.
     */
    public void testRequeryProgress() throws Exception {
        ResumeDownloader downloader = newResumeDownloader();
        DownloadTestUtils.waitForState(downloader, DownloadState.WAITING_FOR_USER);
        downloader.resume();
        DownloadTestUtils.strictWaitForState(downloader, DownloadState.WAITING_FOR_GNET_RESULTS, DownloadState.QUEUED, DownloadState.GAVE_UP);
        assertEquals(amountDownloaded, downloader.getAmountRead());
                
        DownloadMemento memento = downloader.toMemento();
        downloader.stop();
        
        // Verify that in a new instance of LW, things are still right.
        Injector newInjector = LimeTestUtils.createInjector();
        DownloadManagerImpl newDM = (DownloadManagerImpl)newInjector.getInstance(DownloadManager.class);
        CoreDownloader newDownloader = newDM.prepareMemento(memento);
        newDownloader.initialize();
        assertEquals(amountDownloaded, newDownloader.getAmountRead());
        newDownloader.startDownload();

        // Check same state as before serialization.
        DownloadTestUtils.waitForState(newDownloader, DownloadState.WAITING_FOR_USER);
        assertEquals(amountDownloaded, newDownloader.getAmountRead());
        newDownloader.stop();
    }

}
