package com.limegroup.gnutella.messagehandlers;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.settings.FilterSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.vendor.InspectionRequest;
import com.limegroup.gnutella.messages.vendor.InspectionResponse;
import com.limegroup.gnutella.messages.vendor.InspectionResponseFactory;
import com.limegroup.gnutella.stubs.ReplyHandlerStub;

public class InspectionRequestHandlerTest extends BaseTestCase {

    public InspectionRequestHandlerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(InspectionRequestHandlerTest.class);
    }
    
    private Mockery mockery;
    private Injector injector;
    private MessageRouter messageRouter;
    private SecureMessageVerifier verifier;
    private InspectionResponseFactory factory;
    
    @Override
    public void setUp() throws Exception {
        mockery = new Mockery();
        messageRouter = mockery.mock(MessageRouter.class);
        verifier = mockery.mock(SecureMessageVerifier.class);
        factory = mockery.mock(InspectionResponseFactory.class);
        Module m = new AbstractModule() {
            @Override
            public void configure() {
                bind(MessageRouter.class).toInstance(messageRouter);
                bind(InspectionResponseFactory.class).toInstance(factory);
                bind(SecureMessageVerifier.class).annotatedWith(Names.named("inspection")).toInstance(verifier);
            }
        };
        injector = LimeTestUtils.createInjector(m);
    }

    public void testForwardsToLeaves() throws Exception {
        FilterSettings.INSPECTOR_IP_ADDRESSES.set(new String[]{"1.2.3.4"});
        final InspectionRequest request = mockery.mock(InspectionRequest.class);
        final ReplyHandler handler = mockery.mock(ReplyHandler.class);
        final InspectionResponse resp = new InspectionResponse(1, new byte[16], new byte[0]);
        mockery.checking(new Expectations() {{
            one(handler).getAddress();
            will(returnValue("1.2.3.4"));
            one(messageRouter).forwardInspectionRequestToLeaves(request);
            one(factory).createResponses(request);
            will(returnValue(new InspectionResponse[]{resp}));
        }});
        InspectionRequestHandler irh = injector.getInstance(InspectionRequestHandler.class);
        irh.handleMessage(request, new InetSocketAddress("1.2.3.4",1), handler);
        mockery.assertIsSatisfied();
    }
    
    public void testSendsDelayed() throws Exception {
        FilterSettings.INSPECTOR_IP_ADDRESSES.set(new String[]{"1.2.3.4"});
        final InspectionRequest request = mockery.mock(InspectionRequest.class);
        final TestReplyHandler replyHandler = new TestReplyHandler();
        final InspectionResponse [] responses = new InspectionResponse[5];
        for (int i = 0; i < responses.length; i++)
            responses[i] = new TestInspectionResponse();
        
        mockery.checking(new Expectations() {{
            one(messageRouter).forwardInspectionRequestToLeaves(request);
            one(factory).createResponses(request);
            will(returnValue(responses));
            one(request).getSendInterval();
            will(returnValue(500));
        }});
        InspectionRequestHandler irh = injector.getInstance(InspectionRequestHandler.class);
        irh.handleMessage(request, new InetSocketAddress("1.2.3.4",1), replyHandler);
        mockery.assertIsSatisfied();
        
        Thread.sleep(responses.length * 600);
        synchronized(replyHandler) {
            assertEquals(responses.length, replyHandler.timestamps.size());
            assertEquals(responses.length, replyHandler.messages.size());
            
            for (int i = 0 ; i < responses.length ; i++)
                assertSame(responses[i],replyHandler.messages.get(i));
            
            long last = replyHandler.timestamps.get(0);
            for(int i = 1; i < responses.length; i++) {
                long current = replyHandler.timestamps.get(i);
                assertLessThanOrEquals(550, current - last);
                assertGreaterThanOrEquals(450, current - last);
                last = current;
            }
        }
    }
    
    public void testClearsPrevious() throws Exception {
        // prepare a bunch of responses
        FilterSettings.INSPECTOR_IP_ADDRESSES.set(new String[]{"1.2.3.4"});
        final InspectionRequest request = mockery.mock(InspectionRequest.class);
        final TestReplyHandler replyHandler = new TestReplyHandler();
        final InspectionResponse [] responses = new InspectionResponse[5];
        for (int i = 0; i < responses.length; i++)
            responses[i] = new TestInspectionResponse();
        
        mockery.checking(new Expectations() {{
            one(messageRouter).forwardInspectionRequestToLeaves(request);
            one(factory).createResponses(request);
            will(returnValue(responses));
            one(request).getSendInterval();
            will(returnValue(100));
        }});
        InspectionRequestHandler irh = injector.getInstance(InspectionRequestHandler.class);
        irh.handleMessage(request, new InetSocketAddress("1.2.3.4",1), replyHandler);
        mockery.assertIsSatisfied();
        
        // receive 3 out of 5 (1 right away, 2 delayed)
        Thread.sleep( 2 * 120);
       
        synchronized(replyHandler) {
            assertEquals(3, replyHandler.timestamps.size());
            assertEquals(3, replyHandler.messages.size());
        }
        
        // now send another inspection request
        final InspectionRequest emptyRequest = mockery.mock(InspectionRequest.class);
        mockery.checking(new Expectations(){{
            one(messageRouter).forwardInspectionRequestToLeaves(emptyRequest);
            one(factory).createResponses(emptyRequest);
            will(returnValue(new InspectionResponse[0]));
        }});
        irh.handleMessage(emptyRequest, new InetSocketAddress("1.2.3.4",1), replyHandler);
        mockery.assertIsSatisfied();
        
        // wait and wait.. no more responses from the last request should be received
        Thread.sleep(300);
        synchronized(replyHandler) {
            assertEquals(3, replyHandler.timestamps.size());
            assertEquals(3, replyHandler.messages.size());
        }
    }
    
    private static class TestReplyHandler extends ReplyHandlerStub {
        final List<Long> timestamps = new ArrayList<Long>();
        final List<Message> messages = new ArrayList<Message>();
        @Override
        public String getAddress() {
            return "1.2.3.4";
        }
        
        @Override
        public synchronized void reply(Message m) {
            timestamps.add(System.currentTimeMillis());
            messages.add(m);
        }
    }
    
    private static class TestInspectionResponse extends InspectionResponse {
        TestInspectionResponse() {
            super(1, new byte[16], new byte[0]);
        }
        
        @Override
        public boolean shouldBeSent() {
            return true;
        }
    }
}
