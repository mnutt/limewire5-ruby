package org.limewire.core.impl.library;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.listener.EventListener;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.BaseTestCase;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.limegroup.gnutella.library.FileListChangedEvent;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.FriendFileList;

public class FriendFileListImplTest extends BaseTestCase {
    
    private Mockery context = null;

    private CoreLocalFileItemFactory coreLocalFileItemFactory = null;

    private FileManager fileManager = null;

    private CombinedShareList combinedShareList = null;

    private FriendFileListImpl friendFileListImpl = null;

    private FriendFileList testFileList = null;

    private EventList<LocalFileItem> subList = null;

    private AtomicReference<EventListener<FileListChangedEvent>> fileListChangeListener = null;

    public FriendFileListImplTest(String name) {
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

        coreLocalFileItemFactory = context.mock(CoreLocalFileItemFactory.class);
        fileManager = context.mock(FileManager.class);
        combinedShareList = context.mock(CombinedShareList.class);

        subList = new BasicEventList<LocalFileItem>();
        context.checking(new Expectations() {
            {
                one(combinedShareList).createMemberList();
                will(returnValue(subList));
                one(combinedShareList).addMemberList(subList);
            }
        });
        final String name = "name";
        friendFileListImpl = new FriendFileListImpl(coreLocalFileItemFactory, fileManager, name,
                combinedShareList);

        testFileList = context.mock(FriendFileList.class);

        fileListChangeListener = new AtomicReference<EventListener<FileListChangedEvent>>();

        context.checking(new Expectations() {
            {
                one(fileManager).getOrCreateFriendFileList(name);
                will(returnValue(testFileList));
                one(testFileList).addFileListListener(with(any(EventListener.class)));
                will(new AssignParameterAction<EventListener<FileListChangedEvent>>(
                        fileListChangeListener, 0));
                allowing(testFileList).getReadLock();
                will(returnValue(new ReentrantLock()));
                one(testFileList).iterator();
                will(returnValue(Collections.EMPTY_LIST.iterator()));
            }
        });

        friendFileListImpl.commit();

    }

    public void testDispose() {
        context.checking(new Expectations() {
            {
                one(combinedShareList).removeMemberList(subList);
                one(testFileList).removeFileListListener(fileListChangeListener.get());
            }
        });
        friendFileListImpl.dispose();
        context.assertIsSatisfied();
    }

    public void testGetCoreFileList() {
        assertEquals(testFileList, friendFileListImpl.getCoreFileList());
        context.assertIsSatisfied();
    }

    public void testClearCategory() {
        context.checking(new Expectations() {
            {
                one(testFileList).clearCategory(Category.AUDIO);
            }
        });
        friendFileListImpl.clearCategory(Category.AUDIO);
        context.assertIsSatisfied();
    }

    public void testAddSnapshotCategory() {
        context.checking(new Expectations() {
            {
                one(testFileList).addSnapshotCategory(Category.AUDIO);
            }
        });
        friendFileListImpl.addSnapshotCategory(Category.AUDIO);
        context.assertIsSatisfied();
    }

    public void testIsCategoryAutomaticallyAdded() {
        context.checking(new Expectations() {
            {
                one(testFileList).isAddNewAudioAlways();
                will(returnValue(true));
            }
        });
        assertTrue(friendFileListImpl.isCategoryAutomaticallyAdded(Category.AUDIO));
        context.checking(new Expectations() {
            {
                one(testFileList).isAddNewAudioAlways();
                will(returnValue(false));
            }
        });
        assertFalse(friendFileListImpl.isCategoryAutomaticallyAdded(Category.AUDIO));

        context.checking(new Expectations() {
            {
                one(testFileList).isAddNewImageAlways();
                will(returnValue(true));
            }
        });
        assertTrue(friendFileListImpl.isCategoryAutomaticallyAdded(Category.IMAGE));
        context.checking(new Expectations() {
            {
                one(testFileList).isAddNewImageAlways();
                will(returnValue(false));
            }
        });
        assertFalse(friendFileListImpl.isCategoryAutomaticallyAdded(Category.IMAGE));

        context.checking(new Expectations() {
            {
                one(testFileList).isAddNewVideoAlways();
                will(returnValue(true));
            }
        });
        assertTrue(friendFileListImpl.isCategoryAutomaticallyAdded(Category.VIDEO));
        context.checking(new Expectations() {
            {
                one(testFileList).isAddNewVideoAlways();
                will(returnValue(false));
            }
        });
        assertFalse(friendFileListImpl.isCategoryAutomaticallyAdded(Category.VIDEO));
        context.assertIsSatisfied();
    }

    public void testSetCategoryAutomaticallyAdded() {
        context.checking(new Expectations() {
            {
                one(testFileList).setAddNewAudioAlways(true);
            }
        });
        friendFileListImpl.setCategoryAutomaticallyAdded(Category.AUDIO, true);
        context.checking(new Expectations() {
            {
                one(testFileList).setAddNewAudioAlways(false);
            }
        });
        friendFileListImpl.setCategoryAutomaticallyAdded(Category.AUDIO, false);

        context.checking(new Expectations() {
            {
                one(testFileList).setAddNewImageAlways(true);
            }
        });
        friendFileListImpl.setCategoryAutomaticallyAdded(Category.IMAGE, true);
        context.checking(new Expectations() {
            {
                one(testFileList).setAddNewImageAlways(false);
            }
        });
        friendFileListImpl.setCategoryAutomaticallyAdded(Category.IMAGE, false);

        context.checking(new Expectations() {
            {
                one(testFileList).setAddNewVideoAlways(true);
            }
        });
        friendFileListImpl.setCategoryAutomaticallyAdded(Category.VIDEO, true);
        context.checking(new Expectations() {
            {
                one(testFileList).setAddNewVideoAlways(false);
            }
        });
        friendFileListImpl.setCategoryAutomaticallyAdded(Category.VIDEO, false);

        context.assertIsSatisfied();
    }

    public void testAddFile() {
        final File file1 = new File("file1");
        context.checking(new Expectations() {
            {
                one(testFileList).add(file1);
            }
        });
        friendFileListImpl.addFile(file1);
        context.assertIsSatisfied();
    }

    public void testRemoveFile() {
        final File file1 = new File("file1");
        context.checking(new Expectations() {
            {
                one(testFileList).remove(file1);
            }
        });
        friendFileListImpl.removeFile(file1);
        context.assertIsSatisfied();
    }

    public void testAddFolder() {
        final File folder1 = new File("folder1");
        context.checking(new Expectations() {
            {
                one(testFileList).addFolder(folder1);
            }
        });
        friendFileListImpl.addFolder(folder1);
        context.assertIsSatisfied();
    }

    public void testContainsFile() {
        final File file1 = new File("file1");
        context.checking(new Expectations() {
            {
                one(testFileList).contains(file1);
                will(returnValue(true));
            }
        });
        assertTrue(friendFileListImpl.contains(file1));

        final File file2 = new File("file2");
        context.checking(new Expectations() {
            {
                one(testFileList).contains(file2);
                will(returnValue(false));
            }
        });
        assertFalse(friendFileListImpl.contains(file2));
        context.assertIsSatisfied();
    }
}
