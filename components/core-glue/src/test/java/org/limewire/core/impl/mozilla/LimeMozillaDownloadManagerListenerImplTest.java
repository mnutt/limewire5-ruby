package org.limewire.core.impl.mozilla;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.BaseTestCase;
import org.limewire.util.ExecuteRunnableAction;
import org.mozilla.interfaces.nsIDownload;
import org.mozilla.interfaces.nsIDownloadManager;
import org.mozilla.interfaces.nsILocalFile;
import org.mozilla.interfaces.nsISimpleEnumerator;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.mozilla.MozillaDownload;

public class LimeMozillaDownloadManagerListenerImplTest extends BaseTestCase {

    public LimeMozillaDownloadManagerListenerImplTest(String name) {
        super(name);
    }

    public void testResumeDownloads() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final ScheduledExecutorService scheduledExecutorService = context
                .mock(ScheduledExecutorService.class);
        final DownloadManager downloadManager = context.mock(DownloadManager.class);

        final XPComUtility xComUtility = context.mock(XPComUtility.class);

        final nsIDownloadManager nsIDownloadManager = context.mock(nsIDownloadManager.class);

        final nsISimpleEnumerator nsISimpleEnumerator = context.mock(nsISimpleEnumerator.class);

        final nsIDownload nsIDownload = context.mock(nsIDownload.class);

        final long downloadID = 1;
        
        context.checking(new Expectations() {
            {
                allowing(xComUtility).getServiceProxy(
                        LimeMozillaDownloadManagerListenerImpl.NS_IDOWNLOADMANAGER_CID,
                        nsIDownloadManager.class);
                will(returnValue(nsIDownloadManager));

                one(nsIDownloadManager).getActiveDownloads();
                will(returnValue(nsISimpleEnumerator));
                one(nsISimpleEnumerator).hasMoreElements();
                will(returnValue(true));
                one(nsISimpleEnumerator).getNext();
                will(returnValue(nsIDownload));
                one(xComUtility).proxy(nsIDownload, nsIDownload.class);
                will(returnValue(nsIDownload));
                one(nsIDownload).getId();
                will(returnValue(downloadID));
                one(nsIDownloadManager).resumeDownload(downloadID);
                one(nsISimpleEnumerator).hasMoreElements();
                will(returnValue(false));
            }
        });
        LimeMozillaDownloadManagerListenerImpl limeMozillaDownloadManagerListenerImpl = new LimeMozillaDownloadManagerListenerImpl(
                scheduledExecutorService, downloadManager, xComUtility);

        limeMozillaDownloadManagerListenerImpl.resumeDownloads();

        context.assertIsSatisfied();
    }
    
    public void testAddMissingDownloads() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final ScheduledExecutorService scheduledExecutorService = context
                .mock(ScheduledExecutorService.class);
        final DownloadManager downloadManager = context.mock(DownloadManager.class);

        final XPComUtility xComUtility = context.mock(XPComUtility.class);

        final nsIDownloadManager nsIDownloadManager = context.mock(nsIDownloadManager.class);

        final nsISimpleEnumerator nsISimpleEnumerator = context.mock(nsISimpleEnumerator.class);

        final nsIDownload nsIDownload = context.mock(nsIDownload.class);

        
        final long downloadID = 1;
        final long downloadSize = 111;
        final CoreDownloader downloader = context.mock(CoreDownloader.class);
        final nsILocalFile targetFile = context.mock(nsILocalFile.class);
        final AtomicReference<LimeMozillaDownloadProgressListenerImpl> progressListener = new AtomicReference<LimeMozillaDownloadProgressListenerImpl>();
        context.checking(new Expectations() {
            {
                allowing(xComUtility).getServiceProxy(
                        LimeMozillaDownloadManagerListenerImpl.NS_IDOWNLOADMANAGER_CID,
                        nsIDownloadManager.class);
                will(returnValue(nsIDownloadManager));

                one(nsIDownloadManager).getActiveDownloads();
                will(returnValue(nsISimpleEnumerator));
                one(nsISimpleEnumerator).hasMoreElements();
                will(returnValue(true));
                one(nsISimpleEnumerator).getNext();
                will(returnValue(nsIDownload));
                one(xComUtility).proxy(nsIDownload, nsIDownload.class);
                will(returnValue(nsIDownload));
                allowing(nsIDownload).getId();
                will(returnValue(downloadID));
                one(nsISimpleEnumerator).hasMoreElements();
                will(returnValue(false));
                one(nsIDownload).getSize();
                will(returnValue(downloadSize));
                one(nsIDownload).getTargetFile();
                will(returnValue(targetFile));
                one(targetFile).getPath();
                will(returnValue("/tmp/somefile"));
                one(nsIDownloadManager).addListener(with(any(LimeMozillaDownloadProgressListenerImpl.class)));
                will(new AssignParameterAction<LimeMozillaDownloadProgressListenerImpl>(progressListener, 0));
                one(downloadManager).downloadFromMozilla(with(any(MozillaDownload.class)));
                will(returnValue(downloader));
                one(scheduledExecutorService).execute(with(any(Runnable.class)));
                will(new ExecuteRunnableAction());
            }
        });
        LimeMozillaDownloadManagerListenerImpl limeMozillaDownloadManagerListenerImpl = new LimeMozillaDownloadManagerListenerImpl(
                scheduledExecutorService, downloadManager, xComUtility);

        limeMozillaDownloadManagerListenerImpl.addMissingDownloads();
        
        assertNotNull(progressListener.get());
        assertEquals(downloadID, progressListener.get().getDownloadId());

        context.assertIsSatisfied();
    }
}
