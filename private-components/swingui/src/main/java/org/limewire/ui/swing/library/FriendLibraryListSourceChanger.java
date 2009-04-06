package org.limewire.ui.swing.library;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.collection.glazedlists.PluggableList;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

/**
 * Creates a composite list of all libraries from all friends and all browse hosts.
 * By setting a friend, the composite list can be filtered down to show files of 
 * only a single person. 
 */
public class FriendLibraryListSourceChanger implements ListSourceChanger {

    private final PluggableList<RemoteFileItem> delegateList;    
    private final RemoteLibraryManager remoteLibraryManager;
    private final List<ListChangedListener> listeners = new CopyOnWriteArrayList<ListChangedListener>();
    
    private Friend currentFriend;
    private EventList<RemoteFileItem> filterList;
    
    public FriendLibraryListSourceChanger(PluggableList<RemoteFileItem> delegateList,
            RemoteLibraryManager remoteLibraryManager) {
        this.delegateList = delegateList;
        this.remoteLibraryManager = remoteLibraryManager;
    }
    
    public void addListener(ListChangedListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Listens for changes in the current friend library. If it changed, attempts to
     * replace the backing filter.
     */
    public void registerListeners() {
        remoteLibraryManager.getSwingFriendLibraryList().addListEventListener(new ListEventListener<FriendLibrary>(){
            @Override
            public void listChanged(ListEvent<FriendLibrary> listChanges) {
                while(listChanges.next()) {
                    EventList<FriendLibrary> item = listChanges.getSourceList();
                    for(FriendLibrary library : item) {
                        if(isFriendEqual(library.getFriend())) {
                            updateEventList();
                        }
                    }
                }
            }
        });
    }

    /**
     * Set the current friend to filter with. If no filtering is
     * to occur, set friend to null.
     * 
     * This will fire an event that the filter friend has changed, only
     * if the same friend was not previously being filtered on. 
     */
    public void setFriend(Friend friend) {
        // only replace the source list if the friend has changed
        if(!isFriendEqual(friend)) {
            currentFriend = friend;
            
            filterList = getSourceList(friend);

            delegateList.setSource(filterList);
            
            for(ListChangedListener listener : listeners) {
                listener.friendChanged(currentFriend);
            }
        } 
    }
    
    /**
     * Attempts to update the filter list if the friend remains the same
     * but the backing EventList has been changed.
     */
    private void updateEventList() {
        EventList<RemoteFileItem> remoteList = getSourceList(currentFriend);
        
        // if the current EventList for this friend and the list applied
        // to the filter are different, replace the filter with the new 
        // eventList. If they're the same, do nothing.
        if(remoteList != filterList) {
            filterList = remoteList;
            delegateList.setSource(filterList);
        }
    }
    
    /**
     * Returns an EventList for the given friend to filter with. This list can
     * safely be passed to the PluggableList.
     */
    private EventList<RemoteFileItem> getSourceList(Friend friend) {
        if(friend == null || friend.getId() == null) {
            return remoteLibraryManager.getAllFriendsFileList().getSwingModel();
        } else {
            FriendLibrary library = remoteLibraryManager.getFriendLibrary(friend);
            if(library != null)
                return library.getSwingModel();
            else // return an empty, pluggable safe list to use
                return delegateList.createSourceList();
        }
    }
    
    /**
     * Return true is this friend is equal to current friend,
     * false otherwise
     */
    private boolean isFriendEqual(Friend friend) {
        if(friend == null || currentFriend == null) {
            if(friend == null && currentFriend == null)
                return true;
            else
                return false;
        } else
            return friend.getId().equals(currentFriend.getId());
    }
    
    /**
     * Return the current friend that is being filtered on, null if no filtering is
     * occuring.
     */
    public Friend getCurrentFriend() {
        return currentFriend;
    }
}

