package org.limewire.ui.swing.friend;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.limewire.core.settings.FacebookSettings;
import org.limewire.facebook.service.FacebookFriend;
import org.limewire.facebook.service.FacebookFriendConnection;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.http.httpclient.HttpClientInstanceUtils;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventUtils;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.limegroup.gnutella.library.SharedFileCollection;
import com.limegroup.gnutella.library.SharedFileCollectionChangeEvent;

/**
 * Listens for friends being added to a shared list and sends the friends
 * a notification if they're Facebook friends and they're online at the time.  
 */
@EagerSingleton
public class FacebookShareEventNotifier {

    private static final Log LOG = LogFactory.getLog(FacebookShareEventNotifier.class);
    
    private final EventBean<FriendConnectionEvent> friendConnectionEventBean;
    
    private final Set<String> notifiedFriends = new HashSet<String>();

    private final HttpClientInstanceUtils httpClientInstanceUtils;

    @Inject
    public FacebookShareEventNotifier(EventBean<FriendConnectionEvent> friendConnectionEventBean,
            HttpClientInstanceUtils httpClientInstanceUtils) {
        this.friendConnectionEventBean = friendConnectionEventBean;
        this.httpClientInstanceUtils = httpClientInstanceUtils;
    }
    
    @Inject
    void register(ListenerSupport<SharedFileCollectionChangeEvent> fileCollectionEventSupport,
            ListenerSupport<FriendConnectionEvent> friendConnectionEventSupport) {
        fileCollectionEventSupport.addListener(new EventListener<SharedFileCollectionChangeEvent>() {
            @Override
            public void handleEvent(SharedFileCollectionChangeEvent event) {
                switch (event.getType()) {
                case FRIEND_ADDED:
                    sendNotificationToFriend(event.getFriendId(), event.getSource());
                    break;
                case FRIEND_IDS_CHANGED:
                    Set<String> newFriends = new HashSet<String>(event.getNewFriendIds());
                    newFriends.removeAll(event.getOldFriendIds());
                    for (String friendId : newFriends) {
                        sendNotificationToFriend(friendId, event.getSource());
                    }
                    break;
                }
            }
        });
    }
    
    void sendNotificationToFriend(String friendId, SharedFileCollection collection) {
        LOG.debugf("notifying friend {0} about shared list {1}", friendId, collection);
        if (!FacebookSettings.SEND_SHARE_NOTIFICATIONS.getValue()) {
            LOG.debug("sending share notifications is turned off");
            return;
        }
        if (collection.size() == 0) {
            LOG.debug("collection is empty, no notification");
            return;
        }
        FriendConnection connection = EventUtils.getSource(friendConnectionEventBean);
        if (!(connection instanceof FacebookFriendConnection)) {
            LOG.debug("no connection or no facebook connection");
            return;
        }
        FacebookFriendConnection facebookFriendConnection = (FacebookFriendConnection)connection;
        FacebookFriend friend = facebookFriendConnection.getFriend(friendId);
        if (friend == null) {
            LOG.debugf("no friend: {0}", friendId);
            return;
        }
        if (friend.getPresences().isEmpty()) {
            LOG.debugf("no notification for offline friend: {0}", friend);
            return;
        }
        if (notifiedFriends.contains(friend.getId())) {
            LOG.debugf("friend was already notified of a share event this session: {0}", friend);
            return;
        }
        notifiedFriends.add(friend.getId());
        Locale locale = friend.getLocale();
        // build up args for message formatting
        List<Object> args = new ArrayList<Object>();
        for (String url : FacebookSettings.SHARE_LINK_URLS.get()) {
            args.add(MessageFormat.format("<a href=\"{0}\">", httpClientInstanceUtils.addClientInfoToUrl(url)));
            args.add("</a>");
        }
        args.add(collection.size());
        args.add(collection.getName());
        String[] texts = FacebookSettings.SHARE_NOTIFICATIONS_TEXTS.get();
        String notificationText = I18n.trln(locale, texts[0], texts[1], collection.size(), args.toArray());
        facebookFriendConnection.sendNotification(friend, notificationText);
    }
}
