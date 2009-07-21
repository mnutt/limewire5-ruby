package org.limewire.core.impl.download;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.impl.download.listener.ItunesDownloadListener;
import org.limewire.core.impl.download.listener.ItunesDownloadListenerFactory;
import org.limewire.core.impl.download.listener.TorrentDownloadListener;
import org.limewire.core.impl.download.listener.TorrentDownloadListenerFactory;
import org.limewire.core.impl.magnet.MagnetLinkImpl;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.friend.api.FriendManager;
import org.limewire.io.Address;
import org.limewire.io.IpPort;
import org.limewire.lifecycle.ServiceScheduler;
import org.limewire.listener.EventListener;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.BaseTestCase;
import org.limewire.util.TestPropertyChangeListener;

import ca.odell.glazedlists.EventList;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;

public class CoreDownloadListManagerTest extends BaseTestCase {

    public CoreDownloadListManagerTest(String name) {
        super(name);
    }

    /**
     * Tests that downloadRemove events do not remove the download item from the
     * core download list manager list.
     */
    @SuppressWarnings("unchecked")
    public void testCompletedDownloadsNotRemovedFromCoreDownloadManagerListUntilCleared()
            throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final DownloadListenerList listenerList = context.mock(DownloadListenerList.class);
        final RemoteFileDescFactory remoteFileDescFactory = context
                .mock(RemoteFileDescFactory.class);
        final SpamManager spamManager = context.mock(SpamManager.class);
        final ItunesDownloadListenerFactory itunesDownloadListenerFactory = context
                .mock(ItunesDownloadListenerFactory.class);
        final FriendManager friendManager = context.mock(FriendManager.class);
        final TorrentDownloadListenerFactory torrentDownloadListenerFactory = context
        .mock(TorrentDownloadListenerFactory.class);
        
        final AtomicReference<DownloadListener> downloadListener = new AtomicReference<DownloadListener>();

        context.checking(new Expectations() {
            {
                one(listenerList).addDownloadListener(with(any(DownloadListener.class)));
                will(new AssignParameterAction<DownloadListener>(downloadListener, 0));
            }
        });

        CoreDownloadListManager coreDownloadListManager = new CoreDownloadListManager(
                downloadManager, remoteFileDescFactory, spamManager,
                itunesDownloadListenerFactory, friendManager, torrentDownloadListenerFactory);

        final ScheduledExecutorService backgroundExecutor = context
                .mock(ScheduledExecutorService.class);
        final ServiceScheduler scheduler = context.mock(ServiceScheduler.class);
        context.checking(new Expectations() {
            {
                one(scheduler).scheduleAtFixedRate(with(any(String.class)),
                        with(any(Runnable.class)), with(any(long.class)), with(any(long.class)),
                        with(any(TimeUnit.class)), with(same(backgroundExecutor)));
            }
        });

        coreDownloadListManager.registerDownloadListener(listenerList);
        coreDownloadListManager.registerService(scheduler, backgroundExecutor);

        final Downloader unfinishedItem1 = context.mock(Downloader.class);
        final Downloader unfinishedItem2 = context.mock(Downloader.class);
        final Downloader finishedItem1 = context.mock(Downloader.class);
        final Downloader finishedItem2 = context.mock(Downloader.class);

        final URN urn1 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1");
        final URN urn2 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA2");
        final URN urn3 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA3");
        final URN urn4 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA4");

        final ItunesDownloadListener itunesDownloadListener = context
                .mock(ItunesDownloadListener.class);
        
        final TorrentDownloadListener torrentDownloadListener = context
        .mock(TorrentDownloadListener.class);

        final AtomicReference<DownloadItem> downloadItem1 = new AtomicReference<DownloadItem>();
        final AtomicReference<DownloadItem> downloadItem2 = new AtomicReference<DownloadItem>();
        final AtomicReference<DownloadItem> downloadItem3 = new AtomicReference<DownloadItem>();
        final AtomicReference<DownloadItem> downloadItem4 = new AtomicReference<DownloadItem>();

