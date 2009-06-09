package org.limewire.core.impl.browse;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.URN;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.CoreGlueModule;
import org.limewire.core.impl.search.QueryReplyListenerList;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.core.impl.xmpp.XMPPRemoteFileDescDeserializer;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.io.IpPort;
import org.limewire.io.UnresolvedIpPort;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.api.client.XMPPPresence;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.XMPPFriend;
import org.limewire.xmpp.api.client.XMPPAddress;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.limegroup.gnutella.LimeWireCoreModule;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SearchServices;

/**
 *
 * Test browse/downloads over a real life network.
 */
public class FriendBrowseDownloadRUDPTest extends LimeTestCase {

    private static Log LOG = LogFactory.getLog(FriendBrowseDownloadRUDPTest.class);
    
    private static final int SECONDS_TO_WAIT = 10;
    private static final String USERNAME_1 = "limenetworktest1@gmail.com";
    private static final String FRIEND = "limenetworktest2@gmail.com";

    private static final String PASSWORD_1 = "limebuddy123";
    private static final String SERVICE = "gmail.com";

    private XMPPConnection conn;
    private ServiceRegistry registry;
    protected XMPPService service;
    protected Injector injector;

    public FriendBrowseDownloadRUDPTest(String name) {
        super(name);
    }


    @Override
    protected void setUp() throws Exception {
        injector = createInjector(getModules());
        registry = injector.getInstance(ServiceRegistry.class);
        registry.initialize();                                                                                
        registry.start();
        service = injector.getInstance(XMPPService.class);
        XMPPConnectionConfiguration config = getDefaultXmppConnectionConfig(USERNAME_1, PASSWORD_1, SERVICE);

        ListeningFuture<XMPPConnection> loginTask = service.login(config);
        conn = loginTask.get(SECONDS_TO_WAIT, TimeUnit.SECONDS);
    }

    @Override
    public void tearDown() throws Exception {
        service.logout();
        registry.stop();
    }

    private Injector createInjector(Module... modules) {
        return Guice.createInjector(Stage.PRODUCTION, modules);
    }

    private XMPPConnectionConfiguration getDefaultXmppConnectionConfig(final String userName, final String passwd,
                                                                       final String serviceName) {
        return new XMPPConnectionConfiguration() {
            @Override public boolean isDebugEnabled() { return true; }
            @Override public String getUserInputLocalID() { return userName; }
            @Override public String getPassword() { return passwd; }
            @Override public String getLabel() { return getServiceName(); }
            @Override public String getServiceName() { return serviceName; }
            @Override public String getResource() { return "LimeWire"; }
            @Override public EventListener<RosterEvent> getRosterListener() { return null; }
            @Override public String getCanonicalizedLocalID() { return getUserInputLocalID(); }
            @Override public String getNetworkName() { return getServiceName(); }
            @Override public List<UnresolvedIpPort> getDefaultServers() { return UnresolvedIpPort.EMPTY_LIST;}
        };
    }

    private Module[] getModules() {
        List<Module> modules = new ArrayList<Module>();
        modules.add(new LimeWireCoreModule());
        modules.add(new CoreGlueModule());
        return modules.toArray(new Module[modules.size()]);
    }

    /**
     * Tests browsing a friend, and downloading a file from one of the browse results
     *
     */
    public void testBrowseDownloadFromFriendBehindFirewall() throws Exception {
        waitForFeature(AddressFeature.ID, FRIEND);
        
        Collection<XMPPPresence> presences = conn.getFriend(FRIEND).getPresences().values();
        assertEquals(1, presences.size());
        XMPPPresence presence = presences.iterator().next();


        SearchServices searchServices = injector.getInstance(SearchServices.class);
        QueryReplyListenerList queryReplyListenerList = injector.getInstance(QueryReplyListenerList.class);

        CoreBrowse coreBrowse = new CoreBrowse(presence, searchServices, queryReplyListenerList);

        BrowseStatistics browser = new BrowseStatistics(presence);
        coreBrowse.start(browser);

        Map<String, SearchResult> browseResults = waitForBrowse(browser);

        // checking browse results
        assertEquals(4, browseResults.size());
        assertEquals("urn:sha1:M44YAZ2RDV2A5ZF4XLWGCTWIXE63F7M2", browseResults.get("2610431487_a7f2e127e7.jpg").getUrn().toString());
        assertEquals("urn:sha1:DQCI4IOVHEHNR3BWOUKANM2S2AMVUVFN", browseResults.get("2611264598_f5b419bbd1.jpg").getUrn().toString());
        assertEquals("urn:sha1:TGYN5XX62CLPKGMINUL2ZU3GVBMWTXEQ", browseResults.get("2611264634_c650e4fd81.jpg").getUrn().toString());
        assertEquals("urn:sha1:FEZP6EIMEPRXUEHSRQNEBJB3JNYBUNEC", browseResults.get("2611264936_6103872e4d.jpg").getUrn().toString());


        // test downloading from buddy
        String fileNameToDownload = "2611264598_f5b419bbd1.jpg";
        URN urnOfFile = browseResults.get(fileNameToDownload).getUrn();

        SearchResult resultToDownload = browseResults.get(fileNameToDownload);
        List<SearchResult> toDownload = Collections.singletonList(resultToDownload);

        DownloadListManager downloader = injector.getInstance(DownloadListManager.class);
        DownloadItem dlItem = downloader.getDownloadItem(urnOfFile);

        try {
            dlItem = downloader.addDownload(null, toDownload);
            waitForDownloadCompletion(dlItem);
        } catch (SaveLocationException e) {
            e.printStackTrace();
            fail("Failed to save the file: " + e.getMessage());
        }

        // check size of downloaded file
        assertEquals(142682L, dlItem.getTotalSize());

    }

