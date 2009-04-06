package org.limewire.core.impl.download.listener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.downloader.CoreDownloader;

public class RecentDownloadListenerTest extends BaseTestCase {

    public RecentDownloadListenerTest(String name) {
        super(name);
    }

    /**
     * Testing that downloaders with null save files do not have their values
     * added to the RecentDownloads setting.
     */
    public void testNullFileNotAdded() {
        DownloadSettings.RECENT_DOWNLOADS.clear();
        Mockery context = new Mockery();
        final Downloader downloader = context.mock(CoreDownloader.class);
        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(DownloadState.COMPLETE));
                one(downloader).getSaveFile();
                will(returnValue(null));
            }
        });

        assertEquals(0, DownloadSettings.RECENT_DOWNLOADS.length());
        new RecentDownloadListener(downloader);
        assertEquals(0, DownloadSettings.RECENT_DOWNLOADS.length());
        context.assertIsSatisfied();
    }

    /**
     * Testing that events that are not completion events for a downloader do
     * not have their values added to the RecentDownloads setting.
     */
    public void testIncomplete1File() {
        DownloadSettings.RECENT_DOWNLOADS.clear();
        Mockery context = new Mockery();
        final Downloader downloader = context.mock(CoreDownloader.class);
        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(DownloadState.ABORTED));
            }
        });

        assertEquals(0, DownloadSettings.RECENT_DOWNLOADS.length());
        new RecentDownloadListener(downloader);
        assertEquals(0, DownloadSettings.RECENT_DOWNLOADS.length());
        context.assertIsSatisfied();
    }

    /**
     * Testing that adding a complete downloader adds a file to the
     * RecentDownloads Setting
     */
    public void testComplete1File() {
        DownloadSettings.RECENT_DOWNLOADS.clear();
        Mockery context = new Mockery();
        final Downloader downloader = context.mock(CoreDownloader.class);
        final File saveFile = new File("testComplete1File");
        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(DownloadState.COMPLETE));
                one(downloader).getSaveFile();
                will(returnValue(saveFile));
            }
        });

        assertEquals(0, DownloadSettings.RECENT_DOWNLOADS.length());
        new RecentDownloadListener(downloader);
        assertEquals(1, DownloadSettings.RECENT_DOWNLOADS.length());
        assertEquals(saveFile.getName(), DownloadSettings.RECENT_DOWNLOADS.get().iterator()
                .next().getName());
        context.assertIsSatisfied();
    }

    /**
     * Testing that adding 2 complete downloaders adds both files
     */
    public void testComplete2Files() {
        DownloadSettings.RECENT_DOWNLOADS.clear();
        Mockery context = new Mockery();
        final Downloader downloader1 = context.mock(CoreDownloader.class);
        final File saveFile1 = new File("testComplete2Files_1");
        final Downloader downloader2 = context.mock(CoreDownloader.class);
        final File saveFile2 = new File("testComplete2Files_2");

        context.checking(new Expectations() {
            {
                one(downloader1).getState();
                will(returnValue(DownloadState.COMPLETE));
                one(downloader1).getSaveFile();
                will(returnValue(saveFile1));
                one(downloader2).getState();
                will(returnValue(DownloadState.COMPLETE));
                one(downloader2).getSaveFile();
                will(returnValue(saveFile2));
            }
        });

        assertEquals(0, DownloadSettings.RECENT_DOWNLOADS.length());
        new RecentDownloadListener(downloader1);
        assertEquals(1, DownloadSettings.RECENT_DOWNLOADS.length());
        assertEquals(saveFile1.getName(), DownloadSettings.RECENT_DOWNLOADS.get().iterator()
                .next().getName());
        new RecentDownloadListener(downloader2);
        assertEquals(2, DownloadSettings.RECENT_DOWNLOADS.length());
        
        List<File> list = new ArrayList<File>(DownloadSettings.RECENT_DOWNLOADS.get());
        Collections.sort(list, new SortByFileNameComparator());
        assertEquals(saveFile1.getName(), list.get(0).getName());
        assertEquals(saveFile2.getName(), list.get(1).getName());
        context.assertIsSatisfied();
    }
    
    /**
     * Testing that adding 2 complete downloaders adds the files and keeps the
     * most recent 1 when going over the max tracked amount.
     */
    public void testFilesGetRemovedWhenGoingOverLimit() {
        DownloadSettings.RECENT_DOWNLOADS.clear();
        Mockery context = new Mockery();
        final Downloader downloader1 = context.mock(CoreDownloader.class);
        final File saveFile1 = new File("testComplete2Files_1");
        final Downloader downloader2 = context.mock(CoreDownloader.class);
        final File saveFile2 = new File("testComplete2Files_2");
        context.checking(new Expectations() {
            {
                one(downloader1).getState();
                will(returnValue(DownloadState.COMPLETE));
                one(downloader1).getSaveFile();
                will(returnValue(saveFile1));
                one(downloader2).getState();
                will(returnValue(DownloadState.COMPLETE));
                one(downloader2).getSaveFile();
                will(returnValue(saveFile2));
            }
        });

        assertEquals(0, DownloadSettings.RECENT_DOWNLOADS.length());
        new RecentDownloadListener(downloader1);
        assertEquals(1, DownloadSettings.RECENT_DOWNLOADS.length());
        assertEquals(saveFile1.getName(), DownloadSettings.RECENT_DOWNLOADS.get().iterator()
                .next().getName());
        new RecentDownloadListener(downloader2, 1);
        assertEquals(1, DownloadSettings.RECENT_DOWNLOADS.length());
        assertEquals(saveFile2.getName(), DownloadSettings.RECENT_DOWNLOADS.get().iterator()
                .next().getName());
        context.assertIsSatisfied();
    }
    
    /**
     * Orders files by name.
     */
    private class SortByFileNameComparator implements Comparator<File> {
        @Override
        public int compare(File o1, File o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }
}