        context.checking(new Expectations() {
            {
                allowing(itunesDownloadListenerFactory).createListener(with(any(Downloader.class)));
                will(returnValue(itunesDownloadListener));
                
                allowing(torrentDownloadListenerFactory).createListener(with(any(Downloader.class)), with(any(List.class)));
                will(returnValue(torrentDownloadListener));

                allowing(unfinishedItem1).getState();
                will(returnValue(Downloader.DownloadState.DOWNLOADING));
                allowing(unfinishedItem2).getState();
                will(returnValue(Downloader.DownloadState.CONNECTING));
                allowing(finishedItem1).getState();
                will(returnValue(Downloader.DownloadState.COMPLETE));
                allowing(finishedItem2).getState();
                will(returnValue(Downloader.DownloadState.COMPLETE));

                allowing(unfinishedItem1).addListener(with(any(EventListener.class)));
                allowing(unfinishedItem2).addListener(with(any(EventListener.class)));
                allowing(finishedItem1).addListener(with(any(EventListener.class)));
                allowing(finishedItem2).addListener(with(any(EventListener.class)));

                allowing(unfinishedItem1).setAttribute(with(equal(DownloadItem.DOWNLOAD_ITEM)),
                        with(any(DownloadItem.class)), with(equal(false)));
                will(new AssignParameterAction<DownloadItem>(downloadItem1, 1));
                allowing(unfinishedItem2).setAttribute(with(equal(DownloadItem.DOWNLOAD_ITEM)),
                        with(any(DownloadItem.class)), with(equal(false)));
                will(new AssignParameterAction<DownloadItem>(downloadItem2, 1));
                allowing(finishedItem1).setAttribute(with(equal(DownloadItem.DOWNLOAD_ITEM)),
                        with(any(DownloadItem.class)), with(equal(false)));
                will(new AssignParameterAction<DownloadItem>(downloadItem3, 1));
                allowing(finishedItem2).setAttribute(with(equal(DownloadItem.DOWNLOAD_ITEM)),
                        with(any(DownloadItem.class)), with(equal(false)));
                will(new AssignParameterAction<DownloadItem>(downloadItem4, 1));
                
                allowing(unfinishedItem1).getAttribute(with(equal(DownloadItem.DOWNLOAD_START_DATE)));
                will(returnValue(null));
                allowing(unfinishedItem2).getAttribute(with(equal(DownloadItem.DOWNLOAD_START_DATE)));
                will(returnValue(null));
                allowing(finishedItem1).getAttribute(with(equal(DownloadItem.DOWNLOAD_START_DATE)));
                will(returnValue(null));
                allowing(finishedItem2).getAttribute(with(equal(DownloadItem.DOWNLOAD_START_DATE)));
                will(returnValue(null));                

                allowing(unfinishedItem1).setAttribute(with(equal(DownloadItem.DOWNLOAD_START_DATE)), 
                        with(any(Date.class)), with(equal(true)));
                allowing(unfinishedItem2).setAttribute(with(equal(DownloadItem.DOWNLOAD_START_DATE)), 
                        with(any(Date.class)), with(equal(true)));
                allowing(finishedItem1).setAttribute(with(equal(DownloadItem.DOWNLOAD_START_DATE)), 
                        with(any(Date.class)), with(equal(true)));
                allowing(finishedItem2).setAttribute(with(equal(DownloadItem.DOWNLOAD_START_DATE)), 
                        with(any(Date.class)), with(equal(true)));

                allowing(unfinishedItem1).getSha1Urn();
                will(returnValue(urn1));
                allowing(unfinishedItem2).getSha1Urn();
                will(returnValue(urn2));
                allowing(finishedItem1).getSha1Urn();
                will(returnValue(urn3));
                allowing(finishedItem2).getSha1Urn();
                will(returnValue(urn4));
            }
        });

        EventList<DownloadItem> downloads = coreDownloadListManager.getDownloads();

        downloadListener.get().downloadAdded(unfinishedItem1);
        downloadListener.get().downloadAdded(unfinishedItem2);
        downloadListener.get().downloadAdded(finishedItem1);
        downloadListener.get().downloadAdded(finishedItem2);

        assertEquals(4, downloads.size());

        assertTrue(coreDownloadListManager.contains(urn1));
        assertTrue(coreDownloadListManager.contains(urn2));
        assertTrue(coreDownloadListManager.contains(urn3));
        assertTrue(coreDownloadListManager.contains(urn4));

        assertNotNull(coreDownloadListManager.getDownloadItem(urn1));
        assertNotNull(coreDownloadListManager.getDownloadItem(urn2));
        assertNotNull(coreDownloadListManager.getDownloadItem(urn3));
        assertNotNull(coreDownloadListManager.getDownloadItem(urn4));

