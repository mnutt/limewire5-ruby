package org.limewire.core.impl.download;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.DownloadItem.ErrorState;
import org.limewire.io.Address;
import org.limewire.listener.EventListener;
import org.limewire.util.BaseTestCase;
import org.limewire.util.TestPropertyChangeListener;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

public class CoreDownloadItemTest extends BaseTestCase {
    private Mockery context;

    private Downloader downloader;

    private QueueTimeCalculator queueTimeCalculator;

    private CoreDownloadItem coreDownloadItem;

    private LimeXMLDocument document;

    private final String title = "Hello World";

    private final String artist = "Me and you";

    private final String album = "Testing the waters";

    private final String genre = "Rock";

    private final String comments = "woah!";

    private final Long bitrate = new Long(128);

    private final Long seconds = new Long(956);

    private final Long year = new Long(1999);

    private final String track = "5";

    public CoreDownloadItemTest(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void setUp() throws Exception {
        context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        downloader = context.mock(Downloader.class);
        queueTimeCalculator = context.mock(QueueTimeCalculator.class);
        document = context.mock(LimeXMLDocument.class);

        context.checking(new Expectations() {
            {
                one(downloader).addListener(with(any(EventListener.class)));
                allowing(document).getValue(LimeXMLNames.AUDIO_ALBUM);
                will(returnValue(album.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_ARTIST);
                will(returnValue(artist.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_BITRATE);
                will(returnValue(bitrate.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_COMMENTS);
                will(returnValue(comments.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_GENRE);
                will(returnValue(genre.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_TRACK);
                will(returnValue(track.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_YEAR);
                will(returnValue(year.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_TITLE);
                will(returnValue(title.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_SECONDS);
                will(returnValue(seconds.toString()));
            }
        });

        coreDownloadItem = new CoreDownloadItem(downloader, queueTimeCalculator, null);

    }

    /**
     * Tests cancel method for the CoreDownloadItem. Ensures that the
     * downloaders stop method is called. Ensures that a property change event
     * is fired on the state property with a DownloadState of Cancelled.
     */
    public void testCancel() throws Exception {
        TestPropertyChangeListener listener = new TestPropertyChangeListener();
        coreDownloadItem.addPropertyChangeListener(listener);

        context.checking(new Expectations() {
            {
                one(downloader).stop();
                one(downloader).deleteIncompleteFiles();
            }
        });

        assertEquals(0, listener.getEventCount());

        coreDownloadItem.cancel();

        assertEquals(1, listener.getEventCount());
        PropertyChangeEvent propertyChangeEvent = listener.getLatestEvent();
        assertEquals("state", propertyChangeEvent.getPropertyName());
        DownloadState downloadState = (DownloadState) propertyChangeEvent.getNewValue();
        assertEquals(DownloadState.CANCELLED, downloadState);
        Thread.sleep(1000);
        context.assertIsSatisfied();
    }

    /**
     * Test the getCategory method for the CoreDownloadItem. Handles cases where
     * getFile is available or cases where only getSaveFile is available.
     */
    public void testGetCategory() {
        context.checking(new Expectations() {
            {
                one(downloader).getFile();
                will(returnValue(new File("test.mp3")));
            }
        });
        Category testCategory1 = coreDownloadItem.getCategory();
        assertEquals(Category.AUDIO, testCategory1);

        context.checking(new Expectations() {
            {
                one(downloader).getFile();
                will(returnValue(null));
                one(downloader).getSaveFile();
                will(returnValue(new File("test.txt")));
            }
        });
        Category testCategory2 = coreDownloadItem.getCategory();
        assertEquals(Category.DOCUMENT, testCategory2);
        context.assertIsSatisfied();
    }

    public void testGetDownloadSourceCount() {
        final int downloadSoureCount = 5;

        context.checking(new Expectations() {
            {
                one(downloader).getNumHosts();
                will(returnValue(downloadSoureCount));
            }
        });
        assertEquals(downloadSoureCount, coreDownloadItem.getDownloadSourceCount());

        context.assertIsSatisfied();
    }

    public void testGetLocalQueuePriority() {
        final int localQueuePriority = 3;

        context.checking(new Expectations() {
            {
                one(downloader).getInactivePriority();
                will(returnValue(localQueuePriority));
            }
        });
        assertEquals(localQueuePriority, coreDownloadItem.getLocalQueuePriority());

        context.assertIsSatisfied();
    }

    public void testPause() {

        context.checking(new Expectations() {
            {
                one(downloader).pause();
            }
        });

        coreDownloadItem.pause();
        context.assertIsSatisfied();
    }

    public void testIsLauncable() {

        context.checking(new Expectations() {
            {
                one(downloader).isLaunchable();
                will(returnValue(true));
            }
        });

        assertTrue(coreDownloadItem.isLaunchable());

        context.checking(new Expectations() {
            {
                one(downloader).isLaunchable();
                will(returnValue(false));
            }
        });

        assertFalse(coreDownloadItem.isLaunchable());
        context.assertIsSatisfied();
    }

    public void testIsSearchAgainEnabled() {

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.WAITING_FOR_USER));
            }
        });

        assertTrue(coreDownloadItem.isTryAgainEnabled());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.DOWNLOADING));
            }
        });

