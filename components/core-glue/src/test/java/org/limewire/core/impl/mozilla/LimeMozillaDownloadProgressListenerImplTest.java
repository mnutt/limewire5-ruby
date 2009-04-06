package org.limewire.core.impl.mozilla;

import java.util.concurrent.ScheduledExecutorService;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.util.BaseTestCase;
import org.limewire.util.ExecuteRunnableAction;
import org.mozilla.interfaces.nsIDownload;
import org.mozilla.interfaces.nsIDownloadManager;
import org.mozilla.interfaces.nsILocalFile;

import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.downloader.CoreDownloader;

public class LimeMozillaDownloadProgressListenerImplTest extends BaseTestCase {

    public LimeMozillaDownloadProgressListenerImplTest(String name) {
        super(name);
    }

    public void testInitialization() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        final nsIDownload download = context.mock(nsIDownload.class);
        final long downloadId = 1;
        final long downloadSize = 1234;
        final nsILocalFile targetFile = context.mock(nsILocalFile.class);
        final LimeMozillaDownloadManagerListenerImpl downloadManager = context
                .mock(LimeMozillaDownloadManagerListenerImpl.class);
        final ScheduledExecutorService executorService = context
                .mock(ScheduledExecutorService.class);
        final CoreDownloader coreDownloader = context.mock(CoreDownloader.class);
        final XPComUtility xpComUtility = context.mock(XPComUtility.class);

        context.checking(new Expectations() {
            {
                allowing(download).getId();
                will(returnValue(downloadId));
                one(download).getSize();
                will(returnValue(downloadSize));
                one(download).getTargetFile();
                will(returnValue(targetFile));
                one(targetFile).getPath();
                will(returnValue("/tmp/somefile"));
                one(executorService).execute(with(any(Runnable.class)));
                will(new ExecuteRunnableAction());
            }
        });
        LimeMozillaDownloadProgressListenerImpl limeMozillaDownloadProgressListenerImpl = new LimeMozillaDownloadProgressListenerImpl(
                downloadManager, executorService, download, xpComUtility);

        limeMozillaDownloadProgressListenerImpl.init(coreDownloader,
                nsIDownloadManager.DOWNLOAD_QUEUED);

        assertEquals(downloadId, limeMozillaDownloadProgressListenerImpl.getDownloadId());
        
