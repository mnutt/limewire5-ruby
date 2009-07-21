package org.limewire.ui.swing.friends.chat;

import java.net.URL;

import org.limewire.friend.api.FriendPresence;


class ChatFriendsUtil {
    
    public static String getIconURL(FriendPresence.Mode mode) {
        switch(mode) {
        case available:
            return getURL("/org/limewire/ui/swing/mainframe/resources/icons/friends/available.png");
        case dnd:
            return getURL("/org/limewire/ui/swing/mainframe/resources/icons/friends/doNotDisturb.png");
        }
        return getURL("/org/limewire/ui/swing/mainframe/resources/icons/friends/away.png");
    }
    
    private static String getURL(String path) {
        URL resource = ChatFriendsUtil.class.getResource(path);
        return resource != null ? resource.toExternalForm() : "";
    }
}