    private Map<String, SearchResult> waitForBrowse(BrowseStatistics browse) throws Exception {
        return browse.waitForBrowseToFinish();
    }

    private void waitForDownloadCompletion(final DownloadItem dlItem) throws Exception {

        // check to see if download is already done.
        if (dlItem.getState() == DownloadState.DONE) {
            return;
        }
        
        final CountDownLatch latch = new CountDownLatch(1);
        int downloadTimeLimit = SECONDS_TO_WAIT;

        dlItem.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if ("state".equals(evt.getPropertyName())) {
                    DownloadState state = (DownloadState) evt.getNewValue();
                    if (state == DownloadState.DONE) {
                        latch.countDown();
                    }
                    LOG.debugf("current download state: {0}", state);
                }
            }
        });

        assertTrue("Download not completed within " + downloadTimeLimit + " seconds",
                   latch.await(downloadTimeLimit, TimeUnit.SECONDS));
    }

    private void waitForFeature(final URI featureUri, final String friendId) throws Exception {
        int featureWaitTimeout = SECONDS_TO_WAIT;
        final CountDownLatch latch = new CountDownLatch(1);
        final ListenerSupport<FeatureEvent> featureListener =
                injector.getInstance(Key.get(new TypeLiteral<ListenerSupport<FeatureEvent>>() {
                }));

        final EventListener<FeatureEvent> listener = new EventListener<FeatureEvent>() {

            public void handleEvent(FeatureEvent event) {

                if (event.getType() == FeatureEvent.Type.ADDED &&
                    event.getData().getID().equals(featureUri) &&
                    event.getSource().getFriend().getId().equals(friendId)) {
                    latch.countDown();
                    featureListener.removeListener(this);
                }
            }
        };
        featureListener.addListener(listener);


        // check for the feature in case it already came in prior to listener being added
        XMPPFriend user = conn.getFriend(friendId);
        if (user != null && !user.getPresences().isEmpty()) {
            // friend already signed in
            XMPPPresence presence = conn.getFriend(friendId).getPresences().values().iterator().next();
            if (presence.getFeature(featureUri) != null) {
                return;
            }
        }

        // wait at most 5 seconds for feature to be detected
        assertTrue("Feature " + featureUri + " not detected in " + featureWaitTimeout + " seconds.",
                   latch.await(featureWaitTimeout, TimeUnit.SECONDS));
    }

    private class BrowseStatistics implements BrowseListener {

        private final Map<String, SearchResult> searchResults;
        private final CountDownLatch latch;
        private final XMPPRemoteFileDescDeserializer searchResultToXmppAdapter;
        private final XMPPPresence presence;

        BrowseStatistics(XMPPPresence presence) {
            this.presence = presence;
            this.searchResultToXmppAdapter = injector.getInstance(XMPPRemoteFileDescDeserializer.class);
            this.searchResults = new ConcurrentHashMap<String, SearchResult>();
            this.latch = new CountDownLatch(1);
        }

        public void handleBrowseResult(SearchResult searchResult) {
            SearchResult adaptedSearchResultWithXmppAddress = convertToXmppCompatibleSearchResult(searchResult);
            searchResults.put(searchResult.getFileName(), adaptedSearchResultWithXmppAddress);
        }

        public void browseFinished(boolean success) {
            latch.countDown();
        }

        /**
         * This method (hack) is necessary because it is not currently possible to perform a
         * friend browse, and use the results of the browse ({@link SearchResult} objects) for
         * downloading, because the FriendPresence contained in such a SearchResult is a GnutellaPresence
         */
        private SearchResult convertToXmppCompatibleSearchResult(SearchResult gnutellaOnlySearchResult) {
            RemoteFileDescAdapter rfdAdapter = (RemoteFileDescAdapter)gnutellaOnlySearchResult;
            RemoteFileDesc oldRfd = rfdAdapter.getRfd();
            XMPPAddress xmppAddress = new XMPPAddress(presence.getJID());
            RemoteFileDesc newRfd = searchResultToXmppAdapter.promoteRemoteFileDescAndExchangeAddress(oldRfd, xmppAddress);
            Set<IpPort> ipPort = new HashSet<IpPort>();
            ipPort.addAll(rfdAdapter.getAlts());

            return new RemoteFileDescAdapter(newRfd, ipPort);
        }

        public Map<String, SearchResult> waitForBrowseToFinish() throws InterruptedException {
            assertTrue("Browse not completed within " + SECONDS_TO_WAIT + " seconds",
                       latch.await(SECONDS_TO_WAIT, TimeUnit.SECONDS));
            return searchResults;
        }
    }
}
