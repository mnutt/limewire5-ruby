package org.limewire.facebook.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.limewire.facebook.service.settings.FacebookReportBugs;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendException;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.ExceptionUtils;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class PresenceListener implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(PresenceListener.class);
    
    private final FacebookFriendConnection connection;
    private final BuddyListResponseDeserializer deserializer = new BuddyListResponseDeserializer();
    private Set<String> lastOnlineFriends = new HashSet<String>();

    private final Provider<Boolean> reportBugs;

    @Inject
    PresenceListener(@Assisted FacebookFriendConnection connection,
            @FacebookReportBugs Provider<Boolean> reportBugs) {
        this.connection = connection;
        this.reportBugs = reportBugs;
    }
    
    public void run() {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("buddy_list", "1"));
        nvps.add(new BasicNameValuePair("notifications", "1"));
        nvps.add(new BasicNameValuePair("force_render", "true"));
        nvps.add(new BasicNameValuePair("user", connection.getUID()));
        nvps.add(new BasicNameValuePair("popped_out", "true"));
        
        try{
            LOG.debugf("posting buddy list request: {0}", nvps);
            String responseStr = connection.httpPOST("http://www.facebook.com", "/ajax/presence/update.php", nvps);
            if (responseStr == null) {
                LOG.debug("no response for buddy list post");
                throw new IOException("no buddy list response");
            } 
            LOG.debugf("buddy list response: {0}", responseStr);

            Set<String> onlineFriendIds = deserializer.parseOnlineFriendIds(responseStr);
            for (Friend friend : connection.getFriends()) {
                if (onlineFriendIds.contains(friend.getId())) {
                    connection.addPresence(friend.getId());
                } else if (lastOnlineFriends.contains(friend.getId())) {
                    try {
                        // the check implicitly removes the friend if he's offline
                        boolean online = connection.sendFriendIsOnline(friend.getId());
                        if (online) {
                            LOG.debugf("friend still online, overriding buddy list: {0}", friend);
                            // re-add friend to the set of online friends, so a recheck 
                            // happens the next time he's reported offline
                            onlineFriendIds.add(friend.getId());
                        } else {
                            LOG.debugf("buddy list was right, friend offline: {0}", friend);
                        }
                    } catch (FriendException e) {
                        LOG.debug("error checking online state", e);
                    }
                } else {
                    LOG.debugf("friend not online yet or buddy list out of date: {0}", friend);
                }
            }
            lastOnlineFriends = onlineFriendIds;
        } catch (JSONException e) {
            LOG.debug("error deserializing JSON response", e);
            if (reportBugs.get()) {
                ExceptionUtils.reportOrReturn(e);
            }
        } catch (IOException e) {
            LOG.debug("POST error", e);
            try {
                connection.reconnect();
            } catch (IOException e1) {
                LOG.debug("error reconnecting", e1);
                connection.logout();
            }
        }        
    }
 
}