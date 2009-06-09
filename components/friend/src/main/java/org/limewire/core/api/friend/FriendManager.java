package org.limewire.core.api.friend;

import java.util.Collection;

/**
 * Retrieves relevant information about a Friend.
 */
public interface FriendManager {

    /**
     * If there's a Friend for this ID, returns the most relevant FriendPresence. If there
     * is no Friend associated with this ID or no FriendPresence associated with the
     * Friend of this ID, null is returned. The most relevant FriendPresence is one
     * containing LW Features.
     */
    public FriendPresence getMostRelevantFriendPresence(String id);
    
    /** 
     * Returns true if a Friend exists for this ID and they are  
     * available, false otherwise. 
     */ 
    public boolean containsAvailableFriend(String id);
    
    /**
	 * Returns a Collection of all known friends.
	 */
    public Collection<Friend> getKnownFriends();
}