        context.assertIsSatisfied();
    }

    public void testOnStateChange() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        final nsIDownload download = context.mock(nsIDownload.class);
        final long downloadId = 1;
        final long downloadSize = 1234;
        final nsILocalFile targetFile = context.mock(nsILocalFile.class);
        final LimeMozillaDownloadManagerListenerImpl downloadManager = context
                .mock(LimeMozillaDownloadManagerListenerImpl.class);
        final ScheduledExecutorService executorService = context
                .mock(ScheduledExecutorService.class);
        final CoreDownloader coreDownloader = context.mock(CoreDownloader.class);
        final XPComUtility xpComUtility = context.mock(XPComUtility.class);

        context.checking(new Expectations() {
            {
                allowing(download).getId();
                will(returnValue(downloadId));
                allowing(download).getSize();
                will(returnValue(downloadSize));
                one(download).getTargetFile();
                will(returnValue(targetFile));
                one(targetFile).getPath();
                will(returnValue("/tmp/somefile"));
                allowing(executorService).execute(with(any(Runnable.class)));
                will(new ExecuteRunnableAction());
            }
        });
        LimeMozillaDownloadProgressListenerImpl limeMozillaDownloadProgressListenerImpl = new LimeMozillaDownloadProgressListenerImpl(
                downloadManager, executorService, download, xpComUtility);

        limeMozillaDownloadProgressListenerImpl.init(coreDownloader,
                nsIDownloadManager.DOWNLOAD_QUEUED);

        assertTrue(limeMozillaDownloadProgressListenerImpl.isQueued());
        assertTrue(limeMozillaDownloadProgressListenerImpl.isInactive());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCancelled());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCompleted());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isPaused());

        assertEquals(DownloadState.QUEUED, limeMozillaDownloadProgressListenerImpl
                .getDownloadStatus());

        context.checking(new Expectations() {
            {
                one(download).getState();
                will(returnValue(nsIDownloadManager.DOWNLOAD_PAUSED));
            }
        });

        limeMozillaDownloadProgressListenerImpl.onStateChange(null, null, -1, -1, download);

        assertTrue(limeMozillaDownloadProgressListenerImpl.isPaused());
        assertTrue(limeMozillaDownloadProgressListenerImpl.isInactive());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCancelled());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCompleted());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isQueued());

        assertEquals(DownloadState.PAUSED, limeMozillaDownloadProgressListenerImpl
                .getDownloadStatus());

        context.checking(new Expectations() {
            {
                one(download).getState();
                will(returnValue(nsIDownloadManager.DOWNLOAD_CANCELED));
            }
        });

        limeMozillaDownloadProgressListenerImpl.onStateChange(null, null, -1, -1, download);

        assertTrue(limeMozillaDownloadProgressListenerImpl.isInactive());
        assertTrue(limeMozillaDownloadProgressListenerImpl.isCancelled());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isPaused());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCompleted());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isQueued());

        assertEquals(DownloadState.ABORTED, limeMozillaDownloadProgressListenerImpl
                .getDownloadStatus());

        context.checking(new Expectations() {
            {
                one(download).getState();
                will(returnValue(nsIDownloadManager.DOWNLOAD_FAILED));
            }
        });

        limeMozillaDownloadProgressListenerImpl.onStateChange(null, null, -1, -1, download);

        assertTrue(limeMozillaDownloadProgressListenerImpl.isInactive());
        assertTrue(limeMozillaDownloadProgressListenerImpl.isCancelled());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isPaused());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCompleted());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isQueued());

        assertEquals(DownloadState.INVALID, limeMozillaDownloadProgressListenerImpl
                .getDownloadStatus());

        context.checking(new Expectations() {
            {
                one(download).getState();
                will(returnValue(nsIDownloadManager.DOWNLOAD_BLOCKED_PARENTAL));
            }
        });

        limeMozillaDownloadProgressListenerImpl.onStateChange(null, null, -1, -1, download);

        assertTrue(limeMozillaDownloadProgressListenerImpl.isInactive());
        assertTrue(limeMozillaDownloadProgressListenerImpl.isCancelled());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isPaused());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCompleted());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isQueued());

        assertEquals(DownloadState.INVALID, limeMozillaDownloadProgressListenerImpl
                .getDownloadStatus());

        context.checking(new Expectations() {
            {
                one(download).getState();
                will(returnValue(nsIDownloadManager.DOWNLOAD_BLOCKED_POLICY));
            }
        });

        limeMozillaDownloadProgressListenerImpl.onStateChange(null, null, -1, -1, download);

        assertTrue(limeMozillaDownloadProgressListenerImpl.isInactive());
        assertTrue(limeMozillaDownloadProgressListenerImpl.isCancelled());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isPaused());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCompleted());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isQueued());

        assertEquals(DownloadState.INVALID, limeMozillaDownloadProgressListenerImpl
                .getDownloadStatus());

        context.checking(new Expectations() {
            {
                one(download).getState();
                will(returnValue(nsIDownloadManager.DOWNLOAD_DIRTY));
            }
        });

        limeMozillaDownloadProgressListenerImpl.onStateChange(null, null, -1, -1, download);

        assertTrue(limeMozillaDownloadProgressListenerImpl.isInactive());
        assertTrue(limeMozillaDownloadProgressListenerImpl.isCancelled());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isPaused());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCompleted());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isQueued());

        assertEquals(DownloadState.INVALID, limeMozillaDownloadProgressListenerImpl
                .getDownloadStatus());

        context.checking(new Expectations() {
            {
                one(download).getState();
                will(returnValue(nsIDownloadManager.DOWNLOAD_FAILED));
            }
        });

        limeMozillaDownloadProgressListenerImpl.onStateChange(null, null, -1, -1, download);

        assertTrue(limeMozillaDownloadProgressListenerImpl.isInactive());
        assertTrue(limeMozillaDownloadProgressListenerImpl.isCancelled());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isPaused());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCompleted());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isQueued());

        assertEquals(DownloadState.INVALID, limeMozillaDownloadProgressListenerImpl
                .getDownloadStatus());

        context.checking(new Expectations() {
            {
                one(download).getState();
                will(returnValue(nsIDownloadManager.DOWNLOAD_DOWNLOADING));
            }
        });

        limeMozillaDownloadProgressListenerImpl.onStateChange(null, null, -1, -1, download);

        assertFalse(limeMozillaDownloadProgressListenerImpl.isInactive());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCancelled());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isPaused());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCompleted());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isQueued());

        assertEquals(DownloadState.DOWNLOADING, limeMozillaDownloadProgressListenerImpl
                .getDownloadStatus());

        context.checking(new Expectations() {
            {
                one(download).getState();
                will(returnValue(nsIDownloadManager.DOWNLOAD_NOTSTARTED));
            }
        });

        limeMozillaDownloadProgressListenerImpl.onStateChange(null, null, -1, -1, download);

        assertTrue(limeMozillaDownloadProgressListenerImpl.isInactive());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCancelled());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isPaused());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCompleted());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isQueued());

        assertEquals(DownloadState.PAUSED, limeMozillaDownloadProgressListenerImpl
                .getDownloadStatus());
        
        
        context.checking(new Expectations() {
            {
                one(download).getState();
                will(returnValue(nsIDownloadManager.DOWNLOAD_SCANNING));
            }
        });

        limeMozillaDownloadProgressListenerImpl.onStateChange(null, null, -1, -1, download);

        assertFalse(limeMozillaDownloadProgressListenerImpl.isInactive());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCancelled());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isPaused());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCompleted());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isQueued());

        assertEquals(DownloadState.RESUMING, limeMozillaDownloadProgressListenerImpl
                .getDownloadStatus());
        
        context.checking(new Expectations() {
            {
                one(download).getState();
                will(returnValue(nsIDownloadManager.DOWNLOAD_FINISHED));
            }
        });

        limeMozillaDownloadProgressListenerImpl.onStateChange(null, null, -1, -1, download);

        assertTrue(limeMozillaDownloadProgressListenerImpl.isCompleted());
        assertTrue(limeMozillaDownloadProgressListenerImpl.isInactive());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCancelled());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isPaused());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isQueued());

        assertEquals(DownloadState.COMPLETE, limeMozillaDownloadProgressListenerImpl
                .getDownloadStatus());

        assertEquals(downloadSize, limeMozillaDownloadProgressListenerImpl.getAmountDownloaded());
        assertEquals(0, limeMozillaDownloadProgressListenerImpl.getAmountPending());
        assertEquals(downloadSize, limeMozillaDownloadProgressListenerImpl.getContentLength());

        
        limeMozillaDownloadProgressListenerImpl.setDiskError();

        assertTrue(limeMozillaDownloadProgressListenerImpl.isInactive());
        assertTrue(limeMozillaDownloadProgressListenerImpl.isCancelled());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCompleted());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isPaused());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isQueued());

        assertEquals(DownloadState.INVALID, limeMozillaDownloadProgressListenerImpl
                .getDownloadStatus());
        
        context.assertIsSatisfied();
    }
    
    public void testOnProgressChange() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        final nsIDownload download = context.mock(nsIDownload.class);
        final long downloadId = 1;
        final long downloadSize = 1234;
        final nsILocalFile targetFile = context.mock(nsILocalFile.class);
        final LimeMozillaDownloadManagerListenerImpl downloadManager = context
                .mock(LimeMozillaDownloadManagerListenerImpl.class);
        final ScheduledExecutorService executorService = context
                .mock(ScheduledExecutorService.class);
        final CoreDownloader coreDownloader = context.mock(CoreDownloader.class);
        final XPComUtility xpComUtility = context.mock(XPComUtility.class);

        context.checking(new Expectations() {
            {
                allowing(download).getId();
                will(returnValue(downloadId));
                allowing(download).getSize();
                will(returnValue(downloadSize));
                one(download).getTargetFile();
                will(returnValue(targetFile));
                one(targetFile).getPath();
                will(returnValue("/tmp/somefile"));
                allowing(executorService).execute(with(any(Runnable.class)));
                will(new ExecuteRunnableAction());
            }
        });
        LimeMozillaDownloadProgressListenerImpl limeMozillaDownloadProgressListenerImpl = new LimeMozillaDownloadProgressListenerImpl(
                downloadManager, executorService, download, xpComUtility);

        limeMozillaDownloadProgressListenerImpl.init(coreDownloader,
                nsIDownloadManager.DOWNLOAD_QUEUED);

        assertTrue(limeMozillaDownloadProgressListenerImpl.isQueued());
        assertTrue(limeMozillaDownloadProgressListenerImpl.isInactive());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCancelled());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCompleted());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isPaused());

        assertEquals(DownloadState.QUEUED, limeMozillaDownloadProgressListenerImpl
                .getDownloadStatus());

        assertEquals(0, limeMozillaDownloadProgressListenerImpl.getAmountDownloaded());
        assertEquals(downloadSize, limeMozillaDownloadProgressListenerImpl.getAmountPending());
        assertEquals(downloadSize, limeMozillaDownloadProgressListenerImpl.getContentLength());
        
        context.checking(new Expectations() {
            {
                allowing(download).getState();
                will(returnValue(nsIDownloadManager.DOWNLOAD_DOWNLOADING));
            }
        });
        
        limeMozillaDownloadProgressListenerImpl.onProgressChange(null, null, -1, -1, 10, -1, download);
        
        assertFalse(limeMozillaDownloadProgressListenerImpl.isQueued());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isInactive());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCancelled());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCompleted());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isPaused());

        assertEquals(DownloadState.DOWNLOADING, limeMozillaDownloadProgressListenerImpl
                .getDownloadStatus());

        assertEquals(10, limeMozillaDownloadProgressListenerImpl.getAmountDownloaded());
        assertEquals(downloadSize - 10, limeMozillaDownloadProgressListenerImpl.getAmountPending());
        assertEquals(downloadSize, limeMozillaDownloadProgressListenerImpl.getContentLength());
        
        
        limeMozillaDownloadProgressListenerImpl.onProgressChange(null, null, -1, -1, 510, -1, download);
        
        assertFalse(limeMozillaDownloadProgressListenerImpl.isQueued());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isInactive());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCancelled());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isCompleted());
        assertFalse(limeMozillaDownloadProgressListenerImpl.isPaused());

        assertEquals(DownloadState.DOWNLOADING, limeMozillaDownloadProgressListenerImpl
                .getDownloadStatus());

        assertEquals(510, limeMozillaDownloadProgressListenerImpl.getAmountDownloaded());
        assertEquals(downloadSize - 10 - 500, limeMozillaDownloadProgressListenerImpl.getAmountPending());
        assertEquals(downloadSize, limeMozillaDownloadProgressListenerImpl.getContentLength());
        
        context.assertIsSatisfied();
    }
}
