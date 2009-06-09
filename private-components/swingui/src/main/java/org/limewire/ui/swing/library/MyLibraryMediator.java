package org.limewire.ui.swing.library;

import java.io.File;

import org.limewire.collection.glazedlists.PluggableList;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.library.nav.NavMediator;

import com.google.inject.Inject;

@LazySingleton
public class MyLibraryMediator implements NavMediator<MyLibraryPanel> {

    private final MyLibraryFactory myLibraryFactory;
    private final PluggableList<LocalFileItem> baseLibraryList;
    private final LibraryListSourceChanger currentFriendFilterChanger;
    private MyLibraryPanel panel;
    
    @Inject
    public MyLibraryMediator(MyLibraryFactory myLibraryFactory, LibraryManager libraryManager, ShareListManager shareListManager) {
        this.myLibraryFactory = myLibraryFactory;
        
        baseLibraryList = new PluggableList<LocalFileItem>(libraryManager.getLibraryListEventPublisher(), libraryManager.getReadWriteLock());
        currentFriendFilterChanger = new LibraryListSourceChanger(baseLibraryList, libraryManager, shareListManager);
    }
    
    public Friend getCurrentFriend() {
        return currentFriendFilterChanger.getCurrentFriend();
    }
    
    public void showSharingState(Friend friend) {
        getComponent().showSharingState(friend);
    }
    
    public void showAllFiles() {
        getComponent().showAllFiles();
    }
    
    public void selectItem(URN urn, Catalog catalog) {
        getComponent().selectItem(urn, catalog);
    }
    
    public void selectItem(File file, Category category) {
        getComponent().selectItem(file, new Catalog(category));
    }
    
    public File getPreviousItem(File file, Catalog catalog) {
        return getComponent().getPreviousItem(file, catalog);
    }
    
    public File getNextItem(File file, Catalog catalog) {
        return getComponent().getNextItem(file, catalog);
    }
    
    @Override
    public MyLibraryPanel getComponent() {
        if(panel == null)
            panel = myLibraryFactory.createMyLibraryPanel(baseLibraryList, currentFriendFilterChanger);
        return panel;
    }
    
    public void addFriendListener(ListSourceChanger.ListChangedListener listener) {
        currentFriendFilterChanger.addListener(listener);
    }
}
