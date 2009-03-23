package com.limegroup.gnutella;

import org.jmock.Mockery;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.limewire.inject.Providers;
import com.google.inject.Provider;

import org.limewire.util.BaseTestCase;
import junit.framework.Test;

import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;

/**
 * Tests the <tt>Pinger</tt> class that periodically sends pings to gather new
 * host data.
 */
public final class PingerTest extends BaseTestCase {

    private Mockery context = new Mockery();
    private Pinger pinger;
    
    private ScheduledExecutorService backgroundExecutor;
    private ConnectionServices       connectionServices;
    private Provider<MessageRouter>  messageRouterP;
    private MessageRouter            messageRouter;
    private PingRequestFactory       pingRequestFactory;
    private PingRequest              pingRequest;  
    

    public PingerTest(String name) {
        super(name);        
    }

    public static Test suite() {
        return buildTestSuite(PingerTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    public void setUp() {
 
        context = new Mockery();
        
        backgroundExecutor = context.mock(ScheduledExecutorService.class);
        connectionServices = context.mock(ConnectionServices.class);
        
        messageRouter      = context.mock(MessageRouter.class);
        messageRouterP     = Providers.of(messageRouter);
        
        pingRequestFactory = context.mock(PingRequestFactory.class);
        pingRequest        = context.mock(PingRequest.class);  
        
        pinger = new Pinger(backgroundExecutor,connectionServices, messageRouterP, pingRequestFactory);
        
        context.checking(new Expectations() {
            {
                atLeast(1).of(backgroundExecutor).scheduleWithFixedDelay(
                        with(any(Runnable.class)),
                        with(any(long.class)), 
                        with(any(long.class)),
                        with(any(TimeUnit.class)));
            }
        });
        
    }
    
    /**
     * Test to make sure that we're correctly sending out periodic pings
     * from the pinger as an Ultrapeer (we should not be sending these
     * periodic pings as a leaf).
     */
    public void testPeriodicUltrapeerPings() throws Exception {
 
        context.checking(new Expectations() {
            {
                atLeast(1).of(messageRouter).broadcastPingRequest(with(any(PingRequest.class)));
                allowing(connectionServices).isSupernode();
                will(returnValue(true));
                atLeast(1).of(pingRequestFactory).createPingRequest(with(any(byte.class)));
                will(returnValue(pingRequest));
            }
        });
        
        pinger.start();
        pinger.run();
        
        context.assertIsSatisfied();
        
    }

    /**
     * Test to make sure that we're not sending out periodic pings as a 
     * leaf.
     */
    public void testPeriodicLeafPings() throws Exception {
  
        context.checking(new Expectations() {
            {
                atMost(0).of(messageRouter).broadcastPingRequest(with(any(PingRequest.class)));
                allowing(connectionServices).isSupernode();
                will(returnValue(false));
            }
        });
              
        pinger.start();
        pinger.run();

        context.assertIsSatisfied();
        
    }
}
