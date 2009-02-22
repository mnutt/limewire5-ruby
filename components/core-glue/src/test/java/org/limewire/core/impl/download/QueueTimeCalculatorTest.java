package org.limewire.core.impl.download;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.util.BaseTestCase;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

public class QueueTimeCalculatorTest extends BaseTestCase {

    public QueueTimeCalculatorTest(String name) {
        super(name);
    }

    public void testGetRemainingQueueTime1ItemDownloading1Queued() {
        Mockery context = new Mockery();

        final DownloadItem downloadItem1 = context.mock(DownloadItem.class);
        final DownloadItem downloadItem2 = context.mock(DownloadItem.class);

        EventList<DownloadItem> downloadItems = new BasicEventList<DownloadItem>();
        downloadItems.add(downloadItem1);
        downloadItems.add(downloadItem2);

        final long remaingDownloadTime1 = 500L;

        context.checking(new Expectations() {
            {
                allowing(downloadItem1).getState();
                will(returnValue(DownloadState.DOWNLOADING));
                allowing(downloadItem2).getState();
                will(returnValue(DownloadState.LOCAL_QUEUED));

                allowing(downloadItem1).getRemainingDownloadTime();
                will(returnValue(remaingDownloadTime1));

                allowing(downloadItem2).getLocalQueuePriority();
                will(returnValue(1));
            }
        });

        QueueTimeCalculator queueTimeCalculator = new QueueTimeCalculator(downloadItems);
        assertEquals(remaingDownloadTime1, queueTimeCalculator.getRemainingQueueTime(downloadItem2));

        context.assertIsSatisfied();
    }

    public void testGetRemainingQueueTime2ItemsDownloading1Queued() {
        Mockery context = new Mockery();

        final DownloadItem downloadItem1 = context.mock(DownloadItem.class);
        final DownloadItem downloadItem2 = context.mock(DownloadItem.class);
        final DownloadItem downloadItem3 = context.mock(DownloadItem.class);

        EventList<DownloadItem> downloadItems = new BasicEventList<DownloadItem>();
        downloadItems.add(downloadItem1);
        downloadItems.add(downloadItem2);
        downloadItems.add(downloadItem3);

        final long remaingDownloadTime1 = 500L;
        final long remaingDownloadTime2 = 200L;

        context.checking(new Expectations() {
            {
                allowing(downloadItem1).getState();
                will(returnValue(DownloadState.DOWNLOADING));
                allowing(downloadItem2).getState();
                will(returnValue(DownloadState.DOWNLOADING));
                allowing(downloadItem3).getState();
                will(returnValue(DownloadState.LOCAL_QUEUED));

                allowing(downloadItem1).getRemainingDownloadTime();
                will(returnValue(remaingDownloadTime1));

                allowing(downloadItem2).getRemainingDownloadTime();
                will(returnValue(remaingDownloadTime2));

                allowing(downloadItem3).getLocalQueuePriority();
                will(returnValue(1));
            }
        });

        QueueTimeCalculator queueTimeCalculator = new QueueTimeCalculator(downloadItems);
        assertEquals(remaingDownloadTime2, queueTimeCalculator.getRemainingQueueTime(downloadItem3));

        context.assertIsSatisfied();
    }

    public void testGetRemainingQueueTime2ItemsDownloading2Queued() {
        Mockery context = new Mockery();

        final DownloadItem downloadItem1 = context.mock(DownloadItem.class);
        final DownloadItem downloadItem2 = context.mock(DownloadItem.class);
        final DownloadItem downloadItem3 = context.mock(DownloadItem.class);
        final DownloadItem downloadItem4 = context.mock(DownloadItem.class);

        EventList<DownloadItem> downloadItems = new BasicEventList<DownloadItem>();
        downloadItems.add(downloadItem1);
        downloadItems.add(downloadItem2);
        downloadItems.add(downloadItem3);
        downloadItems.add(downloadItem4);

        final long remaingDownloadTime1 = 500L;
        final long remaingDownloadTime2 = 200L;

        context.checking(new Expectations() {
            {
                allowing(downloadItem1).getState();
                will(returnValue(DownloadState.DOWNLOADING));
                allowing(downloadItem2).getState();
                will(returnValue(DownloadState.DOWNLOADING));
                allowing(downloadItem3).getState();
                will(returnValue(DownloadState.LOCAL_QUEUED));
                allowing(downloadItem4).getState();
                will(returnValue(DownloadState.LOCAL_QUEUED));

                allowing(downloadItem1).getRemainingDownloadTime();
                will(returnValue(remaingDownloadTime1));

                allowing(downloadItem2).getRemainingDownloadTime();
                will(returnValue(remaingDownloadTime2));

                allowing(downloadItem3).getLocalQueuePriority();
                will(returnValue(1));
                allowing(downloadItem4).getLocalQueuePriority();
                will(returnValue(2));
            }
        });

        QueueTimeCalculator queueTimeCalculator = new QueueTimeCalculator(downloadItems);
        assertEquals(remaingDownloadTime2, queueTimeCalculator.getRemainingQueueTime(downloadItem3));
        assertEquals(remaingDownloadTime1, queueTimeCalculator.getRemainingQueueTime(downloadItem4));

        context.assertIsSatisfied();
    }
}
