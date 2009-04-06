package org.limewire.core.impl.download.listener;

import java.io.File;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.impl.itunes.ItunesMediator;
import org.limewire.core.settings.iTunesSettings;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Downloader.DownloadState;
import com.limegroup.gnutella.downloader.CoreDownloader;

public class ItunesDownloadListenerTest extends BaseTestCase {

    public ItunesDownloadListenerTest(String name) {
        super(name);
    }

    /**
     * Testing that downloaders with null save files do not have their values
     * added to Itunes
     */
    public void testNullFileNotAdded() {
        iTunesSettings.ITUNES_SUPPORT_ENABLED.setValue(true);
        Mockery context = new Mockery();
        final Downloader downloader = context.mock(CoreDownloader.class);
        final ItunesMediator itunesMediator = context.mock(ItunesMediator.class);

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(DownloadState.COMPLETE));
                one(downloader).getSaveFile();
                will(returnValue(null));
            }
        });

        new ItunesDownloadListener(downloader, itunesMediator);
        context.assertIsSatisfied();
    }

    /**
     * Testing that downloaders that are notComplete will not have their files
     * added to itunes.
     */
    public void testDownloadNotComplete() {
        iTunesSettings.ITUNES_SUPPORT_ENABLED.setValue(true);
        Mockery context = new Mockery();
        final Downloader downloader = context.mock(CoreDownloader.class);
        final ItunesMediator itunesMediator = context.mock(ItunesMediator.class);

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(DownloadState.ABORTED));
            }
        });

        new ItunesDownloadListener(downloader, itunesMediator);
        context.assertIsSatisfied();
    }

    /**
     * Testing complete files are added to itunes.
     */
    public void testDownloadComplete() {
        iTunesSettings.ITUNES_SUPPORT_ENABLED.setValue(true);
        Mockery context = new Mockery();
        final Downloader downloader = context.mock(CoreDownloader.class);
        final ItunesMediator itunesMediator = context.mock(ItunesMediator.class);
        final File file = new File("testDownloadComplete");
        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(DownloadState.COMPLETE));
                one(downloader).getSaveFile();
                will(returnValue(file));
                one(itunesMediator).addSong(file);
            }
        });

        new ItunesDownloadListener(downloader, itunesMediator);
        context.assertIsSatisfied();
    }
    
    /**
     * Testing files are not added when itunes is not enabled.
     */
    public void testDownloadCompleteItunesNotEnabled() {
        iTunesSettings.ITUNES_SUPPORT_ENABLED.setValue(false);
        Mockery context = new Mockery();
        final Downloader downloader = context.mock(CoreDownloader.class);
        final ItunesMediator itunesMediator = context.mock(ItunesMediator.class);
        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(DownloadState.ABORTED));
            }
        });

        new ItunesDownloadListener(downloader, itunesMediator);
        context.assertIsSatisfied();
    }
}
