package org.limewire.core.impl.integration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.limegroup.gnutella.DownloadServices;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.DownloadStateEvent;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import org.limewire.core.impl.tests.CoreGlueTestUtils;
import org.limewire.core.impl.xmpp.XMPPRemoteFileDesc;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.feature.features.AuthTokenFeature;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.io.Address;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventBroadcaster;
import org.limewire.net.SocketsManager;
import org.limewire.net.ConnectivityChangeEvent;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressResolutionObserver;
import org.limewire.util.StringUtils;
import org.limewire.util.TestUtils;
import org.limewire.xmpp.api.client.XMPPAddress;
import org.limewire.xmpp.client.impl.XMPPAddressResolver;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpFields;
import org.mortbay.http.HttpMessage;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.NotFoundHandler;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.util.B64Code;
import org.mortbay.util.Resource;
import org.jmock.Mockery;
import org.jmock.Expectations;


/**
 * Collection of friend download integration tests. This tests the friend download
 * code and will attempt to download from an http server run on localhost.
 * <p/>
 * The test code will mock out or simulate the server
 * behavior, and we are testing LimeWire's friend download behavior.
 */
public class FriendDownloadTest extends LimeTestCase {

    private static final String FRIEND_ID = "limebuddytest@gmail.com";
    private static final String AUTH_TOKEN = "authToken";
    private static final String DOWNLOADER_LOGIN_ID = "limedownloader@gmail.com";
    private static final String RESOURCE = "Home1234567";
    private static final String PRESENCE_ID = FRIEND_ID + "/" + RESOURCE;
    private static final String HOST = "localhost";
    private static final String FILE_TO_DOWNLOAD_URN = "urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ";
    private static final int SIZE_OF_DOWNLOAD = 26;

    private static final String FRIEND_DOWNLOAD_PREFIX = "/friend/download/";
    private static final int PORT = 8000;
    private static final int DOWNLOAD_WAIT_MILLIS = 10000;
    private static final String FILE_NAME = "alphabet test file#2.txt";

    private HttpServer server;
    private Injector injector;
    private ServiceRegistry registry;
    private Mockery context;
    private RemoteFileDesc remoteFileDesc;
    private DownloadServices dlServices;

    // keeps track of whether we are designating the sharing friend (the one who is sharing files)
    // as "signed in" or not, since we are not actually logging in to a server
    private AtomicBoolean isSharingFriendLoggedIn = new AtomicBoolean(true);

    // used to signify connection state changes because this is
    // how LW (specifically ManagedDownloader) knows when to retry downloads
    private EventBroadcaster<ConnectivityChangeEvent> connectivityBroadcaster;

