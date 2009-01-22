package org.limewire.ui.swing.library.sharing;

import java.util.Collections;
import java.util.Map;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.ui.swing.util.I18n;

public class SharingTarget {
    
    private final Friend friend;
    
    public final static SharingTarget GNUTELLA_SHARE = new SharingTarget(new Gnutella());

    public SharingTarget(Friend friend){
        this.friend = friend;
    }

    public Friend getFriend() {
        return friend;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((friend == null) ? 0 : friend.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null){
            return false;
        }
        
        if (!(obj instanceof SharingTarget)) {            
                return false;
        }

        return getFriend().getId().equals(((SharingTarget)obj).getFriend().getId());
    }
    
    public boolean isGnutellaNetwork(){
        return this == GNUTELLA_SHARE;
    }
    
    @Override
    public String toString(){
        return getFriend().getRenderName();
    }

    private static class Gnutella implements Friend {
        @Override
        public boolean isAnonymous() {
            return true;
        }
        
        @Override
        public String getId() {
            return Friend.P2P_FRIEND_ID;
        }

        @Override
        public String getName() {
            return I18n.tr("P2P Network");
        }
        
        @Override
        public String getRenderName() {
            return getName();
        }

        public void setName(String name) {
            
        }

        @Override
        public Network getNetwork() {
            return null;
        }

        public Map<String, FriendPresence> getFriendPresences() {
            return Collections.emptyMap();
        }

        @Override
        public String getFirstName() {
            return getName();
        }
    }

}
