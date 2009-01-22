package com.limegroup.gnutella;

import java.io.File;
import java.util.UUID;

import org.jmock.Mockery;
import org.limewire.core.api.download.SaveLocationException;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.downloader.CantResumeException;
import com.limegroup.gnutella.util.LimeTestCase;

public class DownloadManagerImplTest extends LimeTestCase {

    public DownloadManagerImplTest(String name) {
        super(name);
    }

    public void testContains() throws SaveLocationException, CantResumeException {
        DownloadManager downloadManager = createDownloadManager();
        Downloader downloader = downloadManager.download(new File("T-5-"+UUID.randomUUID().toString()));
        assertTrue(downloadManager.contains(downloader));
        
        Downloader dummyDownloader = createDummyDownloader();
        assertFalse(downloadManager.contains(dummyDownloader));
        downloader.stop(true);
    }

    private Downloader createDummyDownloader() {
        Mockery mockery = new Mockery();
        Downloader downloader = mockery.mock(Downloader.class);
        return downloader;
    }

    private DownloadManager createDownloadManager() {
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new LimeWireCoreModule(
                ActivityCallbackAdapter.class));
        DownloadManager downloadManager = injector.getInstance(DownloadManager.class);
        downloadManager.start();
        return downloadManager;
    }
}
