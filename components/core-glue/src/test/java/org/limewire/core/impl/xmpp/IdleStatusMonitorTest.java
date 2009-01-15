package org.limewire.core.impl.xmpp;


import junit.framework.TestCase;

import org.limewire.lifecycle.Service;
import org.limewire.listener.BroadcastPolicy;
import org.limewire.listener.CachingEventMulticasterImpl;
import org.limewire.listener.EventListener;
import org.limewire.xmpp.activity.XmppActivityEvent;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

public class IdleStatusMonitorTest extends TestCase {
    private MockIdleTime idleTime;
    private Runnable monitorRunnable;
    private CachingEventMulticasterImpl<XMPPConnectionEvent> connectionSupport;
    private TestXmppActivityListener listener;

    @Override
    protected void setUp() throws Exception {
        // TODO don't mock out env, especially the
        // TODO broadcasters
        MockScheduledExecutorService backgroundExecutor = new MockScheduledExecutorService();
        idleTime = new MockIdleTime();
        CachingEventMulticasterImpl<XmppActivityEvent> activityBroadcaster = new CachingEventMulticasterImpl<XmppActivityEvent>(BroadcastPolicy.IF_NOT_EQUALS); 
        listener = new TestXmppActivityListener();
        activityBroadcaster.addListener(listener);
        connectionSupport = new CachingEventMulticasterImpl<XMPPConnectionEvent>(BroadcastPolicy.IF_NOT_EQUALS);
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
        assertNull(listener.event);
    }

    public void testXMPPNotConnected() {
        idleTime.supportsIdleTimeReturn = true;
        monitorRunnable.run();
        assertNull("XmppActivityEvent fired, but there is no xmpp connection", listener.event);
        sendConnectionEvent(XMPPConnectionEvent.Type.DISCONNECTED);
        assertNull("XmppActivityEvent fired, but there is no xmpp connection", listener.event);
    }

    private void sendConnectionEvent(org.limewire.xmpp.api.client.XMPPConnectionEvent.Type connectionState) {
        connectionSupport.handleEvent(new XMPPConnectionEvent(new MockXMPPConnection2(), connectionState));
    }
    
    public void testIdleState() {
        idleTime.supportsIdleTimeReturn = true;
        sendConnectionEvent(XMPPConnectionEvent.Type.CONNECTED);
        
        idleTime.getIdleTimeReturn.add(0l);
        monitorRunnable.run();
        assertEquals(XmppActivityEvent.ActivityState.Active, listener.event.getSource());
        listener.clear();
        
        idleTime.getIdleTimeReturn.add(5l);
        monitorRunnable.run();
        assertNull(listener.event);
        
        idleTime.getIdleTimeReturn.add(Long.MAX_VALUE - 1);
        monitorRunnable.run();
        assertEquals(XmppActivityEvent.ActivityState.Idle, listener.event.getSource());
        listener.clear();
        
        idleTime.getIdleTimeReturn.add(Long.MAX_VALUE);
        monitorRunnable.run();
        assertNull(listener.event);
        
        idleTime.getIdleTimeReturn.add(0l);
        monitorRunnable.run();
        assertEquals(XmppActivityEvent.ActivityState.Active, listener.event.getSource());
        listener.clear();
        
        idleTime.getIdleTimeReturn.add(5l);
        monitorRunnable.run();
        assertNull(listener.event);
    }
    
   private static class TestXmppActivityListener implements EventListener<XmppActivityEvent> {
       private volatile XmppActivityEvent event;

       @Override
       public void handleEvent(XmppActivityEvent event) {
           this.event = event;
       }
       
       void clear() {
           event = null;
       }
   }
}