    public FriendDownloadTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        server = new HttpServer();
        injector = CoreGlueTestUtils.createInjectorAndStart();
        registry = injector.getInstance(ServiceRegistry.class);
        context = new Mockery();
        connectivityBroadcaster = injector.getInstance(
                Key.get(new TypeLiteral<EventBroadcaster<ConnectivityChangeEvent>>() {
                }));
        dlServices = injector.getInstance(DownloadServices.class);
        remoteFileDesc = getRemoteFileDesc();
        registry.initialize();
        registry.start();
    }

    private RemoteFileDesc getRemoteFileDesc() throws IOException {
        AddressFactory factory = injector.getInstance(AddressFactory.class);
        LocalhostXmppAddressResolver resolver = new LocalhostXmppAddressResolver();
        resolver.initialize();

        XMPPAddress xmppAddress = new XMPPAddress(PRESENCE_ID);

        URN sha1Urn = URN.createSHA1Urn(FILE_TO_DOWNLOAD_URN);
        Set<URN> sha1UrnSet = new HashSet<URN>();
        sha1UrnSet.add(sha1Urn);

        return new XMPPRemoteFileDesc(xmppAddress, 1, FILE_NAME, SIZE_OF_DOWNLOAD,
                GUID.makeGuid(), 1, 1, null, sha1UrnSet, "vendor", -1, false, factory, resolver);
    }

    @Override
    protected void tearDown() throws Exception {
        this.server.stop();
        this.registry.stop();
    }


    /**
     * A friend download from start to finish where the friend source is online the whole time.
     */
    public void testStartToFinishFriendIsAlwaysOnline() throws Exception {
        setupAndStartServer(new TestResourceHandler());
        setSharingFriendSignedIn(true);
        Downloader dl = dlServices.download(new RemoteFileDesc[]{remoteFileDesc}, true, new GUID());
        assertNotNull(dl);
        assertTrue(waitForDownloadState(dl, DOWNLOAD_WAIT_MILLIS, Downloader.DownloadState.COMPLETE));
        assertEquals(dl.getState(), Downloader.DownloadState.COMPLETE);
    }

    /**
     * A friend download should never complete if the friend is
     * offline for the entire duration of the test
     */
    public void testStartToFinishFriendIsAlwaysOffline() throws Exception {
        // Set sharing friend to be "not signed in". Attempt download.
        setupAndStartServer(new TestResourceHandler());
        setSharingFriendSignedIn(false);
        Downloader dl = dlServices.download(new RemoteFileDesc[]{remoteFileDesc}, true, new GUID());

        assertFalse(waitForDownloadState(dl, DOWNLOAD_WAIT_MILLIS, Downloader.DownloadState.COMPLETE));
        assertFalse(dl.isCompleted());
    }

    /**
     * A friend download that starts while the friend is not online, then the friend becomes available.
     */
    public void testDownloadStartsWhileFriendNotOnlineComesOnlineLater() throws Exception {
        // Set sharing friend to be "not signed in". Attempt download.
        setupAndStartServer(new TestResourceHandler());
        setSharingFriendSignedIn(false);
        Downloader dl = dlServices.download(new RemoteFileDesc[]{remoteFileDesc}, true, new GUID());

        // Sleep some time for the download to start.
        // Then change the sharing friend to "signed in"
        Thread.sleep(3000);

        // Download should most definitely not be complete!
        assertFalse(dl.isCompleted());

        setSharingFriendSignedIn(true);

        // Wait for download completion.
        assertTrue(waitForDownloadState(dl, DOWNLOAD_WAIT_MILLIS, Downloader.DownloadState.COMPLETE));
        assertEquals(dl.getState(), Downloader.DownloadState.COMPLETE);
    }

    /**
     * A friend download where the friend goes offline (network outage) and becomes available
     * again, testing if the download is resumed correctly.
     */
    public void testFriendOutageDownloadResume() throws Exception {
        setSharingFriendSignedIn(true);
        final AtomicBoolean shouldTruncate = new AtomicBoolean(true);

        setupAndStartServer(new TestResourceHandler() {
            @Override
            protected void sendData(org.mortbay.http.HttpRequest httpRequest,
                                    org.mortbay.http.HttpResponse httpResponse) throws IOException {
                // this flag is used to trigger the http server into uploading half the file's bytes
                if (shouldTruncate.get()) {
                    httpHeader.put("Range", "bytes=0-" + SIZE_OF_DOWNLOAD / 2);
                }
                super.sendData(httpRequest, httpResponse);
            }
        });

        // start download
        Downloader dl = dlServices.download(new RemoteFileDesc[]{remoteFileDesc}, true, new GUID());

        // wait and verify the downloading client has effectively given up
        assertTrue(waitForDownloadState(dl, DOWNLOAD_WAIT_MILLIS, Downloader.DownloadState.DOWNLOADING));
        server.stop();

        // sleep for a little, and make sure the downloader is not
        // in a terminal state
        Thread.sleep(5000);
        assertFalse(dl.isCompleted());
        assertTrue(dl.isResumable());
        setSharingFriendSignedIn(false);

        // tell the http server request handler to upload the entire range of the file
        shouldTruncate.set(false);

        // restart the http server
        assertFalse(server.isStarted());
        server.start();

        // set sharing friend as being online. this should kick off a downloading retry
        setSharingFriendSignedIn(true);

        // Wait for download completion.
        assertTrue(waitForDownloadState(dl, DOWNLOAD_WAIT_MILLIS, Downloader.DownloadState.COMPLETE));
        assertEquals(dl.getState(), Downloader.DownloadState.COMPLETE);
    }

    /**
     * A friend download where the friend cannot authenticate.
     */
    public void testFriendCannotAuthenticate() throws Exception {
        setSharingFriendSignedIn(true);
        setupAndStartServer(new TestResourceHandler() {
            @Override
            protected void sendData(org.mortbay.http.HttpRequest httpRequest,
                                    org.mortbay.http.HttpResponse httpResponse) throws IOException {
                httpResponse.sendError(HttpResponse.__401_Unauthorized);
            }
        });
        Downloader dl = dlServices.download(new RemoteFileDesc[]{remoteFileDesc}, true, new GUID());

        // Wait for download completion. Download should fail.
        assertFalse(waitForDownloadState(dl, DOWNLOAD_WAIT_MILLIS, Downloader.DownloadState.COMPLETE));
        assertFalse(dl.isCompleted());
    }

    /**
     * Starts the jetty http server given a {@link TestResourceHandler}
     *
     * @param requestHandler TestResourceHandler to handle http requests
     * @throws Exception if server fails to start
     */
    private void setupAndStartServer(TestResourceHandler requestHandler) throws Exception {
        SocketListener listener = new SocketListener();
        listener.setPort(PORT);
        listener.setMinThreads(1);
        server.addListener(listener);

        HttpContext context = server.addContext(FRIEND_DOWNLOAD_PREFIX);
        String fileDir = TestUtils.getResourceInPackage(
                FILE_NAME, getClass()).getParentFile().getAbsolutePath();
        context.setResourceBase(fileDir);

        requestHandler.setAcceptRanges(true);
        requestHandler.setDirAllowed(true);
        context.addHandler(requestHandler);
        context.addHandler(new NotFoundHandler());
        server.start();
    }

    /**
     * Wait for the downloader to be at a certain download state.
     * Returns true if download state was arrived at before timeout occurs.
     *
     * @param dl                 Downloader to wait for
     * @param milliSecondsToWait time in milliseconds to wait
     * @param state              download state waiting for
     * @return true if the download state was reached before the time elapsed.
     *         false if time elapsed first.
     * @throws InterruptedException if thread is interrupted
     */
    private boolean waitForDownloadState(Downloader dl, long milliSecondsToWait,
                                         final Downloader.DownloadState state) throws InterruptedException {
        final ManagedDownloader mdl = (ManagedDownloader) dl;
        final CountDownLatch latch = new CountDownLatch(1);
        EventListener<DownloadStateEvent> dlListener = new EventListener<DownloadStateEvent>() {
            @Override
            public void handleEvent(DownloadStateEvent event) {
                if (event.getType() == state) {
                    latch.countDown();
                }
            }
        };
        mdl.addListener(dlListener);

        return latch.await(milliSecondsToWait, TimeUnit.MILLISECONDS);
    }

    private void setSharingFriendSignedIn(boolean signedIn) {
        if (isSharingFriendLoggedIn.getAndSet(signedIn) != signedIn) {
            connectivityBroadcaster.broadcast(new ConnectivityChangeEvent());
        }
    }

    /**
     * An address resolver that always resolves to localhost as long as the friend
     * associated with the xmpp address is deemed to be signed in. Otherwise, it
     * does not resolve.
     */
    private class LocalhostXmppAddressResolver extends XMPPAddressResolver {

        private FriendPresence mockFriendPresence;
        private final AuthTokenFeature authTokenFeature = new AuthTokenFeature(AUTH_TOKEN.getBytes());

        public LocalhostXmppAddressResolver() {
            super(null, null, null, null);
            initFriendPresence();
        }

        public void initialize() {
            SocketsManager mgr = injector.getInstance(SocketsManager.class);
            mgr.registerResolver(this);
        }

        @Override
        public boolean canResolve(Address address) {
            return (address instanceof XMPPAddress) && isSharingFriendLoggedIn.get();
        }

        @Override
        public <T extends AddressResolutionObserver> T resolve(Address address, T observer) {
            Address resolvedAddress = null;
            try {
                resolvedAddress = new ConnectableImpl(HOST, PORT, false);
            } catch (UnknownHostException e) {
                fail(HOST + " is an unknown host", e);
            }
            observer.resolved(resolvedAddress);
            return observer;
        }

        @Override
        public FriendPresence getPresence(XMPPAddress address) {
            return mockFriendPresence;
        }

        private void initFriendPresence() {
            mockFriendPresence = context.mock(FriendPresence.class);
            final Friend friend = context.mock(Friend.class);
            final Network network = context.mock(Network.class);
            final String canonLocalId = DOWNLOADER_LOGIN_ID;

            context.checking(new Expectations() {
                {
                    allowing(mockFriendPresence).getFeature(AuthTokenFeature.ID);
                    will(returnValue(authTokenFeature));
                    allowing(mockFriendPresence).getFriend();
                    will(returnValue(friend));
                    allowing(friend).getNetwork();
                    will(returnValue(network));
                    allowing(network).getCanonicalizedLocalID();
                    will(returnValue(canonLocalId));
                }
            });
        }
    }

    /**
     * HTTP Request handler code. May override {@link #sendData}
     * method to specify behavior than just uploading the expected file.
     */
    private class TestResourceHandler extends ResourceHandler {

        // used to get at and set HTTP header info
        protected HttpFields httpHeader;

        @Override
        public void handleGet(org.mortbay.http.HttpRequest httpRequest,
                              org.mortbay.http.HttpResponse httpResponse,
                              String s, java.lang.String s1,
                              org.mortbay.util.Resource resource) throws java.io.IOException {
            super.handleGet(httpRequest, httpResponse, s, s1, resource);
            parseAndValidate(httpRequest);
            sendData(httpRequest, httpResponse);
        }

        private void parseAndValidate(org.mortbay.http.HttpRequest httpRequest) throws UnsupportedEncodingException {
            // validate request.  Failure means a bad request, and there is a bug in Downloading code
            String request = httpRequest.getURI().toString();
            String[] splitQuery = StringUtils.split(request, '/');
            String friendIdParsed = org.mortbay.util.UrlEncoded.decodeString(splitQuery[2], "UTF-8");
            String sha1Urn = splitQuery[4].substring(4);

            if (!(request.startsWith(FRIEND_DOWNLOAD_PREFIX))) {
                fail("Friend download HTTP Request must begin with " + FRIEND_DOWNLOAD_PREFIX);
            } else if (!sha1Urn.equals(FILE_TO_DOWNLOAD_URN)) {
                fail("URN mismatch; Expected URN " + FILE_TO_DOWNLOAD_URN);
            }

            // parse and base 64 decode the auth token
            httpRequest.setState(HttpMessage.__MSG_EDITABLE);
            httpHeader = httpRequest.getHeader();

            String[] authHeaderSplit = httpHeader.get("Authorization").split(" ");
            String authToken = B64Code.decode(authHeaderSplit[1], "UTF-8");

            // check auth token to make sure the user is authorized!
            if (!friendIdParsed.equals(DOWNLOADER_LOGIN_ID) || !authToken.endsWith(":" + AUTH_TOKEN)) {
                fail("Friend " + friendIdParsed + " has wrong auth token " + authToken);
            }
        }

        protected void sendData(org.mortbay.http.HttpRequest httpRequest,
                                org.mortbay.http.HttpResponse httpResponse) throws IOException {
            // send back file
            httpResponse.setStatus(org.mortbay.http.HttpResponse.__200_OK);
            Resource res = getResource(FILE_NAME);
            sendData(httpRequest, httpResponse, "./", res, true);
            httpResponse.commit();
        }
    }
}