package org.limewire.core.impl.browse;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.core.api.URN;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.CoreGlueModule;
import org.limewire.core.impl.search.QueryReplyListenerList;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.limewire.friend.api.FriendConnectionFactory;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.RosterEvent;
import org.limewire.friend.api.feature.AddressFeature;
import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.io.UnresolvedIpPort;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.limegroup.gnutella.LimeWireCoreModule;
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

    private FriendConnection conn;
    private ServiceRegistry registry;
    protected FriendConnectionFactory friendConnectionFactory;
    protected Injector injector;

    public FriendBrowseDownloadRUDPTest(String name) {
        super(name);
    }


    @Override
    protected void setUp() throws Exception {
        injector = createInjector(getModules());
        registry = injector.getInstance(ServiceRegistry.class);
        friendConnectionFactory = injector.getInstance(FriendConnectionFactory.class);
        registry.initialize();                                                                                
        registry.start();
        FriendConnectionConfiguration config = getDefaultXmppConnectionConfig(USERNAME_1, PASSWORD_1, SERVICE);

        ListeningFuture<FriendConnection> loginTask = friendConnectionFactory.login(config);
        conn = loginTask.get(SECONDS_TO_WAIT, TimeUnit.SECONDS);
    }

    @Override
    public void tearDown() throws Exception {
        conn.logout();
        registry.stop();
    }

    private Injector createInjector(Module... modules) {
        return Guice.createInjector(Stage.PRODUCTION, modules);
    }

    private FriendConnectionConfiguration getDefaultXmppConnectionConfig(final String userName, final String passwd,
                                                                       final String serviceName) {
        return new FriendConnectionConfiguration() {
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
            @Override public Type getType() { return Network.Type.XMPP;}
            @Override public Icon getIcon() { return null;}
            @Override
            public Object getAttribute(String key) {
                return null;
            }
            @Override
            public void setAttribute(String key, Object property) {
            }
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
        
        Collection<FriendPresence> presences = conn.getFriend(FRIEND).getPresences().values();
        assertEquals(1, presences.size());
        FriendPresence presence = presences.iterator().next();


        SearchServices searchServices = injector.getInstance(SearchServices.class);
        QueryReplyListenerList queryReplyListenerList = injector.getInstance(QueryReplyListenerList.class);

        CoreBrowse coreBrowse = new CoreBrowse(presence, searchServices, queryReplyListenerList);

        BrowseResultsCollector browser = new BrowseResultsCollector();
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
        } catch (DownloadException e) {
            e.printStackTrace();
            fail("Failed to save the file: " + e.getMessage());
        }

        // check size of downloaded file
        assertEquals(142682L, dlItem.getTotalSize());

    }

    private Map<String, SearchResult> waitForBrowse(BrowseResultsCollector browse) throws Exception {
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
        Friend friend = conn.getFriend(friendId);

        if (friend != null && !friend.getPresences().isEmpty()) {
            // friend already signed in
            FriendPresence presence = conn.getFriend(friendId).getPresences().values().iterator().next();
            if (presence.getFeature(featureUri) != null) {
                return;
            }
        }

        // wait at most 5 seconds for feature to be detected
        assertTrue("Feature " + featureUri + " not detected in " + featureWaitTimeout + " seconds.",
                   latch.await(featureWaitTimeout, TimeUnit.SECONDS));
    }

    private class BrowseResultsCollector implements BrowseListener {

        private final Map<String, SearchResult> searchResults = new ConcurrentHashMap<String, SearchResult>();
        private final CountDownLatch latch = new CountDownLatch(1);

        public void handleBrowseResult(SearchResult searchResult) {
            searchResults.put(searchResult.getFileName(), searchResult);
        }

        public void browseFinished(boolean success) {
            latch.countDown();
        }

        public Map<String, SearchResult> waitForBrowseToFinish() throws InterruptedException {
            assertTrue("Browse not completed within " + SECONDS_TO_WAIT + " seconds",
                       latch.await(SECONDS_TO_WAIT, TimeUnit.SECONDS));
            return searchResults;
        }
    }
}