        context.checking(new Expectations() {
            {
                allowing(unfinishedItem1).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem1.get()));
                allowing(unfinishedItem2).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem2.get()));
                allowing(finishedItem1).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem3.get()));
                allowing(finishedItem2).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem4.get()));
            }
        });
        downloadListener.get().downloadRemoved(finishedItem1);
        downloadListener.get().downloadRemoved(finishedItem1);

        assertEquals(4, downloads.size());

        assertTrue(coreDownloadListManager.contains(urn1));
        assertTrue(coreDownloadListManager.contains(urn2));
        assertTrue(coreDownloadListManager.contains(urn3));
        assertTrue(coreDownloadListManager.contains(urn4));

        assertNotNull(coreDownloadListManager.getDownloadItem(urn1));
        assertNotNull(coreDownloadListManager.getDownloadItem(urn2));
        assertNotNull(coreDownloadListManager.getDownloadItem(urn3));
        assertNotNull(coreDownloadListManager.getDownloadItem(urn4));

        coreDownloadListManager.clearFinished();

        assertEquals(2, downloads.size());
        assertTrue(coreDownloadListManager.contains(urn1));
        assertTrue(coreDownloadListManager.contains(urn2));

        assertFalse(coreDownloadListManager.contains(urn3));
        assertFalse(coreDownloadListManager.contains(urn4));

        assertNotNull(coreDownloadListManager.getDownloadItem(urn1));
        assertNotNull(coreDownloadListManager.getDownloadItem(urn2));
        assertNull(coreDownloadListManager.getDownloadItem(urn3));
        assertNull(coreDownloadListManager.getDownloadItem(urn4));

        context.assertIsSatisfied();
    }

    /**
     * Tests that completed downloads are removed from the
     * CoreDownloadListManager when the clearFinished method is called.
     */
    @SuppressWarnings("unchecked")
    public void testClearFinished() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final DownloadListenerList listenerList = context.mock(DownloadListenerList.class);
        final RemoteFileDescFactory remoteFileDescFactory = context
                .mock(RemoteFileDescFactory.class);
        final SpamManager spamManager = context.mock(SpamManager.class);
        final ItunesDownloadListenerFactory itunesDownloadListenerFactory = context
                .mock(ItunesDownloadListenerFactory.class);
        final FriendManager friendManager = context.mock(FriendManager.class);
        final TorrentDownloadListenerFactory torrentDownloadListenerFactory = context
        .mock(TorrentDownloadListenerFactory.class);

        final AtomicReference<DownloadListener> downloadListener = new AtomicReference<DownloadListener>();

        context.checking(new Expectations() {
            {
                one(listenerList).addDownloadListener(with(any(DownloadListener.class)));
                will(new AssignParameterAction<DownloadListener>(downloadListener, 0));
            }
        });

        CoreDownloadListManager coreDownloadListManager = new CoreDownloadListManager(
                downloadManager, remoteFileDescFactory, spamManager,
                itunesDownloadListenerFactory, friendManager, torrentDownloadListenerFactory);

        coreDownloadListManager.registerDownloadListener(listenerList);

        final Downloader unfinishedItem1 = context.mock(Downloader.class);
        final Downloader unfinishedItem2 = context.mock(Downloader.class);
        final Downloader finishedItem1 = context.mock(Downloader.class);
        final Downloader finishedItem2 = context.mock(Downloader.class);

        final URN urn1 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1");
        final URN urn2 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA2");
        final URN urn3 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA3");
        final URN urn4 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA4");

        final ItunesDownloadListener itunesDownloadListener = context
                .mock(ItunesDownloadListener.class);
        
        final TorrentDownloadListener torrentDownloadListener = context
        .mock(TorrentDownloadListener.class);
        
        context.checking(new Expectations() {
            {
                allowing(itunesDownloadListenerFactory).createListener(with(any(Downloader.class)));
                will(returnValue(itunesDownloadListener));
                
                allowing(torrentDownloadListenerFactory).createListener(with(any(Downloader.class)), with(any(List.class)));
                will(returnValue(torrentDownloadListener));

                allowing(unfinishedItem1).getState();
                will(returnValue(Downloader.DownloadState.DOWNLOADING));
                allowing(unfinishedItem2).getState();
                will(returnValue(Downloader.DownloadState.CONNECTING));
                allowing(finishedItem1).getState();
                will(returnValue(Downloader.DownloadState.COMPLETE));
                allowing(finishedItem2).getState();
                will(returnValue(Downloader.DownloadState.COMPLETE));                

                allowing(unfinishedItem1).getAttribute(with(equal(DownloadItem.DOWNLOAD_START_DATE)));
                will(returnValue(null));
                allowing(unfinishedItem2).getAttribute(with(equal(DownloadItem.DOWNLOAD_START_DATE)));
                will(returnValue(null));
                allowing(finishedItem1).getAttribute(with(equal(DownloadItem.DOWNLOAD_START_DATE)));
                will(returnValue(null));
                allowing(finishedItem2).getAttribute(with(equal(DownloadItem.DOWNLOAD_START_DATE)));
                will(returnValue(null));
                

                allowing(unfinishedItem1).setAttribute(with(equal(DownloadItem.DOWNLOAD_START_DATE)), 
                        with(any(Date.class)), with(equal(true)));
                allowing(unfinishedItem2).setAttribute(with(equal(DownloadItem.DOWNLOAD_START_DATE)), 
                        with(any(Date.class)), with(equal(true)));
                allowing(finishedItem1).setAttribute(with(equal(DownloadItem.DOWNLOAD_START_DATE)), 
                        with(any(Date.class)), with(equal(true)));
                allowing(finishedItem2).setAttribute(with(equal(DownloadItem.DOWNLOAD_START_DATE)), 
                        with(any(Date.class)), with(equal(true)));

                allowing(unfinishedItem1).addListener(with(any(EventListener.class)));
                allowing(unfinishedItem2).addListener(with(any(EventListener.class)));
                allowing(finishedItem1).addListener(with(any(EventListener.class)));
                allowing(finishedItem2).addListener(with(any(EventListener.class)));

                allowing(unfinishedItem1).setAttribute(with(equal(DownloadItem.DOWNLOAD_ITEM)),
                        with(any(DownloadItem.class)), with(equal(false)));
                allowing(unfinishedItem2).setAttribute(with(equal(DownloadItem.DOWNLOAD_ITEM)),
                        with(any(DownloadItem.class)), with(equal(false)));
                allowing(finishedItem1).setAttribute(with(equal(DownloadItem.DOWNLOAD_ITEM)),
                        with(any(DownloadItem.class)), with(equal(false)));
                allowing(finishedItem2).setAttribute(with(equal(DownloadItem.DOWNLOAD_ITEM)),
                        with(any(DownloadItem.class)), with(equal(false)));

                allowing(unfinishedItem1).getSha1Urn();
                will(returnValue(urn1));
                allowing(unfinishedItem2).getSha1Urn();
                will(returnValue(urn2));
                allowing(finishedItem1).getSha1Urn();
                will(returnValue(urn3));
                allowing(finishedItem2).getSha1Urn();
                will(returnValue(urn4));
            }
        });

        EventList<DownloadItem> downloads = coreDownloadListManager.getDownloads();

        downloadListener.get().downloadAdded(unfinishedItem1);
        downloadListener.get().downloadAdded(unfinishedItem2);
        downloadListener.get().downloadAdded(finishedItem1);
        downloadListener.get().downloadAdded(finishedItem2);
        coreDownloadListManager.clearFinished();

        assertEquals(2, downloads.size());
        assertTrue(coreDownloadListManager.contains(urn1));
        assertTrue(coreDownloadListManager.contains(urn2));

        assertFalse(coreDownloadListManager.contains(urn3));
        assertFalse(coreDownloadListManager.contains(urn4));

        assertNotNull(coreDownloadListManager.getDownloadItem(urn1));
        assertNotNull(coreDownloadListManager.getDownloadItem(urn2));
        assertNull(coreDownloadListManager.getDownloadItem(urn3));
        assertNull(coreDownloadListManager.getDownloadItem(urn4));

        context.assertIsSatisfied();
    }

    /**
     * Tests that the Downloads completed event fires only when there are no
     * downloads in progress.
     */
    public void testUpdateDownloadsCompleted() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final DownloadListenerList listenerList = context.mock(DownloadListenerList.class);
        final RemoteFileDescFactory remoteFileDescFactory = context
                .mock(RemoteFileDescFactory.class);
        final SpamManager spamManager = context.mock(SpamManager.class);
        final ItunesDownloadListenerFactory itunesDownloadListenerFactory = context
                .mock(ItunesDownloadListenerFactory.class);
        final FriendManager friendManager = context.mock(FriendManager.class);
        final TorrentDownloadListenerFactory torrentDownloadListenerFactory = context
        .mock(TorrentDownloadListenerFactory.class);
        
        context.checking(new Expectations() {
            {
                one(listenerList).addDownloadListener(with(any(DownloadListener.class)));
                one(downloadManager).downloadsInProgress();
                will(returnValue(0));
            }
        });

        CoreDownloadListManager coreDownloadListManager = new CoreDownloadListManager(
                downloadManager, remoteFileDescFactory, spamManager,
                itunesDownloadListenerFactory, friendManager, torrentDownloadListenerFactory);

        coreDownloadListManager.registerDownloadListener(listenerList);

        TestPropertyChangeListener listener = new TestPropertyChangeListener();
        coreDownloadListManager.addPropertyChangeListener(listener);
        coreDownloadListManager.updateDownloadsCompleted();

        assertEquals(1, listener.getEventCount());
        assertEquals(CoreDownloadListManager.DOWNLOADS_COMPLETED, listener.getLatestEvent()
                .getPropertyName());
        assertEquals(Boolean.TRUE, listener.getLatestEvent().getNewValue());

        coreDownloadListManager.removePropertyChangeListener(listener);

        context.checking(new Expectations() {
            {
                one(downloadManager).downloadsInProgress();
                will(returnValue(2));
            }
        });

        listener = new TestPropertyChangeListener();
        coreDownloadListManager.addPropertyChangeListener(listener);
        coreDownloadListManager.updateDownloadsCompleted();

        assertEquals(0, listener.getEventCount());
        assertNull(listener.getLatestEvent());

        context.assertIsSatisfied();
    }

    public void testAddMagnetDownload() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final DownloadListenerList listenerList = context.mock(DownloadListenerList.class);
        final RemoteFileDescFactory remoteFileDescFactory = context
                .mock(RemoteFileDescFactory.class);
        final SpamManager spamManager = context.mock(SpamManager.class);
        final ItunesDownloadListenerFactory itunesDownloadListenerFactory = context
                .mock(ItunesDownloadListenerFactory.class);
        final FriendManager friendManager = context.mock(FriendManager.class);
        final TorrentDownloadListenerFactory torrentDownloadListenerFactory = context
        .mock(TorrentDownloadListenerFactory.class);
        
        context.checking(new Expectations() {
            {
                one(listenerList).addDownloadListener(with(any(DownloadListener.class)));
            }
        });

        CoreDownloadListManager coreDownloadListManager = new CoreDownloadListManager(
                downloadManager, remoteFileDescFactory, spamManager,
                itunesDownloadListenerFactory, friendManager, torrentDownloadListenerFactory);
        coreDownloadListManager.registerDownloadListener(listenerList);

        final MagnetLinkImpl magnetLink = context.mock(MagnetLinkImpl.class);
        final MagnetOptions magnetOptions = context.mock(MagnetOptions.class);
        final DownloadItem downloadItem = context.mock(DownloadItem.class);
        final Downloader downloader = context.mock(Downloader.class);

        // overwrite false and null saveFile
        context.checking(new Expectations() {
            {
                one(magnetLink).getMagnetOptions();
                will(returnValue(magnetOptions));
                one(downloadManager).download(magnetOptions, false, null, null);
                will(returnValue(downloader));
                one(downloader).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem));

            }
        });

        DownloadItem downloadItemResult = coreDownloadListManager.addDownload(magnetLink, null,
                false);
        assertEquals(downloadItem, downloadItemResult);

        // overwrite true and null saveFile
        context.checking(new Expectations() {
            {
                one(magnetLink).getMagnetOptions();
                will(returnValue(magnetOptions));
                one(downloadManager).download(magnetOptions, true, null, null);
                will(returnValue(downloader));
                one(downloader).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem));

            }
        });

        downloadItemResult = coreDownloadListManager.addDownload(magnetLink, null, true);
        assertEquals(downloadItem, downloadItemResult);

        // overwrite true and nonnull saveFile
        final String fileName = "somename.txt";
        final File parentDir = new File("/tmp/somedir/");
        final File saveFile = new File(parentDir, fileName);

        context.checking(new Expectations() {
            {
                one(magnetLink).getMagnetOptions();
                will(returnValue(magnetOptions));
                one(downloadManager).download(magnetOptions, true, parentDir, fileName);
                will(returnValue(downloader));
                one(downloader).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem));

            }
        });

        downloadItemResult = coreDownloadListManager.addDownload(magnetLink, saveFile, true);
        assertEquals(downloadItem, downloadItemResult);

        context.assertIsSatisfied();
    }

    public void testAddTorrentURIDownload() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final DownloadListenerList listenerList = context.mock(DownloadListenerList.class);
        final RemoteFileDescFactory remoteFileDescFactory = context
                .mock(RemoteFileDescFactory.class);
        final SpamManager spamManager = context.mock(SpamManager.class);
        final ItunesDownloadListenerFactory itunesDownloadListenerFactory = context
                .mock(ItunesDownloadListenerFactory.class);
        final FriendManager friendManager = context.mock(FriendManager.class);

        final TorrentDownloadListenerFactory torrentDownloadListenerFactory = context
        .mock(TorrentDownloadListenerFactory.class);
        
        context.checking(new Expectations() {
            {
                one(listenerList).addDownloadListener(with(any(DownloadListener.class)));
            }
        });

        CoreDownloadListManager coreDownloadListManager = new CoreDownloadListManager(
                downloadManager, remoteFileDescFactory, spamManager,
                itunesDownloadListenerFactory, friendManager, torrentDownloadListenerFactory);
        coreDownloadListManager.registerDownloadListener(listenerList);

        final DownloadItem downloadItem = context.mock(DownloadItem.class);
        final Downloader downloader = context.mock(Downloader.class);

        final URI uri = new URI("http://www.limewire.com");

        // overwrite false
        context.checking(new Expectations() {
            {
                one(downloadManager).downloadTorrent(uri, false);
                will(returnValue(downloader));
                one(downloader).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem));

            }
        });

        DownloadItem downloadItemResult = coreDownloadListManager.addTorrentDownload(uri, false);
        assertEquals(downloadItem, downloadItemResult);

        // overwrite true
        context.checking(new Expectations() {
            {
                one(downloadManager).downloadTorrent(uri, true);
                will(returnValue(downloader));
                one(downloader).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem));

            }
        });

        downloadItemResult = coreDownloadListManager.addTorrentDownload(uri, true);
        assertEquals(downloadItem, downloadItemResult);

        context.assertIsSatisfied();
    }
    
    public void testAddTorrentFileDownload() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final DownloadListenerList listenerList = context.mock(DownloadListenerList.class);
        final RemoteFileDescFactory remoteFileDescFactory = context
                .mock(RemoteFileDescFactory.class);
        final SpamManager spamManager = context.mock(SpamManager.class);
        final ItunesDownloadListenerFactory itunesDownloadListenerFactory = context
                .mock(ItunesDownloadListenerFactory.class);
        final FriendManager friendManager = context.mock(FriendManager.class);
        final TorrentDownloadListenerFactory torrentDownloadListenerFactory = context
        .mock(TorrentDownloadListenerFactory.class);
        
        context.checking(new Expectations() {
            {
                one(listenerList).addDownloadListener(with(any(DownloadListener.class)));
            }
        });

        CoreDownloadListManager coreDownloadListManager = new CoreDownloadListManager(
                downloadManager, remoteFileDescFactory, spamManager,
                itunesDownloadListenerFactory, friendManager, torrentDownloadListenerFactory);
        coreDownloadListManager.registerDownloadListener(listenerList);

        final DownloadItem downloadItem = context.mock(DownloadItem.class);
        final Downloader downloader = context.mock(Downloader.class);

        final File file = new File("testFile");

        // overwrite false
        context.checking(new Expectations() {
            {
                one(downloadManager).downloadTorrent(file, null, false);
                will(returnValue(downloader));
                one(downloader).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem));

            }
        });

        DownloadItem downloadItemResult = coreDownloadListManager.addTorrentDownload(file, null, false);
        assertEquals(downloadItem, downloadItemResult);

        // overwrite true
        context.checking(new Expectations() {
            {
                one(downloadManager).downloadTorrent(file, null, true);
                will(returnValue(downloader));
                one(downloader).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem));

            }
        });

        downloadItemResult = coreDownloadListManager.addTorrentDownload(file, null, true);
        assertEquals(downloadItem, downloadItemResult);

        context.assertIsSatisfied();
    }
    
    @SuppressWarnings("unchecked")
    public void testAddSearchDownload() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final DownloadListenerList listenerList = context.mock(DownloadListenerList.class);
        final RemoteFileDescFactory remoteFileDescFactory = context
                .mock(RemoteFileDescFactory.class);
        final SpamManager spamManager = context.mock(SpamManager.class);
        final ItunesDownloadListenerFactory itunesDownloadListenerFactory = context
                .mock(ItunesDownloadListenerFactory.class);
        final FriendManager friendManager = context.mock(FriendManager.class);
        final TorrentDownloadListenerFactory torrentDownloadListenerFactory = context
        .mock(TorrentDownloadListenerFactory.class);
        
        context.checking(new Expectations() {
            {
                one(listenerList).addDownloadListener(with(any(DownloadListener.class)));
            }
        });

        CoreDownloadListManager coreDownloadListManager = new CoreDownloadListManager(
                downloadManager, remoteFileDescFactory, spamManager,
                itunesDownloadListenerFactory, friendManager, torrentDownloadListenerFactory);
        coreDownloadListManager.registerDownloadListener(listenerList);

        final DownloadItem downloadItem = context.mock(DownloadItem.class);
        final Downloader downloader = context.mock(Downloader.class);

        final Search search = context.mock(Search.class);
        final RemoteFileDescAdapter searchResult = context.mock(RemoteFileDescAdapter.class);
        final List<RemoteFileDescAdapter> searchResults = Collections.singletonList(searchResult);
        final RemoteFileDesc remoteFileDesc = context.mock(RemoteFileDesc.class);
        final RemoteFileDesc[] remoteFileDescs = new RemoteFileDesc[]{remoteFileDesc};
        final List<IpPort> alts = new ArrayList<IpPort>();
        final URN urn1 = URN.createUrnFromString("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA1");
        final Address address = context.mock(Address.class);
        final Category category = Category.AUDIO;
        
        // overwrite false saveFile null
        context.checking(new Expectations() {
            {
                one(remoteFileDesc).getSHA1Urn();
                will(returnValue(urn1));
                one(remoteFileDesc).getAddress();
                will(returnValue(address));
                one(searchResult).getRfd();
                will(returnValue(remoteFileDesc));
                one(searchResult).getAlts();
                will(returnValue(alts));
                one(searchResult).getCategory();
                will(returnValue(category));
                one(downloadManager).download(remoteFileDescs, Collections.EMPTY_LIST, null, false, null, null);
                will(returnValue(downloader));
                one(downloader).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem));
                one(spamManager).handleUserMarkedGood(searchResults);
            }
        });

        DownloadItem downloadItemResult = coreDownloadListManager.addDownload(search, searchResults);
        assertEquals(downloadItem, downloadItemResult);
        
        // overwrite true saveFile null
        context.checking(new Expectations() {
            {
                one(remoteFileDesc).getSHA1Urn();
                will(returnValue(urn1));
                one(remoteFileDesc).getAddress();
                will(returnValue(address));
                one(searchResult).getRfd();
                will(returnValue(remoteFileDesc));
                one(searchResult).getAlts();
                will(returnValue(alts));
                one(searchResult).getCategory();
                will(returnValue(category));
                one(downloadManager).download(remoteFileDescs, Collections.EMPTY_LIST, null, true, null, null);
                will(returnValue(downloader));
                one(downloader).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem));
                one(spamManager).handleUserMarkedGood(searchResults);
            }
        });

        downloadItemResult = coreDownloadListManager.addDownload(search, searchResults, null, true);
        assertEquals(downloadItem, downloadItemResult);
        
        // overwrite true saveFile non-null
        final String fileName = "somename.txt";
        final File parentDir = new File("/tmp/somedir/");
        final File saveFile = new File(parentDir, fileName);
        context.checking(new Expectations() {
            {
                one(remoteFileDesc).getSHA1Urn();
                will(returnValue(urn1));
                one(remoteFileDesc).getAddress();
                will(returnValue(address));
                one(searchResult).getRfd();
                will(returnValue(remoteFileDesc));
                one(searchResult).getAlts();
                will(returnValue(alts));
                one(searchResult).getCategory();
                will(returnValue(category));
                one(downloadManager).download(remoteFileDescs, Collections.EMPTY_LIST, null, true, parentDir, fileName);
                will(returnValue(downloader));
                one(downloader).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem));
                one(spamManager).handleUserMarkedGood(searchResults);
            }
        });

        downloadItemResult = coreDownloadListManager.addDownload(search, searchResults, saveFile, true);
        assertEquals(downloadItem, downloadItemResult);
        
        context.assertIsSatisfied();
    }
}
