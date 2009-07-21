package org.limewire.core.impl.friend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.impl.friend.FriendFirewalledAddressConnector.PushedSocketConnectObserver;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.ConnectBackRequestFeature;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.friend.impl.address.FriendAddressResolver;
import org.limewire.friend.impl.address.FriendFirewalledAddress;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.net.ConnectBackRequest;
import org.limewire.net.SocketsManager;
import org.limewire.net.address.FirewalledAddress;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.observer.ConnectObserver;
import org.limewire.rudp.AbstractNBSocketChannel;
import org.limewire.rudp.UDPSelectorProvider;
import org.limewire.util.BaseTestCase;
import org.limewire.util.MatchAndCopy;

import com.google.inject.Provider;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.SocketProcessor;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.downloader.PushedSocketHandlerRegistry;

public class FriendFirewalledAddressConnectorTest extends BaseTestCase {

    public FriendFirewalledAddressConnectorTest(String name) {
        super(name);
    }

    /** 
     * Test that ensures the register methods will correctly link the connector to the sockets manager and socket
     *  handler register.
     */
    public void testRegister() {

        Mockery context = new Mockery();
        
        final SocketsManager socketsManager = context.mock(SocketsManager.class);
        final PushedSocketHandlerRegistry pushedSocketHandlerRegistry = context.mock(PushedSocketHandlerRegistry.class);
        
        final FriendFirewalledAddressConnector connector 
            = new FriendFirewalledAddressConnector(null, null, null, null, null, null);
        
        context.checking(new Expectations() {
            {   exactly(1).of(socketsManager).registerConnector(connector);
                exactly(1).of(pushedSocketHandlerRegistry).register(connector);
            }});
        
        connector.register(socketsManager);
        connector.register(pushedSocketHandlerRegistry);
        
        context.assertIsSatisfied();
        
    }
    
