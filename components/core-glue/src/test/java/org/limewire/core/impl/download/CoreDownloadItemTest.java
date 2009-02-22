package org.limewire.core.impl.download;

import java.beans.PropertyChangeEvent;
import java.io.File;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadState;
import org.limewire.listener.EventListener;
import org.limewire.util.BaseTestCase;
import org.limewire.util.TestPropertyChangeListener;

import com.limegroup.gnutella.Downloader;

public class CoreDownloadItemTest extends BaseTestCase {

    public CoreDownloadItemTest(String name) {
        super(name);
    }

    /**
     * Tests cancel method for the CoreDownloadItem. Ensures that the
     * downloaders stop method is called. Ensures that a property change event
     * is fired on the state property with a DownloadState of Cancelled.
     */
    @SuppressWarnings("unchecked")
    public void testCancel() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final Downloader downloader = context.mock(Downloader.class);
        final QueueTimeCalculator queueTimeCalculator = context.mock(QueueTimeCalculator.class);

        context.checking(new Expectations() {
            {
                one(downloader).addListener(with(any(EventListener.class)));
            }
        });
        CoreDownloadItem coreDownloadItem = new CoreDownloadItem(downloader, queueTimeCalculator);
        TestPropertyChangeListener listener = new TestPropertyChangeListener();
        coreDownloadItem.addPropertyChangeListener(listener);

        context.checking(new Expectations() {
            {
                one(downloader).stop(true);
                one(downloader).getFile();
                will(returnValue(null));
            }
        });

        assertEquals(0, listener.getEventCount());

        coreDownloadItem.cancel();

        assertEquals(1, listener.getEventCount());
        PropertyChangeEvent propertyChangeEvent = listener.getLatestEvent();
        assertEquals("state", propertyChangeEvent.getPropertyName());
        DownloadState downloadState = (DownloadState) propertyChangeEvent.getNewValue();
        assertEquals(DownloadState.CANCELLED, downloadState);
        context.assertIsSatisfied();

    }

    /**
     * Test the getCategory method for the CoreDownloadItem. Handles cases where
     * getFile is available or cases where only getSaveFile is available.
     */
    @SuppressWarnings("unchecked")
    public void testGetCategory() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final Downloader downloader = context.mock(Downloader.class);
        final QueueTimeCalculator queueTimeCalculator = context.mock(QueueTimeCalculator.class);

        context.checking(new Expectations() {
            {
                one(downloader).addListener(with(any(EventListener.class)));
            }
        });
        CoreDownloadItem coreDownloadItem = new CoreDownloadItem(downloader, queueTimeCalculator);

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

    }
}
