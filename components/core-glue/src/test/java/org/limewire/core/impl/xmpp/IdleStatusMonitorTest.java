package org.limewire.core.impl.xmpp;


import junit.framework.TestCase;

import org.limewire.lifecycle.Service;
import org.limewire.xmpp.activity.XmppActivityEvent;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

public class IdleStatusMonitorTest extends TestCase {
    private MockIdleTime idleTime;
    private Runnable monitorRunnable;
    private MockActivityEventBroadcaster activityBroadcaster;
    private MockListenerSupport connectionSupport;

    @Override
    protected void setUp() throws Exception {
        MockScheduledExecutorService backgroundExecutor = new MockScheduledExecutorService();
        idleTime = new MockIdleTime();
        activityBroadcaster = new MockActivityEventBroadcaster();
        connectionSupport = new MockListenerSupport();
        IdleStatusMonitor monitor = new IdleStatusMonitor(backgroundExecutor, idleTime, activityBroadcaster);
        MockServiceRegistry registry = new MockServiceRegistry();
        
        //trigger the monitoring code
        monitor.register(registry);
        monitor.register(connectionSupport);
        Service service = registry.registeredService;
        service.start();
        monitorRunnable = backgroundExecutor.scheduleAtFixedRateCommand;
    }

    public void testIdleTimeNotSupported() {
        idleTime.supportsIdleTimeReturn = false;
        monitorRunnable.run();
        assertNull("No event ever fired if system does not support idle time", activityBroadcaster.event);
    }

    public void testXMPPNotConnected() {
        idleTime.supportsIdleTimeReturn = true;
        monitorRunnable.run();
        assertNull("No event ever fired if system does not support idle time", activityBroadcaster.event);
        sendConnectionEvent(XMPPConnectionEvent.Type.DISCONNECTED);
        assertNull("No event ever fired if system does not support idle time", activityBroadcaster.event);
    }

    private void sendConnectionEvent(org.limewire.xmpp.api.client.XMPPConnectionEvent.Type connectionState) {
        connectionSupport.listener.handleEvent(new XMPPConnectionEvent(new MockXMPPConnection2(), connectionState));
    }
    
    public void testIdleStateIfInactiveAfter20Min() {
        idleTime.supportsIdleTimeReturn = true;
        sendConnectionEvent(XMPPConnectionEvent.Type.CONNECTED);
        
        idleTime.getIdleTimeReturn.add(0l);
        monitorRunnable.run();
        assertNull("No event broadcast if hasn't been idle before", activityBroadcaster.event);
        
        idleTime.getIdleTimeReturn.add(Long.MAX_VALUE);
        monitorRunnable.run();
        assertEquals(XmppActivityEvent.ActivityState.Idle, activityBroadcaster.event.getSource());
        
        idleTime.getIdleTimeReturn.add(0l);
        monitorRunnable.run();
        assertEquals(XmppActivityEvent.ActivityState.Active, activityBroadcaster.event.getSource());
    }
}
