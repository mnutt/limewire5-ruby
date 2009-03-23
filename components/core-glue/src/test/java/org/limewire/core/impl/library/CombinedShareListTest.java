package org.limewire.core.impl.library;

import java.io.File;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.util.BaseTestCase;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEventPublisher;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

public class CombinedShareListTest extends BaseTestCase {

    public CombinedShareListTest(String name) {
        super(name);
    }

    public void testCombinedShareList() throws Exception {
        Mockery context = new Mockery();

        final LocalFileItem localFileItem1 = context.mock(LocalFileItem.class);
        final LocalFileItem localFileItem2 = context.mock(LocalFileItem.class);
        final LocalFileItem localFileItem3 = context.mock(LocalFileItem.class);
        final LocalFileItem duplicateItem = context.mock(LocalFileItem.class);

        context.checking(new Expectations() {
            {
                allowing(localFileItem1).getFile();
                will(returnValue(new File("/tmp/1")));
                allowing(localFileItem2).getFile();
                will(returnValue(new File("/tmp/2")));
                allowing(localFileItem3).getFile();
                will(returnValue(new File("/tmp/3")));
                allowing(duplicateItem).getFile();
                will(returnValue(new File("/tmp/1")));

            }

        });

        EventList<LocalFileItem> masterList = new BasicEventList<LocalFileItem>();
        masterList.add(localFileItem1);

        ListEventPublisher listEventPublisher = masterList.getPublisher();
        ReadWriteLock readWriteLock = masterList.getReadWriteLock();

        CombinedShareList combinedShareList = new CombinedShareList(listEventPublisher,
                readWriteLock);

        assertEquals(0, combinedShareList.size());

        EventList<LocalFileItem> subList1 = combinedShareList.createMemberList();
        subList1.add(localFileItem1);

        assertEquals(0, combinedShareList.size());

        combinedShareList.addMemberList(subList1);
        assertEquals(1, combinedShareList.size());

        EventList<LocalFileItem> subList2 = combinedShareList.createMemberList();
        combinedShareList.addMemberList(subList2);

        subList2.add(localFileItem2);
        subList2.add(localFileItem3);
        assertEquals(3, combinedShareList.size());

        subList2.add(duplicateItem);
        assertEquals(3, combinedShareList.size());

        subList2.remove(duplicateItem);
        assertEquals(3, combinedShareList.size());

        subList2.remove(localFileItem3);
        assertEquals(2, combinedShareList.size());

        combinedShareList.removeMemberList(subList2);
        assertEquals(1, combinedShareList.size());

        subList1.remove(localFileItem1);
        assertEquals(0, combinedShareList.size());
    }
}
