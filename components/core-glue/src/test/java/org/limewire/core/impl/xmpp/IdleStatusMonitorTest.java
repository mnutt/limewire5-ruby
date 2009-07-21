package org.limewire.core.impl.xmpp;


import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.lifecycle.Service;
import org.limewire.listener.BroadcastPolicy;
import org.limewire.listener.CachingEventMulticasterImpl;
import org.limewire.listener.EventListener;
import org.limewire.xmpp.activity.XmppActivityEvent;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;

public class IdleStatusMonitorTest extends TestCase {
    
    private Mockery context;
    private IdleTime idleTime;
    private Runnable monitorRunnable;
    private CachingEventMulticasterImpl<FriendConnectionEvent> connectionSupport;
    private TestXmppActivityListener listener;

    @Override
    protected void setUp() throws Exception {
        
        context = new Mockery();
        
        // TODO don't mock out env, especially the
        // TODO broadcasters
        MockScheduledExecutorService backgroundExecutor = new MockScheduledExecutorService();
        idleTime = context.mock(IdleTime.class);
        CachingEventMulticasterImpl<XmppActivityEvent> activityBroadcaster = new CachingEventMulticasterImpl<XmppActivityEvent>(BroadcastPolicy.IF_NOT_EQUALS); 
        listener = new TestXmppActivityListener();
        activityBroadcaster.addListener(listener);
        connectionSupport = new CachingEventMulticasterImpl<FriendConnectionEvent>(BroadcastPolicy.IF_NOT_EQUALS);
        IdleStatusMonitor monitor = new IdleStatusMonitor(backgroundExecutor, idleTime, activityBroadcaster);
        MockServiceRegistry registry = new MockServiceRegistry();
        
        //trigger the monitoring code
        monitor.register(registry);
        monitor.register(connectionSupport);
        Service service = registry.registeredService;
        assertNotNull(service.getServiceName());
        service.start();
        
        monitorRunnable = backgroundExecutor.scheduleAtFixedRateCommand;
    }

    public void testIdleTimeNotSupported() {        
        
        context.checking(new Expectations() {
            { 
                allowing(idleTime).supportsIdleTime();
                will(returnValue(false));
                
            }});
        
        monitorRunnable.run();
        assertNull(listener.event);
    }

    public void testXMPPNotConnected() {
        
        context.checking(new Expectations() {
            { 
                allowing(idleTime).supportsIdleTime();
                will(returnValue(true));
                
            }});
        
        monitorRunnable.run();
        assertNull("XmppActivityEvent fired, but there is no xmpp connection", listener.event);
        sendConnectionEvent(FriendConnectionEvent.Type.DISCONNECTED);
        assertNull("XmppActivityEvent fired, but there is no xmpp connection", listener.event);
    }

    private void sendConnectionEvent(final org.limewire.friend.api.FriendConnectionEvent.Type connectionState) {
        
        Mockery context = new Mockery();
        final FriendConnection connection = context.mock(FriendConnection.class);
        context.checking(new Expectations() {{
            allowing(connection).isLoggedIn();
            will(returnValue(connectionState != FriendConnectionEvent.Type.DISCONNECTED));
            allowing(connection).isLoggingIn();
            will(returnValue(false));
        }});
        
        connectionSupport.handleEvent(new FriendConnectionEvent(connection, connectionState));
    }
    
    public void testIdleState() {
        
        context.checking(new Expectations() {
            { 
                allowing(idleTime).supportsIdleTime();
                will(returnValue(true));
                
                one(idleTime).getIdleTime();
                will(returnValue(0l));
                one(idleTime).getIdleTime();
                will(returnValue(5l));
                one(idleTime).getIdleTime();
                will(returnValue(Long.MAX_VALUE - 1));
                one(idleTime).getIdleTime();
                will(returnValue(Long.MAX_VALUE));
                one(idleTime).getIdleTime();
                will(returnValue(0l));
                one(idleTime).getIdleTime();
                will(returnValue(5l));
            }});
        
        sendConnectionEvent(FriendConnectionEvent.Type.CONNECTED);
        
        monitorRunnable.run();
        assertEquals(XmppActivityEvent.ActivityState.Active, listener.event.getSource());
        listener.clear();
        
        monitorRunnable.run();
        assertNull(listener.event);
        
        monitorRunnable.run();
        assertEquals(XmppActivityEvent.ActivityState.Idle, listener.event.getSource());
        listener.clear();
        
        monitorRunnable.run();
        assertNull(listener.event);
        
        monitorRunnable.run();
        assertEquals(XmppActivityEvent.ActivityState.Active, listener.event.getSource());
        listener.clear();
        
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
