package org.limewire.core.impl.download.listener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.core.IsAnything;
import org.hamcrest.core.IsEqual;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.limewire.bittorrent.TorrentManager;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadException;
import org.limewire.util.BaseTestCase;

import com.limegroup.bittorrent.BTTorrentFileDownloader;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.library.FileCollection;

public class TorrentDownloadListenerTest extends BaseTestCase {

    public TorrentDownloadListenerTest(String name) {
        super(name);
    }

    /**
     * Testing that downloaders with null save files do not have their values
     * added to the RecentDownloads setting.
     */
    public void testNonTorrentFileNotAdded() {
        Mockery context = new Mockery();
        final Downloader downloader = context.mock(CoreDownloader.class);
        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final ActivityCallback activityCallback = context.mock(ActivityCallback.class);
        final TorrentManager torrentManager = context.mock(TorrentManager.class);
        final FileCollection gnutellaFileCollection = context.mock(FileCollection.class);
        final List<DownloadItem> downloadItems = new ArrayList<DownloadItem>();
        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(DownloadState.COMPLETE));
                one(downloader).getSaveFile();
                will(returnValue(new File("testNonTorrentFileNotAdded")));
            }
        });

        new TorrentDownloadListener(downloadManager, activityCallback, gnutellaFileCollection, torrentManager,
                downloadItems, downloader);
        context.assertIsSatisfied();
    }

    /**
     * Testing that downloaders with which are not complete are not processed.
     */
    public void testNonCompleteFileNotAdded() {
        Mockery context = new Mockery();
        final Downloader downloader = context.mock(CoreDownloader.class);
        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final ActivityCallback activityCallback = context.mock(ActivityCallback.class);
        final List<DownloadItem> downloadItems = new ArrayList<DownloadItem>();
        final FileCollection gnutellaFileCollection = context.mock(FileCollection.class);;
        final TorrentManager torrentManager = context.mock(TorrentManager.class);
        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(DownloadState.ABORTED));
            }
        });

        new TorrentDownloadListener(downloadManager, activityCallback, gnutellaFileCollection, torrentManager,
                downloadItems, downloader);
        context.assertIsSatisfied();
    }

    /**
     * Testing that downloaders with save files that end in .torrent add a
     * torrent download.
     */
    public void testTorrentFileDownloadAdded() throws Exception {
        Mockery context = new Mockery();
        final Downloader downloader = context.mock(CoreDownloader.class);
        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final ActivityCallback activityCallback = context.mock(ActivityCallback.class);
        final List<DownloadItem> downloadItems = new ArrayList<DownloadItem>();
        final File torrentFile = new File("testTorrentFileDownloadAdded.torrent");
        final DownloadItem downloadItem = context.mock(DownloadItem.class);
        final FileCollection gnutellaFileCollection = context.mock(FileCollection.class);
        final TorrentManager torrentManager = context.mock(TorrentManager.class);
        downloadItems.add(downloadItem);
        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(DownloadState.COMPLETE));
                one(downloader).getSaveFile();
                will(returnValue(torrentFile));
                one(downloader).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem));
                one(downloadManager).downloadTorrent(torrentFile, null, false);
                one(torrentManager).isDownloadingTorrent(torrentFile);
                will(returnValue(false));
            }
        });

        new TorrentDownloadListener(downloadManager, activityCallback, gnutellaFileCollection, torrentManager,
                downloadItems, downloader);
        assertFalse(downloadItems.contains(downloadItem));
        context.assertIsSatisfied();
    }

    /**
     * Testing that downloaders with save files that end in .torrent add a
     * torrent download.
     */
    public void testTorrentFileDownloadAddedDownloadException() throws Exception {
        Mockery context = new Mockery();
        final Downloader downloader = context.mock(CoreDownloader.class);
        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final ActivityCallback activityCallback = context.mock(ActivityCallback.class);
        final List<DownloadItem> downloadItems = new ArrayList<DownloadItem>();
        final File torrentFile = new File(
                "testTorrentFileDownloadAddedSaveLocationException.torrent");
        final DownloadException e = new DownloadException(
                DownloadException.ErrorCode.FILE_ALREADY_DOWNLOADING, torrentFile);
        final DownloadItem downloadItem = context.mock(DownloadItem.class);
        final FileCollection gnutellaFileCollection = context.mock(FileCollection.class);
        final TorrentManager torrentManager = context.mock(TorrentManager.class);
        downloadItems.add(downloadItem);

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(DownloadState.COMPLETE));
                one(downloader).getSaveFile();
                will(returnValue(torrentFile));
                one(downloader).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem));
                one(downloadManager).downloadTorrent(torrentFile, null, false);
                will(throwException(e));
                one(activityCallback).handleDownloadException(
                        with(new IsAnything<DownloadAction>()),
                        with(new IsEqual<DownloadException>(e)),
                        with(new IsEqual<Boolean>(false)));
                will(new DownloadActionCaller());
                one(downloadManager).downloadTorrent(torrentFile, null, true);
                one(torrentManager).isDownloadingTorrent(torrentFile);
                will(returnValue(false));
            }
        });

        new TorrentDownloadListener(downloadManager, activityCallback, gnutellaFileCollection, torrentManager,
                downloadItems, downloader);
        assertFalse(downloadItems.contains(downloadItem));
        context.assertIsSatisfied();
    }

    /**
     * Testing that downloaders with save files that end in .torrent add a
     * torrent download.
     */
    public void testBTTorrentFileDownloaderCompleted() throws Exception {
        Mockery context = new Mockery();
        final BTTorrentFileDownloader downloader = context.mock(BTTorrentFileDownloader.class);
        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final ActivityCallback activityCallback = context.mock(ActivityCallback.class);
        final List<DownloadItem> downloadItems = new ArrayList<DownloadItem>();
        final DownloadItem downloadItem = context.mock(DownloadItem.class);
        final FileCollection gnutellaFileCollection = context.mock(FileCollection.class);
        final TorrentManager torrentManager = context.mock(TorrentManager.class);
        downloadItems.add(downloadItem);

        final File torrentFile = new File("");

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(DownloadState.COMPLETE));
                one(downloader).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem));
                one(downloader).getTorrentFile();
                will(returnValue(torrentFile));
                one(downloadManager).downloadTorrent(torrentFile, null, false);
                one(torrentManager).isDownloadingTorrent(torrentFile);
                will(returnValue(false));
            }
        });

        new TorrentDownloadListener(downloadManager, activityCallback, gnutellaFileCollection, torrentManager,
                downloadItems, downloader);
        assertFalse(downloadItems.contains(downloadItem));
        context.assertIsSatisfied();
    }

    /**
     * Testing that downloaders with save files that end in .torrent add a
     * torrent download.
     */
    public void testBTTorrentFileDownloaderCompletedWithDownloadException() throws Exception {
        Mockery context = new Mockery();
        final BTTorrentFileDownloader downloader = context.mock(BTTorrentFileDownloader.class);
        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final ActivityCallback activityCallback = context.mock(ActivityCallback.class);
        final List<DownloadItem> downloadItems = new ArrayList<DownloadItem>();
        final DownloadException e = new DownloadException(
                DownloadException.ErrorCode.FILE_ALREADY_DOWNLOADING, null);
        final DownloadItem downloadItem = context.mock(DownloadItem.class);
        final FileCollection gnutellaFileCollection = context.mock(FileCollection.class);
        final TorrentManager torrentManager = context.mock(TorrentManager.class);
        downloadItems.add(downloadItem);

        final File torrentFile = new File("");

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(DownloadState.COMPLETE));
                one(downloader).getAttribute(DownloadItem.DOWNLOAD_ITEM);
                will(returnValue(downloadItem));
                one(downloader).getTorrentFile();
                will(returnValue(torrentFile));
                one(downloadManager).downloadTorrent(torrentFile, null, false);
                will(throwException(e));
                one(activityCallback).handleDownloadException(
                        with(new IsAnything<DownloadAction>()),
                        with(new IsEqual<DownloadException>(e)),
                        with(new IsEqual<Boolean>(false)));
                will(new DownloadActionCaller());
                one(downloadManager).downloadTorrent(torrentFile, null, true);
                one(torrentManager).isDownloadingTorrent(torrentFile);
                will(returnValue(false));
            }
        });

        new TorrentDownloadListener(downloadManager, activityCallback, gnutellaFileCollection, torrentManager,
                downloadItems, downloader);
        assertFalse(downloadItems.contains(downloadItem));
        context.assertIsSatisfied();
    }

    /**
     * Used to call the downloadAction when entering the DownloadException
     * block. Uses override = true saveFile = null
     */
    private class DownloadActionCaller implements Action {

        @Override
        public void describeTo(Description description) {
            description.appendText("Calls the given download action.");

        }

        @Override
        public Object invoke(Invocation invocation) throws Throwable {
            ((DownloadAction) invocation.getParameter(0)).download(null, true);
            return null;
        }

    }
}
