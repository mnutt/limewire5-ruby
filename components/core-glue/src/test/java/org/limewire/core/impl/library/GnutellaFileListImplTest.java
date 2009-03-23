package org.limewire.core.impl.library;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.listener.EventListener;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.limegroup.gnutella.library.GnutellaFileList;

public class GnutellaFileListImplTest extends TestCase {

    @SuppressWarnings("unchecked")
    public void testRemoveDocuments() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final CoreLocalFileItemFactory coreLocalFileItemFactory = context
                .mock(CoreLocalFileItemFactory.class);
        final GnutellaFileList gnutellaFileList = context.mock(GnutellaFileList.class);
        final CombinedShareList combinedShareList = context.mock(CombinedShareList.class);

        final EventList<LocalFileItem> memberList = new BasicEventList<LocalFileItem>();

        context.checking(new Expectations() {
            {
                one(combinedShareList).createMemberList();
                will(returnValue(memberList));
                one(combinedShareList).addMemberList(memberList);
                one(gnutellaFileList).addFileListListener(with(any(EventListener.class)));
            }
        });
        GnutellaFileListImpl gnutellaFileListImpl = new GnutellaFileListImpl(
                coreLocalFileItemFactory, gnutellaFileList, combinedShareList);

        context.checking(new Expectations() {
            {
                one(gnutellaFileList).removeDocuments();
            }
        });

        gnutellaFileListImpl.removeDocuments();

        context.assertIsSatisfied();
    }

}
