package org.limewire.core.impl.xmpp;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventBroadcaster;
import org.limewire.xmpp.activity.XmppActivityEvent;
import org.limewire.xmpp.activity.XmppActivityEvent.ActivityState;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
class IdleStatusMonitor {
    private static final int TWENTY_MINUTES_IN_MILLIS = 1200000;
    private final IdleTime idleTime;
    private final ScheduledExecutorService backgroundExecutor;
    private final EventBroadcaster<XmppActivityEvent> activityBroadcaster;
    private EventBean<FriendConnectionEvent> connectionEvent;

    @Inject
    public IdleStatusMonitor(@Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor, 
            IdleTime idleTime, EventBroadcaster<XmppActivityEvent> activityBroadcaster) {
        this.backgroundExecutor = backgroundExecutor;
        this.idleTime = idleTime;
        this.activityBroadcaster = activityBroadcaster;
    }

    @Inject void register(ServiceRegistry registry) {
        registry.register(new Service() {

            @Override
            public String getServiceName() {
                return "XMPP Idle Status timer";
            }

            @Override
            public void initialize() {}

            @Override
            public void start() {
                backgroundExecutor.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        FriendConnectionEvent lastEvent = connectionEvent.getLastEvent();
                        if (idleTime.supportsIdleTime() && lastEvent != null && lastEvent.getType().equals(FriendConnectionEvent.Type.CONNECTED)) {
                            if (idleTime.getIdleTime() > TWENTY_MINUTES_IN_MILLIS) {
                                activityBroadcaster.broadcast(new XmppActivityEvent(ActivityState.Idle));
                            } else {
                                activityBroadcaster.broadcast(new XmppActivityEvent(ActivityState.Active));
                            }
                        }
                    }
                }, 0, 1, TimeUnit.SECONDS);
            }

            @Override
            public void stop() {}
        });
    }
    
    @Inject void register(EventBean<FriendConnectionEvent> connectionSupport) {
        this.connectionEvent = connectionSupport;
    }
}