    /**
     * Tries the can connect function under various conditions.
     */
    public void testCanConnect() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }};
        
        final PushDownloadManager pushDownloadManager = context.mock(PushDownloadManager.class);
                
        final Address adressNotXmpp = context.mock(Address.class);
        final FriendFirewalledAddress adressCantConnect = context.mock(FriendFirewalledAddress.class);
        final FriendFirewalledAddress adressCanConnect = context.mock(FriendFirewalledAddress.class);
        
        final Address fwAddressCantConnect = context.mock(FirewalledAddress.class);
        final Address fwAddressCanConnect = context.mock(FirewalledAddress.class);
        
        final FriendFirewalledAddressConnector connector 
            = new FriendFirewalledAddressConnector(null, pushDownloadManager, null, null, null, null);
        
        context.checking(new Expectations() {
            {   allowing(adressCantConnect).getFirewalledAddress();
                will(returnValue(fwAddressCantConnect));
                allowing(adressCanConnect).getFirewalledAddress();
                will(returnValue(fwAddressCanConnect));
                
                atLeast(1).of(pushDownloadManager).canConnect(fwAddressCantConnect);
                will(returnValue(false));
                atLeast(1).of(pushDownloadManager).canConnect(fwAddressCanConnect);
                will(returnValue(true));
            }});
        
        assertFalse(connector.canConnect(adressNotXmpp));
        assertFalse(connector.canConnect(adressCantConnect));
        assertTrue(connector.canConnect(adressCanConnect));
        
        context.assertIsSatisfied();
        
    }
    
    /**
     * Ensure ready sockets are delegated to the correct observers. 
     */
    public void testAcceptPushedSocket() throws IOException {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }};
        
            
        final InetAddress inetAddress = context.mock(InetAddress.class);
            
        final ConnectObserver connectObserverA = context.mock(ConnectObserver.class);    
        final FirewalledAddress fwAddressA = context.mock(FirewalledAddress.class);
        final byte[] guidA = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                16 };
        final Socket socketA = context.mock(Socket.class);
        final Connectable connectableA = context.mock(Connectable.class);

        final ConnectObserver connectObserverB = context.mock(ConnectObserver.class);    
        final FirewalledAddress fwAddressB = context.mock(FirewalledAddress.class);
        final byte[] guidB = new byte[] { 1, 2, 3, 4, 5, 6, '\'', 'n', 9, 10, 11, 12, 13, 14, 15,
                16 };
        final Socket socketB = context.mock(Socket.class);
        final Connectable connectableB = context.mock(Connectable.class);
        
        final Socket socketAwithIOE = context.mock(Socket.class);
        final Socket socketAwithBadAddr = context.mock(Socket.class);
        final InputStream isWithIOE = context.mock(InputStream.class);
        final OutputStream osWithIOE = context.mock(OutputStream.class);
        
        final FriendFirewalledAddressConnector connector 
            = new FriendFirewalledAddressConnector(null, null, null, null, null, null);
        
        context.checking(new Expectations() {
            {
                // Non critical actions
                allowing(fwAddressA).getClientGuid();
                will(returnValue(new GUID(guidA)));
                allowing(fwAddressA).getPublicAddress();
                will(returnValue(connectableA));
                allowing(socketA).getInetAddress();
                will(returnValue(inetAddress));
                
                allowing(fwAddressB).getClientGuid();
                will(returnValue(new GUID(guidB)));
                allowing(fwAddressB).getPublicAddress();
                will(returnValue(connectableB));
                allowing(socketB).getInetAddress();
                will(returnValue(inetAddress));
                
                allowing(socketAwithBadAddr).getInetAddress();
                will(returnValue(null));
                
                allowing(socketAwithIOE).getInetAddress();
                will(returnValue(inetAddress));
                
                allowing(connectableA).getAddress();
                will(returnValue("127.0.0.1"));
                allowing(connectableA).getPort();
                will(returnValue(80));
                allowing(connectableA).getInetAddress();
                will(returnValue(inetAddress));
                
                allowing(connectableB).getAddress();
                will(returnValue("127.0.0.1"));
                allowing(connectableB).getPort();
                will(returnValue(80));
                allowing(connectableB).getInetAddress();
                will(returnValue(inetAddress));


                // The bad socket should be closed
                allowing(socketAwithIOE).getInputStream();
                will(returnValue(isWithIOE));
                allowing(socketAwithIOE).getOutputStream();
                will(returnValue(osWithIOE));
                exactly(1).of(socketAwithIOE).close();
                exactly(1).of(isWithIOE).close();
                exactly(1).of(osWithIOE).close();
                
                // Assertions
                
                // Forced for testing an IOE
                exactly(1).of(connectObserverA).handleConnect(socketAwithIOE);
                will(throwException(new IOException("forced exception")));
                exactly(1).of(connectObserverA).handleConnect(socketA);
                exactly(1).of(connectObserverB).handleConnect(socketB);
                
            }});
        
        // No observers have been set therefore nothing should be grabbed
        assertFalse(connector.acceptPushedSocket("blah", 10, guidA, socketA));
        
        connector.observers.add(createRandomObserver(context));
        connector.observers.add(createRandomObserver(context));
        
        // No matching observers have been set therefore nothing should be grabbed
        assertFalse(connector.acceptPushedSocket("blah", 10, guidA, socketA));
        
        PushedSocketConnectObserver pushedSocketConnectObserverA 
            = new PushedSocketConnectObserver(fwAddressA, connectObserverA);
        
        connector.observers.add(pushedSocketConnectObserverA);
        connector.observers.add(createRandomObserver(context));
        connector.observers.add(new PushedSocketConnectObserver(fwAddressB, connectObserverB));
        connector.observers.add(createRandomObserver(context));
        
        // These should be handled correctly
        assertTrue(connector.acceptPushedSocket("asdsad", -1, guidA, socketA));
        assertTrue(connector.acceptPushedSocket("asdsadsad", 10, guidB, socketB));
        
        // Repeat should fail
        assertFalse(connector.acceptPushedSocket("asdsad", -1, guidA, socketA));
        
        // Try for A with a bad address
        pushedSocketConnectObserverA.acceptedOrFailed.set(false);
        assertFalse(connector.acceptPushedSocket("blah", 10, guidA, socketAwithBadAddr));
        
        // Try for A with a bad socket -- returns true, exception suppressed, and socket closed
        pushedSocketConnectObserverA.acceptedOrFailed.set(false);
        assertTrue(connector.acceptPushedSocket("blah", 10, guidA, socketAwithIOE));
        
        context.assertIsSatisfied();
        
    }
    
    private PushedSocketConnectObserver createRandomObserver(Mockery context) throws IOException {
        final ConnectObserver connectObserver = context.mock(ConnectObserver.class);    
        final FirewalledAddress fwAddress = context.mock(FirewalledAddress.class);
        final byte[] guid = new byte[] { 'x', (byte) (Math.random()*Byte.MAX_VALUE), 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                'x' };
        
        context.checking(new Expectations() {
            {   allowing(fwAddress).getClientGuid();
                will(returnValue(new GUID(guid)));
                never(connectObserver).handleConnect(with(any(Socket.class)));
            }});
        
        return new PushedSocketConnectObserver(fwAddress, connectObserver);
    }
    
    /**
     * Go through the simply success case of XMPPFirewalledAddressConnector::connect() 
     * and confirm the basic interactions.
     * <p> 
     * After manually fire some of the exception handling code and make sure the events
     * are processed correctly. 
     */
    @SuppressWarnings("unchecked")
    public void testConnectSimple() throws Exception {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }};

        final FriendAddressResolver friendAddressResolver = context.mock(FriendAddressResolver.class);
        final NetworkManager networkManager = context.mock(NetworkManager.class);
        final Provider<UDPSelectorProvider> udpSelectorProviderProvider = context.mock(Provider.class);
        final ScheduledExecutorService backgroundExecutor = context.mock(ScheduledExecutorService.class);
        final Provider<SocketProcessor> socketProcessorProvider = context.mock(Provider.class);
        
        final ConnectObserver observer = context.mock(ConnectObserver.class);    
        final FriendFirewalledAddress address = context.mock(FriendFirewalledAddress.class);
        
        final FirewalledAddress fwAddress = context.mock(FirewalledAddress.class);
        final GUID guid = new GUID(new byte[] {'X','x',1,2,3,4,'.','.','.',5,6,9,'n', 10,'x','X'});
        
        final FriendAddress friendAddress = context.mock(FriendAddress.class);
        
        final Connectable publicConnectable = context.mock(Connectable.class);
        final InetAddress inetAddr = context.mock(InetAddress.class);
        final InetSocketAddress inetSocketAddr = context.mock(InetSocketAddress.class);
        
        final UDPSelectorProvider udpSelectorProvider = context.mock(UDPSelectorProvider.class);
        final AbstractNBSocketChannel socketChanel = context.mock(AbstractNBSocketChannel.class);
        final AbstractNBSocket socket = context.mock(AbstractNBSocket.class);
        
        final Socket socketForConnect = context.mock(Socket.class);
        
        final IOException forcedIOE = new IOException("forced");
        
        final SocketProcessor socketProcessor = context.mock(SocketProcessor.class);
        
        final FriendPresence friendPresence = context.mock(FriendPresence.class);
        
        final FeatureTransport<ConnectBackRequest> connectBackTransport = context.mock(FeatureTransport.class);
        
        final MatchAndCopy<ConnectObserver> connectObserverCollector 
            = new MatchAndCopy<ConnectObserver>(ConnectObserver.class);
    
        final MatchAndCopy<Runnable> runnableCollector = new MatchAndCopy<Runnable>(Runnable.class);
        
        final FriendFirewalledAddressConnector connector 
            = new FriendFirewalledAddressConnector(friendAddressResolver, null, networkManager,
                    backgroundExecutor, udpSelectorProviderProvider, socketProcessorProvider);
        
        context.checking(new Expectations() {
            {
            allowing(publicConnectable).getAddress();
            will(returnValue("40.1.0.0"));
            allowing(publicConnectable).getPort();
            will(returnValue(427));
            allowing(publicConnectable).getInetAddress();
            will(returnValue(inetAddr));
            allowing(publicConnectable).getInetSocketAddress();
            will(returnValue(inetSocketAddr));     
        }});
        
        final ConnectBackRequest connectBackRequest = new ConnectBackRequest(publicConnectable, guid, 407);
        context.assertIsSatisfied();
        
        context.checking(new Expectations() {
            {   allowing(address).getFirewalledAddress();
                will(returnValue(fwAddress));
                
                allowing(fwAddress).getClientGuid();
                will(returnValue(guid));
                allowing(fwAddress).getPublicAddress();
                will(returnValue(publicConnectable));
                
                allowing(networkManager).getPublicAddress();
                will(returnValue(publicConnectable));
                
                allowing(address).getFriendAddress();
                will(returnValue(friendAddress));
                allowing(friendAddress).getFullId();
                will(returnValue("403"));
                
                allowing(networkManager).supportsFWTVersion();
                will(returnValue(407));
                
                allowing(udpSelectorProviderProvider).get();
                will(returnValue(udpSelectorProvider));
                allowing(udpSelectorProvider).openSocketChannel();
                will(returnValue(socketChanel));
                allowing(socketChanel).socket();
                will(returnValue(socket));
                
                allowing(socketProcessorProvider).get();
                will(returnValue(socketProcessor));
                
                // Assertions
                exactly(1).of(networkManager).acceptedIncomingConnection();
                will(returnValue(false));
                
                one(friendAddressResolver).getPresence(friendAddress);
                will(returnValue(friendPresence));
                
                one(friendPresence).getTransport(ConnectBackRequestFeature.class);
                will(returnValue(connectBackTransport));
                
                one(connectBackTransport).sendFeature(friendPresence, connectBackRequest);
                
                exactly(1).of(socket).connect(with(same(inetSocketAddr)),
                        with(any(Integer.class)), with(connectObserverCollector));
                
                exactly(1).of(backgroundExecutor).schedule(with(runnableCollector), 
                        with(any(Integer.class)), with((any(TimeUnit.class))));
                
                exactly(1).of(observer).handleIOException(forcedIOE);
                exactly(1).of(observer).handleIOException(with(any(ConnectException.class)));
                exactly(1).of(observer).handleIOException(with(any(IOException.class)));
                
                exactly(1).of(socketProcessor).processSocket(socketForConnect, "GIV");
            }});
        
        // Connect
        connector.connect(address, observer);
 
        // ...
        assertNotEmpty(connector.observers);
        PushedSocketConnectObserver pushedConnectObserver = connector.observers.get(0);
        ConnectObserver connectObserver = connectObserverCollector.getLastMatch();
        Runnable expirerRunnable = runnableCollector.getLastMatch();
        
        // Make sure the observers are properly linked by simulating some events
        connectObserver.shutdown();
        pushedConnectObserver.acceptedOrFailed.set(false);
        connectObserver.handleIOException(forcedIOE);
        connectObserver.handleIOException(forcedIOE);
        pushedConnectObserver.acceptedOrFailed.set(false);
        connectObserver.handleConnect(socketForConnect);
        
        // Force expire the connection.  Ensures
        //  the event is captured and observer is removed
        expirerRunnable.run();
        assertEmpty(connector.observers);
        
        context.assertIsSatisfied();
    }
 
    
    /**
     * Attempt to connect using an invalid public ip.  Ensure this is 
     * handled gracefully and an IOE is passed to the ConnectObserver. 
     */
    @SuppressWarnings("unchecked")
    public void testConnectWithBadIpPort() throws IOException {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }};

        final NetworkManager networkManager = context.mock(NetworkManager.class);
        final Provider<UDPSelectorProvider> udpSelectorProviderProvider = context.mock(Provider.class);
        final ScheduledExecutorService backgroundExecutor = context.mock(ScheduledExecutorService.class);
        final Provider<SocketProcessor> socketProcessorProvider = context.mock(Provider.class);
        
        final ConnectObserver observer = context.mock(ConnectObserver.class);    
        final FriendFirewalledAddress address = context.mock(FriendFirewalledAddress.class);
        
        final FirewalledAddress fwAddress = context.mock(FirewalledAddress.class);
        final GUID guid = new GUID(new byte[] {'X','x',1,2,3,4,'.','.','.',5,6,9,'n', 10,'x','X'});
        
        final Connectable publicConnectable = context.mock(Connectable.class);
        final InetAddress inetAddr = context.mock(InetAddress.class);
        final InetSocketAddress inetSocketAddr = context.mock(InetSocketAddress.class);
        
        final FriendFirewalledAddressConnector connector 
            = new FriendFirewalledAddressConnector(null, null, networkManager,
                backgroundExecutor, udpSelectorProviderProvider, socketProcessorProvider);
        
        context.checking(new Expectations() {
            {   allowing(address).getFirewalledAddress();
                will(returnValue(fwAddress));
                
                allowing(fwAddress).getClientGuid();
                will(returnValue(guid));
                allowing(fwAddress).getPublicAddress();
                will(returnValue(publicConnectable));
                allowing(networkManager).getPublicAddress();
                will(returnValue(publicConnectable));
                
                allowing(publicConnectable).getAddress();
                will(returnValue("401.0.0.0"));
                allowing(publicConnectable).getPort();
                will(returnValue(-404));
                allowing(publicConnectable).getInetAddress();
                will(returnValue(inetAddr));
                allowing(publicConnectable).getInetSocketAddress();
                will(returnValue(inetSocketAddr));
                
                
                exactly(1).of(observer).handleIOException(with(any(ConnectException.class)));
            }});
        
        // Connect
        connector.connect(address, observer);
         
        context.assertIsSatisfied();
    }

    
    /**
     * Connect when is there is no incoming connection and an send failure.
     */
    @SuppressWarnings("unchecked")
    public void testConnectWithFails() throws Exception {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }};

            final FriendAddressResolver friendAddressResolver = context.mock(FriendAddressResolver.class);
        final NetworkManager networkManager = context.mock(NetworkManager.class);
        final Provider<UDPSelectorProvider> udpSelectorProviderProvider = context.mock(Provider.class);
        final ScheduledExecutorService backgroundExecutor = context.mock(ScheduledExecutorService.class);
        final Provider<SocketProcessor> socketProcessorProvider = context.mock(Provider.class);
        
        final PushDownloadManager pushDownloadManager = context.mock(PushDownloadManager.class);
        
        final ConnectObserver observer = context.mock(ConnectObserver.class);    
        final FriendFirewalledAddress address = context.mock(FriendFirewalledAddress.class);
        
        final FirewalledAddress fwAddress = context.mock(FirewalledAddress.class);
        final GUID guid = new GUID(new byte[] {'X','x',1,2,3,4,'.','.','.',5,6,9,'n', 10,'x','X'});
        
        final FriendAddress friendAddress = context.mock(FriendAddress.class);
        
        final Connectable publicConnectable = context.mock(Connectable.class);
        final InetAddress inetAddr = context.mock(InetAddress.class);
        final InetSocketAddress inetSocketAddr = context.mock(InetSocketAddress.class);
        
        final FriendPresence friendPresence = context.mock(FriendPresence.class);
        
        final FeatureTransport<ConnectBackRequest> connectBackTransport = context.mock(FeatureTransport.class);
    
        final FriendFirewalledAddressConnector connector 
            = new FriendFirewalledAddressConnector(friendAddressResolver, pushDownloadManager, networkManager,
                    backgroundExecutor, udpSelectorProviderProvider, socketProcessorProvider);
        
        context.checking(new Expectations() {
            {
            allowing(publicConnectable).getAddress();
            will(returnValue("40.1.0.0"));
            allowing(publicConnectable).getPort();
            will(returnValue(427));
            allowing(publicConnectable).getInetAddress();
            will(returnValue(inetAddr));
            allowing(publicConnectable).getInetSocketAddress();
            will(returnValue(inetSocketAddr));     
        }});
        
        final ConnectBackRequest connectBackRequest = new ConnectBackRequest(publicConnectable, guid, 0);
        context.assertIsSatisfied();
        
        context.checking(new Expectations() {
            {   allowing(address).getFirewalledAddress();
                will(returnValue(fwAddress));
                
                allowing(fwAddress).getClientGuid();
                will(returnValue(guid));
                allowing(fwAddress).getPublicAddress();
                will(returnValue(publicConnectable));
                
                allowing(networkManager).getPublicAddress();
                will(returnValue(publicConnectable));
                
                allowing(address).getFriendAddress();
                will(returnValue(friendAddress));
                allowing(friendAddress).getFullId();
                will(returnValue("403"));
                
                // Assertions
                exactly(2).of(networkManager).acceptedIncomingConnection();
                will(returnValue(true));
                
                allowing(friendAddressResolver).getPresence(friendAddress);
                will(returnValue(friendPresence));
                
                allowing(friendPresence).getTransport(ConnectBackRequestFeature.class);
                will(returnValue(connectBackTransport));
                
                one(connectBackTransport).sendFeature(friendPresence, connectBackRequest);
                will(throwException(new FriendException("error sending")));
                
                one(connectBackTransport).sendFeature(friendPresence, connectBackRequest);
                
                exactly(1).of(pushDownloadManager).connect(fwAddress, observer);
                
                exactly(1).of(backgroundExecutor).schedule(with(any(Runnable.class)), 
                        with(any(Integer.class)), with((any(TimeUnit.class))));
            }});
        
        // Connect
        connector.connect(address, observer);
        connector.connect(address, observer);
        
        context.assertIsSatisfied();
    }
    

    
}
