package org.limewire.core.impl.library;

import java.util.Arrays;

import org.jmock.Mockery;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.gnutella.tests.LimeTestCase;

import ca.odell.glazedlists.BasicEventList;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileDescStub;

public class LocalFileListImplTest extends LimeTestCase {

    private Mockery context;

    public LocalFileListImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
    }
    
    public void testAddAllFileDescs() throws Exception {

        LocalFileItem item1 = context.mock(LocalFileItem.class);
        LocalFileItem item2 = context.mock(LocalFileItem.class);
        
        BasicEventList<LocalFileItem> eventList = new BasicEventList<LocalFileItem>();
        FileDesc fileDesc1 = new FileDescStub("hello", URN.createSHA1Urn(UrnHelper.VALID_URN_STRINGS[0]), 1);
        fileDesc1.putClientProperty(LocalFileListImpl.FILE_ITEM_PROPERTY, item1);
        FileDesc fileDesc2 = new FileDescStub("world", URN.createSHA1Urn(UrnHelper.VALID_URN_STRINGS[1]), 2);
        fileDesc2.putClientProperty(LocalFileListImpl.FILE_ITEM_PROPERTY, item2);
    
        LocalFileListImpl localFileList = new LocalFileListImpl(eventList, null) {
            @Override
            protected FileCollection getCoreCollection() {
                return null;
            }
        };
        
        localFileList.addAllFileDescs(Arrays.asList(fileDesc1, fileDesc2));
        
        assertEquals(2, eventList.size());
        assertContains(eventList, item1);
        assertContains(eventList, item2);
    }
}
