package org.limewire.core.impl.library;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryData;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEventPublisher;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

import com.google.inject.Singleton;

@Singleton
public class MockLibraryManager implements SharedFileListManager, LibraryManager {

    private FileListAdapter allFileList;

    private FriendFileListAdapter gnutellaList;

    private FriendFileListAdapter friendList;

    private LibraryData libraryData;

    private Map<String, LocalFileList> friendMap;

    public MockLibraryManager() {
        allFileList = new FileListAdapter();
        gnutellaList = new FriendFileListAdapter();
        friendList = new FriendFileListAdapter();
        libraryData = new LibraryDataAdapter();

        friendMap = new HashMap<String, LocalFileList>();

        initializeMockGnutellaData();
        initializeMockFriendData();
        initializeFriends();
    }

    @Override
    public ListEventPublisher getLibraryListEventPublisher() {
        return allFileList.getModel().getPublisher();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return allFileList.getModel().getReadWriteLock();
    }

    private void initializeMockGnutellaData() {
        // MockLocalFileItem item = new MockLocalFileItem("Big Town Hero.mp3",
        // 1000,12345,23456, 5,1, Category.AUDIO);
        // gnutellaList.addFileItem(item);
        // allFileList.addFileItem(item);

        MockLocalFileItem item = new MockLocalFileItem("Pictures.jpg", 12345, 12312, 534512, 3, 2,
                Category.IMAGE);
        gnutellaList.addFileItem(item);
        allFileList.addFileItem(item);

        // item = new MockLocalFileItem("LimeWireStore.html", 32423, 3415412,
        // 123123,0,0, Category.DOCUMENT);
        // gnutellaList.addFileItem(item);
        // allFileList.addFileItem(item);

        // item = new MockLocalFileItem("Apu Cannon ball.avi", 32423, 3415412,
        // 123123,0,0, Category.VIDEO);
        // gnutellaList.addFileItem(item);
        // allFileList.addFileItem(item);

        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    return;
                }
                MockLocalFileItem item = new MockLocalFileItem("Lazy load.bmp", 32423, 3415412,
                        123123, 0, 0, Category.IMAGE);
                gnutellaList.addFileItem(item);
                allFileList.addFileItem(item);
            }
        });
        t.start();
    }

    private void initializeMockFriendData() {
        MockLocalFileItem item = new MockLocalFileItem("Small Town Hero.mp3", 1000, 12345, 23456,
                1, 1, Category.AUDIO);
        friendList.addFileItem(item);
        allFileList.addFileItem(item);

        item = new MockLocalFileItem("LimeWire4.18.exe", 12345, 12312, 534512, 5, 0,
                Category.PROGRAM);
        friendList.addFileItem(item);
        allFileList.addFileItem(item);

        item = new MockLocalFileItem("Pictures_SanFrancisco.zip", 32423, 3415412, 123123, 0, 0,
                Category.OTHER);
        friendList.addFileItem(item);
        allFileList.addFileItem(item);
    }

    private void initializeFriends() {
        friendMap.put("Sean", new FileListAdapter());
        initializeFriend4(friendMap.get("Sean"));
        friendMap.put("Bob", new FileListAdapter());
        friendMap.put("Johanna", new FileListAdapter());
        friendMap.put("Mark", new FileListAdapter());
        initializeFriend(friendMap.get("Mark"));
        friendMap.put("Rob", new FileListAdapter());
        initializeFriend2(friendMap.get("Rob"));
    }

    private void initializeFriend(FileList fileList) {
        FileListAdapter files = (FileListAdapter) fileList;
        files.addFileItem(allFileList.getModel().get(0));
    }

    private void initializeFriend2(FileList fileList) {
        FileListAdapter files = (FileListAdapter) fileList;
        files.addFileItem(allFileList.getModel().get(0));
        files.addFileItem(allFileList.getModel().get(2));
    }

    private void initializeFriend4(FileList fileList) {
        FileListAdapter files = (FileListAdapter) fileList;
        files.addFileItem(allFileList.getModel().get(0));
        files.addFileItem(allFileList.getModel().get(1));
        files.addFileItem(allFileList.getModel().get(2));
        files.addFileItem(allFileList.getModel().get(3));
    }

    @Override
    public LibraryFileList getLibraryManagedList() {
        return allFileList;
    }

    @Override
    public LibraryData getLibraryData() {
        return libraryData;
    }

    private class LibraryDataAdapter implements LibraryData {

        @Override
        public boolean isFileManageable(File f) {
            return true;
        }

        @Override
        public boolean isProgramManagingAllowed() {
            return false;
        }

        @Override
        public Collection<Category> getManagedCategories() {
            return EnumSet.allOf(Category.class);
        }

        @Override
        public boolean isDirectoryAllowed(File folder) {
            return folder.isDirectory();
        }

        @Override
        public Collection<String> getDefaultExtensions() {
            return new ArrayList<String>();
        }

        @Override
        public Map<Category, Collection<String>> getExtensionsPerCategory() {
            return new HashMap<Category, Collection<String>>();
        }

        @Override
        public void setManagedExtensions(Collection<String> extensions) {
        }

        @Override
        public void setCategoriesToIncludeWhenAddingFolders(Collection<Category> managedCategories) {
            
        }
    }

    @Override
    public int createNewSharedFileList(String name) {
        return -1;
    }

    private final BasicEventList<SharedFileList> model = new BasicEventList<SharedFileList>();
    @Override
    public EventList<SharedFileList> getModel() {
        return model;
    }

    @Override
    public void deleteSharedFileList(SharedFileList list) {

    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public int getSharedFileCount() {
        return 0;
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {

    }

    @Override
    public void removeDocumentsFromPublicLists() {
        
    }
}
