package com.limegroup.gnutella;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.GUID;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.limegroup.gnutella.connection.ConnectionCapabilities;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.util.MessageTestUtils;
import com.limegroup.gnutella.util.TestConnectionManager;

/**
 * Temporary test class to ensure that MessageRouterImpl works as before
 * after refactoring. Test cases should be moved to new top level handler
 * classes eventually.
 */
public class MessageRouterImplRefactoringTest extends BaseTestCase {

    private Mockery context;
//    private TestConnectionFactory testConnectionFactory;
    private MessageRouterImpl messageRouterImpl;
//    private TestConnectionManager connectionManager;

    public MessageRouterImplRefactoringTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
    }

    /**
     * Helper method to configure the injector and return it. It also sets 
     * 
     * Neeeded as each test method needs its own participants. 
     */
    private Injector createInjectorAndInitialize(Module... modules) {
        Injector injector = LimeTestUtils.createInjector(modules);
//        testConnectionFactory = injector.getInstance(TestConnectionFactory.class);
        messageRouterImpl = (MessageRouterImpl) injector.getInstance(MessageRouter.class);
        messageRouterImpl.start();
        return injector;
    }
    
    /**
     * Calls {@link #createInjectorAndInitialize(Module...)} and with a module
     * that sets up the TestConnectionManager. 
     */
    private Injector createDefaultInjector(Module... modules) {
        Module m = new AbstractModule() {
            @Override
            protected void configure() {
                bind(ConnectionManager.class).to(TestConnectionManager.class);
            }
        };
        List<Module> list = new ArrayList<Module>();
        list.addAll(Arrays.asList(modules));
        list.add(m);
        Injector injector = createInjectorAndInitialize(list.toArray(new Module[0]));
//        connectionManager = (TestConnectionManager)injector.getInstance(ConnectionManager.class);
        return injector;
    }
    
    
    /**
     * Tests if tcp push requests are correctly handled before and after refactoring.
     */
    public void testHandlePushRequestTCP() {
        createDefaultInjector();
        final PushRequest pushRequest = context.mock(PushRequest.class);
        final ReplyHandler senderHandler = context.mock(ReplyHandler.class);
        final GUID guid = new GUID  ();
        context.checking(new Expectations() {{
            allowing(pushRequest).getClientGUID();
            will(returnValue(guid.bytes()));
            one(senderHandler).countDroppedMessage();
        }});
        context.checking(MessageTestUtils.createDefaultMessageExpectations(pushRequest, PushRequest.class));
        
        // test without installed reply handler
        messageRouterImpl.handleMessage(pushRequest, senderHandler);
        context.assertIsSatisfied();
        
        final ReplyHandler replyHandler = context.mock(ReplyHandler.class);
        context.checking(new Expectations() {{
            allowing(replyHandler).isOpen();
            will(returnValue(true));
            one(replyHandler).handlePushRequest(with(same(pushRequest)), with(same(senderHandler)));
        }});
        
        messageRouterImpl.getPushRouteTable().routeReply(guid.bytes(), replyHandler);
        
        // test with installed reply handler
        messageRouterImpl.handleMessage(pushRequest, senderHandler);
        context.assertIsSatisfied();
    }
    
    /**
     * Tests if udp/multicast push requests are correctly handled before and after refactoring.
     */
    public void testhandlePushRequestUDPMulticast() {
        final UDPReplyHandlerCache udpReplyHandlerCache = context.mock(UDPReplyHandlerCache.class);
        createDefaultInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(UDPReplyHandlerCache.class).toInstance(udpReplyHandlerCache);
            }
        });
        
        final PushRequest pushRequest = context.mock(PushRequest.class);
        final ReplyHandler senderHandler = context.mock(ReplyHandler.class);
        final GUID guid = new GUID  ();
        final InetSocketAddress address = InetSocketAddress.createUnresolved("127.0.0.1", 4545);
        context.checking(new Expectations() {{
            allowing(pushRequest).getClientGUID();
            will(returnValue(guid.bytes()));
            one(senderHandler).countDroppedMessage();
            allowing(udpReplyHandlerCache).getUDPReplyHandler(with(any(InetSocketAddress.class)));
            will(returnValue(senderHandler));
        }});
        context.checking(MessageTestUtils.createDefaultMessageExpectations(pushRequest, PushRequest.class));
        
        // test without installed reply handler
        messageRouterImpl.handleUDPMessage(pushRequest, address);
        context.assertIsSatisfied();
        
        // same for multicast
        messageRouterImpl.handleMulticastMessage(pushRequest, address);
        context.assertIsSatisfied();
        
        final ReplyHandler replyHandler = context.mock(ReplyHandler.class);
        context.checking(new Expectations() {{
            allowing(replyHandler).isOpen();
            will(returnValue(true));
            one(replyHandler).handlePushRequest(with(same(pushRequest)), with(same(senderHandler)));
        }});
        
        messageRouterImpl.getPushRouteTable().routeReply(guid.bytes(), replyHandler);
        
        // test with installed reply handler
        messageRouterImpl.handleUDPMessage(pushRequest, address);
        context.assertIsSatisfied();
        
        // same for multicast
        messageRouterImpl.handleMulticastMessage(pushRequest, address);
        context.assertIsSatisfied();
    }
    
    public void testConnectionsAreRemoved() {
        final QueryDispatcher queryDispatcher = context.mock(QueryDispatcher.class);
        Injector injector = createDefaultInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(QueryDispatcher.class).toInstance(queryDispatcher);
            }
        });
        ConnectionManager connectionManager = injector.getInstance(ConnectionManager.class);
        final RoutedConnection connection = context.mock(RoutedConnection.class);
        final ConnectionCapabilities connectionCapabilities = context.mock(ConnectionCapabilities.class);
        
        context.checking(new Expectations() {{
            allowing(connection).getConnectionCapabilities();
            will(returnValue(connectionCapabilities));
            allowing(connection).getAddress();
            will(returnValue("127.0.0.1"));
            ignoring(connection);
            ignoring(connectionCapabilities);
            one(queryDispatcher).removeReplyHandler(with(same(connection)));
        }});
        
        connectionManager.remove(connection);
        context.assertIsSatisfied();
    }
}
