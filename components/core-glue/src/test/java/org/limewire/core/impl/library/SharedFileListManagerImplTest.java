package org.limewire.core.impl.library;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Test;

import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.impl.tests.CoreGlueTestUtils;
import org.limewire.friend.api.Friend;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.library.FileCollectionManager;
import com.limegroup.gnutella.library.SharedFileCollection;

public class SharedFileListManagerImplTest extends LimeTestCase {
    
    @Inject private Injector injector;
    @Inject private SharedFileListManagerImpl listManager;
    @Inject private FileCollectionManager collectionManager;

    public SharedFileListManagerImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SharedFileListManagerImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        CoreGlueTestUtils.createInjectorAndStart(LimeTestUtils.createModule(this));
    }
    
    @Override
    protected void tearDown() throws Exception {
        injector.getInstance(LifecycleManager.class).shutdown();
    }
    
    public void testContainsDefaultCollection() throws Exception {
        assertEquals(listManager.getModel().toString(), 1, listManager.getModel().size());
        SharedFileList list = listManager.getModel().get(0);
        assertEquals(1, list.getFriendIds().size());
        assertEquals(Friend.P2P_FRIEND_ID, list.getFriendIds().get(0));
        assertEquals(0, list.size());
        assertEquals("Public Shared", list.getCollectionName());
        assertFalse(list.isNameChangeAllowed());
    }
    
    public void testCreatingAndModifyingList() {
        assertEquals(1, collectionManager.getSharedFileCollections().size());
        
        listManager.createNewSharedFileList("Test List");
        assertEquals(2, collectionManager.getSharedFileCollections().size());
        assertEquals(2, listManager.getModel().size());
        
        SharedFileList list = listManager.getModel().get(1);
        assertEquals("Test List", list.getCollectionName());
        assertEquals(0, list.getFriendIds().size());
        
        SharedFileCollection collection = collectionManager.getSharedFileCollections().get(1);
        assertEquals("Test List", collection.getName());
        assertEquals(0, collection.getFriendList().size());
        
        list.setCollectionName("List");
        assertEquals("List", list.getCollectionName());
        assertEquals("List", collection.getName());
        
        assertTrue(list.isNameChangeAllowed());
        
        list.addFriend("friend1");
        assertEquals(1, list.getFriendIds().size());
        assertEquals("friend1", list.getFriendIds().get(0));
        assertEquals(1, collection.getFriendList().size());
        assertEquals("friend1", collection.getFriendList().get(0));
        
        list.removeFriend("not a friend");
        assertEquals(1, list.getFriendIds().size());
        assertEquals(1, collection.getFriendList().size());
        
        list.removeFriend("friend1");
        assertEquals(0, list.getFriendIds().size());
        assertEquals(0, collection.getFriendList().size());    
    }
    
    public void testNameChangeTriggersEvent() {
        EventList<SharedFileList> model = listManager.getModel();
        listManager.createNewSharedFileList("Test List");
        assertEquals(2, model.size());
        
        final SharedFileList list = model.get(1);
        final AtomicBoolean triggered = new AtomicBoolean(false);
        
        ListEventListener<SharedFileList> listener = new ListEventListener<SharedFileList>() {
            @Override
            public void listChanged(ListEvent<SharedFileList> listChanges) {
                triggered.set(true);
                assertTrue(listChanges.next());
                assertEquals(ListEvent.UPDATE, listChanges.getType());
                assertEquals(1, listChanges.getIndex());
                assertSame(list, listChanges.getSourceList().get(1));
                assertFalse(listChanges.next());
            }
        };
        model.addListEventListener(listener);
        
        assertEquals("Test List", list.getCollectionName());
        list.setCollectionName("Another Name");
        
        assertTrue(triggered.get());
        assertEquals("Another Name", list.getCollectionName());
    }
    
    
}
