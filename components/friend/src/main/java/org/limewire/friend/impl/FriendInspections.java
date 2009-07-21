package org.limewire.friend.impl;

import java.util.Map;
import java.util.HashMap;

import com.google.inject.Inject;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.LimewireFeature;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionPoint;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectionHistogram;
import org.limewire.inject.LazySingleton;

/**
 * Friend feature uasge inspections.
 */
@LazySingleton
public class FriendInspections {
    
    
    // used for inspecting stats of most recent chat connection
    private EventBean<FriendConnectionEvent> connectionEventBean;


    @SuppressWarnings("unused")
    @InspectableContainer
    private class LazyInspectableContainer {
        
        // keeping previous inspection point name for backward compatibility
        @InspectionPoint("xmpp service")
        private final Inspectable inspectable = new Inspectable() {
            @Override
            public Object inspect() {
                FriendConnection conn = EventUtils.getSource(connectionEventBean);
                boolean isLoggedIn = (conn != null) && conn.isLoggedIn();
                Map<Object, Object> map = new HashMap<Object, Object>();
                map.put("loggedIn", isLoggedIn);
                collectFriendStatistics(map, conn);    
                return map;
            }
            
            private void collectFriendStatistics(Map<Object, Object> data, FriendConnection connection) {
                int count = 0;
                InspectionHistogram<Integer> presencesHistogram = new InspectionHistogram<Integer>();
                if (connection != null) {
                    for (Friend user : connection.getFriends()) {
                        Map<String, FriendPresence> presences = user.getPresences();
                        presencesHistogram.count(presences.size());
                        for (FriendPresence presence : presences.values()) {
                            if (presence.hasFeatures(LimewireFeature.ID)) {
                                count++;
                                // break from inner presence loop, count each user only once
                                break;
                            }
                        }
                    }
                }
                data.put("limewire friends", count);
                data.put("presences", presencesHistogram.inspect());
            }
        };
    }
    
    @Inject
    public FriendInspections(EventBean<FriendConnectionEvent> connectionEventBean) {
        this.connectionEventBean = connectionEventBean;  
    }
}