        assertFalse(coreDownloadItem.isTryAgainEnabled());
        context.assertIsSatisfied();
    }

    public void testResume() {

        context.checking(new Expectations() {
            {
                one(downloader).resume();
            }
        });

        coreDownloadItem.resume();
        context.assertIsSatisfied();
    }

    public void testGetRemoteQueuePosition() {
        final int remoteQueuePosition = 6;

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.REMOTE_QUEUED));
                one(downloader).getQueuePosition();
                will(returnValue(remoteQueuePosition));
            }
        });
        assertEquals(remoteQueuePosition, coreDownloadItem.getRemoteQueuePosition());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.DOWNLOADING));
            }
        });
        assertEquals(-1, coreDownloadItem.getRemoteQueuePosition());

        context.assertIsSatisfied();
    }

    public void testGetTitle() {
        final String fileName = "test.txt";
        context.checking(new Expectations() {
            {
                one(downloader).getSaveFile();
                will(returnValue(new File(fileName)));
            }
        });
        assertEquals(fileName, coreDownloadItem.getTitle());

        context.assertIsSatisfied();
    }

    public void testGetDownloadingFile() {
        final File testFile = new File("test");

        context.checking(new Expectations() {
            {
                one(downloader).getFile();
                will(returnValue(testFile));
            }
        });

        assertEquals(testFile, coreDownloadItem.getDownloadingFile());
        context.assertIsSatisfied();
    }

    public void testGetRemainingDownloadTime() throws InsufficientDataException {
        context.checking(new Expectations() {
            {
                exactly(2).of(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.DOWNLOADING));
                allowing(downloader).getContentLength();
                will(returnValue(1000L * 1000L));
                allowing(downloader).getAmountRead();
                will(returnValue(0L));
                allowing(downloader).getMeasuredBandwidth();
                will(returnValue(100f));
            }
        });
        coreDownloadItem.fireDataChanged();
        assertEquals(9L, coreDownloadItem.getRemainingDownloadTime());
        context.assertIsSatisfied();
    }

    public void testGetPercentageComplete() {
        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.COMPLETE));
            }
        });
        assertEquals(100, coreDownloadItem.getPercentComplete());

        context.checking(new Expectations() {
            {
                exactly(2).of(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.DOWNLOADING));
                one(downloader).getContentLength();
                will(returnValue(0L));
            }
        });
        assertEquals(0, coreDownloadItem.getPercentComplete());

        context.checking(new Expectations() {
            {
                exactly(2).of(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.DOWNLOADING));
                exactly(2).of(downloader).getContentLength();
                will(returnValue(10L));
                one(downloader).getAmountRead();
                will(returnValue(1L));
            }
        });
        coreDownloadItem.fireDataChanged();
        assertEquals(10, coreDownloadItem.getPercentComplete());
        context.assertIsSatisfied();
    }

    public void testGetSources() {
        final List<Address> addresses = new ArrayList<Address>();

        context.checking(new Expectations() {
            {
                one(downloader).getSourcesAsAddresses();
                will(returnValue(addresses));
            }
        });
        assertEquals(addresses, coreDownloadItem.getSources());
        context.assertIsSatisfied();
    }

    public void testGetProperties() {
        context.checking(new Expectations() {
            {
                allowing(downloader).getAttribute("LimeXMLDocument");
                will(returnValue(document));
                allowing(downloader).getSaveFile();
                will(returnValue(new File("test.mp3")));
                allowing(downloader).getFile();
                will(returnValue(new File("test.mp3")));
                allowing(downloader).getContentLength();
                will(returnValue(1234L));
            }
        });

        assertEquals(artist, coreDownloadItem.getProperty(FilePropertyKey.AUTHOR));
        assertEquals(title, coreDownloadItem.getProperty(FilePropertyKey.TITLE));
        assertEquals(album, coreDownloadItem.getProperty(FilePropertyKey.ALBUM));
        assertEquals(genre, coreDownloadItem.getProperty(FilePropertyKey.GENRE));
        assertEquals(track, coreDownloadItem.getProperty(FilePropertyKey.TRACK_NUMBER));
        assertEquals(year, coreDownloadItem.getProperty(FilePropertyKey.YEAR));
        assertEquals(seconds, coreDownloadItem.getProperty(FilePropertyKey.LENGTH));
        assertEquals(bitrate, coreDownloadItem.getProperty(FilePropertyKey.BITRATE));
        assertEquals(comments, coreDownloadItem.getProperty(FilePropertyKey.DESCRIPTION));

        assertEquals(artist, coreDownloadItem.getPropertyString(FilePropertyKey.AUTHOR));
        assertEquals(title, coreDownloadItem.getPropertyString(FilePropertyKey.TITLE));
        assertEquals(album, coreDownloadItem.getPropertyString(FilePropertyKey.ALBUM));
        assertEquals(genre, coreDownloadItem.getPropertyString(FilePropertyKey.GENRE));
        assertEquals(track, coreDownloadItem.getPropertyString(FilePropertyKey.TRACK_NUMBER));
        assertEquals(year + "", coreDownloadItem.getPropertyString(FilePropertyKey.YEAR));
        assertEquals(seconds + "", coreDownloadItem.getPropertyString(FilePropertyKey.LENGTH));
        assertEquals(bitrate + "", coreDownloadItem.getPropertyString(FilePropertyKey.BITRATE));
        assertEquals(comments, coreDownloadItem.getPropertyString(FilePropertyKey.DESCRIPTION));

        context.assertIsSatisfied();
    }

    public void testGetState() throws Exception {
        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.ABORTED));
            }
        });

        assertEquals(DownloadState.CANCELLED, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.BUSY));
            }
        });

        assertEquals(DownloadState.REMOTE_QUEUED, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.REMOTE_QUEUED));
            }
        });

        assertEquals(DownloadState.REMOTE_QUEUED, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.ABORTED));
            }
        });

        assertEquals(DownloadState.CANCELLED, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.COMPLETE));
            }
        });

        assertEquals(DownloadState.DONE, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.CONNECTING));
            }
        });

        assertEquals(DownloadState.CONNECTING, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.CORRUPT_FILE));
            }
        });

        assertEquals(DownloadState.ERROR, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.DISK_PROBLEM));
            }
        });

        assertEquals(DownloadState.ERROR, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.DOWNLOADING));
            }
        });

        assertEquals(DownloadState.DOWNLOADING, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.FETCHING));
            }
        });

        assertEquals(DownloadState.DOWNLOADING, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.GAVE_UP));
            }
        });

        assertEquals(DownloadState.STALLED, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.WAITING_FOR_USER));
            }
        });

        assertEquals(DownloadState.STALLED, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.HASHING));
                one(downloader).getContentLength();
                will(returnValue(2L));
            }
        });

        assertEquals(DownloadState.FINISHING, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.SAVING));
                one(downloader).getContentLength();
                will(returnValue(2L));
            }
        });

        assertEquals(DownloadState.FINISHING, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.IDENTIFY_CORRUPTION));
            }
        });

        assertEquals(DownloadState.ERROR, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.INITIALIZING));
            }
        });

        assertEquals(DownloadState.CONNECTING, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.INVALID));
            }
        });

        assertEquals(DownloadState.ERROR, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.ITERATIVE_GUESSING));
            }
        });

        assertEquals(DownloadState.TRYING_AGAIN, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.PAUSED));
            }
        });

        assertEquals(DownloadState.PAUSED, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.QUERYING_DHT));
            }
        });

        assertEquals(DownloadState.TRYING_AGAIN, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.QUEUED));
            }
        });

        assertEquals(DownloadState.LOCAL_QUEUED, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.RECOVERY_FAILED));
            }
        });

        assertEquals(DownloadState.ERROR, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.REMOTE_QUEUED));
            }
        });

        assertEquals(DownloadState.REMOTE_QUEUED, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.RESUMING));
            }
        });

        assertEquals(DownloadState.RESUMING, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.WAITING_FOR_GNET_RESULTS));
            }
        });

        assertEquals(DownloadState.TRYING_AGAIN, coreDownloadItem.getState());

        context.checking(new Expectations() {
            {
                one(downloader).stop();
                one(downloader).deleteIncompleteFiles();
            }
        });
        coreDownloadItem.cancel();
        Thread.sleep(1000);
        assertEquals(DownloadState.CANCELLED, coreDownloadItem.getState());

        context.assertIsSatisfied();
    }

    public void testGetErrorState() {
        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.CORRUPT_FILE));
            }
        });

        assertEquals(ErrorState.CORRUPT_FILE, coreDownloadItem.getErrorState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.RECOVERY_FAILED));
            }
        });

        assertEquals(ErrorState.CORRUPT_FILE, coreDownloadItem.getErrorState());

        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.DISK_PROBLEM));
            }
        });

        assertEquals(ErrorState.DISK_PROBLEM, coreDownloadItem.getErrorState());
        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.INVALID));
            }
        });

        assertEquals(ErrorState.FILE_NOT_SHARABLE, coreDownloadItem.getErrorState());
        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.GAVE_UP));
            }
        });

        assertEquals(ErrorState.UNABLE_TO_CONNECT, coreDownloadItem.getErrorState());
        context.checking(new Expectations() {
            {
                one(downloader).getState();
                will(returnValue(com.limegroup.gnutella.Downloader.DownloadState.DOWNLOADING));
            }
        });

        assertEquals(ErrorState.NONE, coreDownloadItem.getErrorState());
    }
}
