package org.limewire.core.impl.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchEvent;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.SearchDetails.SearchType;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.core.api.search.sponsored.SponsoredResultTarget;
import org.limewire.core.impl.library.CoreRemoteFileItem;
import org.limewire.core.impl.library.FriendSearcher;
import org.limewire.core.settings.PromotionSettings;
import org.limewire.geocode.GeocodeInformation;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.listener.EventBroadcaster;
import org.limewire.promotion.PromotionSearcher;
import org.limewire.promotion.PromotionSearcher.PromotionSearchResultsCallback;
import org.limewire.promotion.containers.PromotionMessageContainer;
import org.limewire.promotion.containers.PromotionMessageContainer.PromotionOptions;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.BaseTestCase;
import org.limewire.util.Clock;
import org.limewire.util.ExecuteRunnableAction;
import org.limewire.util.MediaType;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.geocode.CachedGeoLocation;
import com.limegroup.gnutella.messages.QueryReply;

public class CoreSearchTest extends BaseTestCase {

    public CoreSearchTest(String name) {
        super(name);
    }

    @SuppressWarnings( { "unchecked", "cast" })
    public void testQueryReplySearchListenerResultsAdded() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        PromotionSettings.PROMOTION_SYSTEM_IS_ENABLED.setValue(false);

        final SearchDetails searchDetails = new TestSearchDetails("test", SearchCategory.ALL,
                SearchType.KEYWORD);
        final SearchServices searchServices = context.mock(SearchServices.class);
        final QueryReplyListenerList listenerList = context.mock(QueryReplyListenerList.class);
        final PromotionSearcher promotionSearcher = context.mock(PromotionSearcher.class);
        final FriendSearcher friendSearcher = context.mock(FriendSearcher.class);
        final CachedGeoLocation geoLocation = context.mock(CachedGeoLocation.class);
        final ScheduledExecutorService backgroundExecutor = context
                .mock(ScheduledExecutorService.class);
        final EventBroadcaster<SearchEvent> searchEventBroadcaster = (EventBroadcaster<SearchEvent>) context
                .mock(EventBroadcaster.class);

        final byte[] searchGuid = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                16 };
        final SearchListener searchListener = context.mock(SearchListener.class);
        final AtomicReference<QueryReplyListener> queryReplyListener = new AtomicReference<QueryReplyListener>();
        final CoreSearch coreSearch = new CoreSearch(searchDetails, searchServices, listenerList,
                promotionSearcher, friendSearcher, geoLocation, backgroundExecutor,
                searchEventBroadcaster, null, null);

        context.checking(new Expectations() {
            {
                one(searchEventBroadcaster).broadcast(
                        new SearchEvent(coreSearch, SearchEvent.Type.STARTED));
                one(searchServices).newQueryGUID();
                will(returnValue(searchGuid));
                one(listenerList).addQueryReplyListener(with(equal(searchGuid)),
                        with(any(QueryReplyListener.class)));
                will(new AssignParameterAction<QueryReplyListener>(queryReplyListener, 1));
                one(searchServices).query(searchGuid, searchDetails.getSearchQuery(), "",
                        MediaType.getAnyTypeMediaType());
                one(backgroundExecutor).execute(with(any(Runnable.class)));
                will(new ExecuteRunnableAction());
                one(friendSearcher).doSearch(with(equal(searchDetails)),
                        with(any(FriendSearchListener.class)));
                one(searchListener).searchStarted(coreSearch);
            }
        });

        coreSearch.addSearchListener(searchListener);

        coreSearch.start();
        
        try {
            coreSearch.start();
            fail("Illegal search repeat not handled");
        } catch (IllegalStateException e) {
            // Expected
        }

        final RemoteFileDesc remoteFileDesc1 = context.mock(RemoteFileDesc.class);
        final QueryReply queryReply1 = context.mock(QueryReply.class);
        final Set<IpPort> ipPorts = new HashSet<IpPort>();
        final Address address1 = context.mock(Address.class);
        final byte[] guid1 = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        final String fileName1 = "remote file name 1.txt";
        context.checking(new Expectations() {
            {
                allowing(remoteFileDesc1).getAddress();
                will(returnValue(address1));
                allowing(remoteFileDesc1).getClientGUID();
                will(returnValue(guid1));
                allowing(address1).getAddressDescription();
                will(returnValue("address 1 description"));
                allowing(remoteFileDesc1).getFileName();

                will(returnValue(fileName1));
                allowing(remoteFileDesc1).getSize();
                will(returnValue(1234L));
                allowing(remoteFileDesc1).getXMLDocument();
                will(returnValue(null));
                allowing(remoteFileDesc1).getCreationTime();
                will(returnValue(5678L));
                one(searchListener).handleSearchResult(with(same(coreSearch)),
                        with(new SearchResultMatcher(fileName1)));
            }
        });
        queryReplyListener.get().handleQueryReply(remoteFileDesc1, queryReply1, ipPorts);

        context.assertIsSatisfied();
    }

    @SuppressWarnings( { "unchecked", "cast" })
    public void testFriendSearchListenerResultsAdded() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        PromotionSettings.PROMOTION_SYSTEM_IS_ENABLED.setValue(false);

        final SearchDetails searchDetails = new TestSearchDetails("test", SearchCategory.ALL,
                SearchType.KEYWORD);
        final SearchServices searchServices = context.mock(SearchServices.class);
        final QueryReplyListenerList listenerList = context.mock(QueryReplyListenerList.class);
        final PromotionSearcher promotionSearcher = context.mock(PromotionSearcher.class);
        final FriendSearcher friendSearcher = context.mock(FriendSearcher.class);
        final CachedGeoLocation geoLocation = context.mock(CachedGeoLocation.class);
        final ScheduledExecutorService backgroundExecutor = context
                .mock(ScheduledExecutorService.class);
        final EventBroadcaster<SearchEvent> searchEventBroadcaster = (EventBroadcaster<SearchEvent>) context
                .mock(EventBroadcaster.class);
        final Clock clock = context.mock(Clock.class);

        final byte[] searchGuid = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                16 };
        final SearchListener searchListener = context.mock(SearchListener.class);
        final AtomicReference<FriendSearchListener> friendSearchListener = new AtomicReference<FriendSearchListener>();
        final CoreSearch coreSearch = new CoreSearch(searchDetails, searchServices, listenerList,
                promotionSearcher, friendSearcher, geoLocation, backgroundExecutor,
                searchEventBroadcaster, null, clock);

        context.checking(new Expectations() {
            {
                one(searchEventBroadcaster).broadcast(
                        new SearchEvent(coreSearch, SearchEvent.Type.STARTED));
                one(searchServices).newQueryGUID();
                will(returnValue(searchGuid));
                one(listenerList).addQueryReplyListener(with(equal(searchGuid)),
                        with(any(QueryReplyListener.class)));
                one(searchServices).query(searchGuid, searchDetails.getSearchQuery(), "",
                        MediaType.getAnyTypeMediaType());
                one(backgroundExecutor).execute(with(any(Runnable.class)));
                will(new ExecuteRunnableAction());
                one(friendSearcher).doSearch(with(equal(searchDetails)),
                        with(any(FriendSearchListener.class)));
                will(new AssignParameterAction<FriendSearchListener>(friendSearchListener, 1));
                one(searchListener).searchStarted(coreSearch);
            }
        });

        coreSearch.addSearchListener(searchListener);

        coreSearch.start();

        final CoreRemoteFileItem coreRemoteFileItem1 = context.mock(CoreRemoteFileItem.class);
        final SearchResult searchResult1 = context.mock(SearchResult.class);
        final String fileName1 = "filename1";

        context.checking(new Expectations() {
            {
                allowing(coreRemoteFileItem1).getSearchResult();
                will(returnValue(searchResult1));
                allowing(searchResult1).getFileName();
                will(returnValue(fileName1));
                one(searchListener).handleSearchResult(with(same(coreSearch)),
                        with(new SearchResultMatcher(fileName1)));
            }
        });

        friendSearchListener.get().handleFriendResults(
                Arrays.asList((RemoteFileItem) coreRemoteFileItem1));

        context.assertIsSatisfied();
    }

    @SuppressWarnings( { "unchecked", "cast" })
    public void testQueryReplySearchListenerPromotionsAdded() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        PromotionSettings.PROMOTION_SYSTEM_IS_ENABLED.setValue(true);

        final SearchDetails searchDetails = new TestSearchDetails("test", SearchCategory.ALL,
                SearchType.KEYWORD);
        final SearchServices searchServices = context.mock(SearchServices.class);
        final QueryReplyListenerList listenerList = context.mock(QueryReplyListenerList.class);
        final PromotionSearcher promotionSearcher = context.mock(PromotionSearcher.class);
        final FriendSearcher friendSearcher = context.mock(FriendSearcher.class);
        final CachedGeoLocation geoLocation = context.mock(CachedGeoLocation.class);
        final ScheduledExecutorService backgroundExecutor = context
                .mock(ScheduledExecutorService.class);
        final EventBroadcaster<SearchEvent> searchEventBroadcaster = (EventBroadcaster<SearchEvent>) context
                .mock(EventBroadcaster.class);
        final Clock clock = context.mock(Clock.class);

        final byte[] searchGuid = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                16 };
        final SearchListener searchListener = context.mock(SearchListener.class);
        final AtomicReference<QueryReplyListener> queryReplyListener = new AtomicReference<QueryReplyListener>();
        final CoreSearch coreSearch = new CoreSearch(searchDetails, searchServices, listenerList,
                promotionSearcher, friendSearcher, geoLocation, backgroundExecutor,
                searchEventBroadcaster, null, clock);

        final GeocodeInformation geocodeInformation = null;

        final AtomicReference<PromotionSearchResultsCallback> promotionResultCallback = new AtomicReference<PromotionSearchResultsCallback>();
        context.checking(new Expectations() {
            {
                allowing(promotionSearcher).isEnabled();
                will(returnValue(true));
                one(searchEventBroadcaster).broadcast(
                        new SearchEvent(coreSearch, SearchEvent.Type.STARTED));
                one(searchServices).newQueryGUID();
                will(returnValue(searchGuid));
                one(listenerList).addQueryReplyListener(with(equal(searchGuid)),
                        with(any(QueryReplyListener.class)));
                will(new AssignParameterAction<QueryReplyListener>(queryReplyListener, 1));
                one(searchServices).query(searchGuid, searchDetails.getSearchQuery(), "",
                        MediaType.getAnyTypeMediaType());
                exactly(2).of(backgroundExecutor).execute(with(any(Runnable.class)));
                will(new ExecuteRunnableAction());
                one(friendSearcher).doSearch(with(equal(searchDetails)),
                        with(any(FriendSearchListener.class)));
                one(searchListener).searchStarted(coreSearch);
                allowing(geoLocation).getGeocodeInformation();
                will(returnValue(geocodeInformation));
                one(promotionSearcher).search(with(equal(searchDetails.getSearchQuery())),
                        with(any(PromotionSearchResultsCallback.class)),
                        with(any(GeocodeInformation.class)));
                will(new AssignParameterAction<PromotionSearchResultsCallback>(
                        promotionResultCallback, 1));
            }
        });

        coreSearch.addSearchListener(searchListener);

        coreSearch.start();

        final PromotionMessageContainer result = context.mock(PromotionMessageContainer.class);
        final PromotionOptions options = context.mock(PromotionOptions.class);

        final String title = "title";
        final String displayUrl = "displayurl.com";
        final String url = "http://url.com/blahblahblah";
        final String description = "description";
        
        final String expectedUrl = PromotionSettings.REDIRECT_URL.getValue() + "?url=" + url + "&now=52&id=42";
        
        final AtomicReference<List<SponsoredResult>> sponsoredResults = new AtomicReference<List<SponsoredResult>>();
        context.checking(new Expectations() {
            {
                allowing(result).getOptions();
                will(returnValue(options));
                allowing(options).isOpenInStoreTab();
                will(returnValue(true));
                allowing(result).getTitle();
                will(returnValue(title));
                allowing(result).getDisplayUrl();
                will(returnValue(displayUrl));
                allowing(result).getURL();
                will(returnValue(url));
                allowing(result).getDescription();
                will(returnValue(description));
                allowing(result).getUniqueID();
                will(returnValue(42L));
                allowing(clock).now();
                will(returnValue(52000L)); //divs by 1000
                one(searchListener).handleSponsoredResults(with(any(Search.class)),
                        with(new SponsoredResultMatcher(title, expectedUrl, displayUrl)));
                will(new AssignParameterAction<List<SponsoredResult>>(sponsoredResults, 1));
            }
        });
        promotionResultCallback.get().process(result);
        
        assertNotEmpty(sponsoredResults.get());
        assertEquals(1, sponsoredResults.get().size());
        
        SponsoredResult sponsoredResult = sponsoredResults.get().get(0);
   
        assertEquals(expectedUrl, sponsoredResult.getUrl());
        assertEquals(SponsoredResultTarget.STORE, sponsoredResult.getTarget());
        assertEquals(description, sponsoredResult.getText());
        assertEquals(title, sponsoredResult.getTitle());
        assertEquals(displayUrl, sponsoredResult.getVisibleUrl());
        
        context.assertIsSatisfied();
    }

    private final class SearchResultMatcher extends BaseMatcher<SearchResult> {
        private final String fileName1;

        private SearchResultMatcher(String fileName1) {
            this.fileName1 = fileName1;
        }

        @Override
        public boolean matches(Object item) {
            SearchResult searchResult = (SearchResult) item;
            assertEquals(fileName1, searchResult.getFileName());
            return true;
        }

        @Override
        public void describeTo(Description description) {

        }
    }

    private final class SponsoredResultMatcher extends BaseMatcher<List<SponsoredResult>> {
        private final String title;

        private final String url;

        private final String displayUrl;

        private SponsoredResultMatcher(String title, String url, String displayUrl) {
            this.title = title;
            this.url = url;
            this.displayUrl = displayUrl;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean matches(Object item) {
            List<SponsoredResult> sponsoredResult = (List<SponsoredResult>) item;
            assertEquals(1, sponsoredResult.size());
            SponsoredResult result1 = sponsoredResult.get(0);
            assertEquals(title, result1.getTitle());
            assertEquals(displayUrl, result1.getVisibleUrl());
            assertEquals(url, result1.getUrl());

            return true;
        }

        @Override
        public void describeTo(Description description) {

        }
    }

    private final class TestSearchDetails implements SearchDetails {

        private final SearchCategory searchCategory;

        private final String query;

        private final SearchType searchType;

        public TestSearchDetails(String query, SearchCategory searchCategory, SearchType searchType) {
            this.query = query;
            this.searchCategory = searchCategory;
            this.searchType = searchType;
        }

        @Override
        public SearchCategory getSearchCategory() {
            return searchCategory;
        }

        @Override
        public String getSearchQuery() {
            return query;
        }

        @Override
        public SearchType getSearchType() {
            return searchType;
        }
        
        @Override
        public Map<FilePropertyKey, String> getAdvancedDetails() {
            return Collections.emptyMap();
        }
    }
    
    @SuppressWarnings("unchecked")
    public void testStop() {
        Mockery context = new Mockery();
        
        final byte[] guid = new byte[] {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
        
        final EventBroadcaster<SearchEvent> searchEventBroadcaster = context.mock(EventBroadcaster.class);
        final QueryReplyListenerList listenerList = context.mock(QueryReplyListenerList.class);
        final SearchServices searchServices = context.mock(SearchServices.class);
        final SearchListener listener = context.mock(SearchListener.class);
        
        final CoreSearch search = new CoreSearch(null, searchServices, listenerList, null, null, null, null, searchEventBroadcaster, null, null);
        
        context.checking(new Expectations() {
            {
                exactly(1).of(searchEventBroadcaster).broadcast(with(any(SearchEvent.class)));
                exactly(1).of(listenerList).removeQueryReplyListener(with(same(guid)),
                        with(any(QueryReplyListener.class)));
                exactly(1).of(searchServices).stopQuery(new GUID(guid));
                exactly(1).of(listener).searchStopped(search);
            }});
            
        search.stop();

        search.searchGuid = guid;
        search.processingStarted.set(true);
        search.addSearchListener(listener);
        search.stop();
        
        context.assertIsSatisfied();
    }
    
    @SuppressWarnings("unchecked")
    public void testRestart() {
        Mockery context = new Mockery();
        
        final byte[] guid1 = new byte[] {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
        final byte[] guid2 = new byte[] {0,1,2,3,4,5,6,'x','n',9,10,11,12,13,14,15};
        
        final EventBroadcaster<SearchEvent> searchEventBroadcaster = context.mock(EventBroadcaster.class);
        final QueryReplyListenerList listenerList = context.mock(QueryReplyListenerList.class);
        final SearchServices searchServices = context.mock(SearchServices.class);
        final SearchListener listener = context.mock(SearchListener.class);
        final SearchDetails details = context.mock(SearchDetails.class);
        
        final CoreSearch search = new CoreSearch(details, searchServices, listenerList, null, null, null, null, searchEventBroadcaster, null, null);
        
        context.checking(new Expectations() {
            {
                exactly(1).of(searchEventBroadcaster).broadcast(new SearchEvent(search, SearchEvent.Type.STOPPED));
                exactly(1).of(listenerList).removeQueryReplyListener(with(same(guid1)),
                        with(any(QueryReplyListener.class)));
                exactly(1).of(searchServices).stopQuery(new GUID(guid1));
                exactly(1).of(listener).searchStopped(search);
                exactly(1).of(listener).searchStarted(with(any(Search.class)));
                exactly(1).of(searchEventBroadcaster).broadcast(new SearchEvent(search, SearchEvent.Type.STARTED));
                exactly(1).of(listenerList).addQueryReplyListener(with(same(guid2)),
                        with(any(QueryReplyListener.class)));
                exactly(1).of(searchServices).queryWhatIsNew(with(same(guid2)),
                        with(same(MediaType.getOtherMediaType())));
                
                allowing(searchServices).newQueryGUID();
                will(returnValue(guid2));
                allowing(details).getSearchType();
                will(returnValue(SearchType.WHATS_NEW));
                allowing(details).getSearchCategory();
                will(returnValue(SearchCategory.OTHER));
                
            }});
            
        
        try {
            search.repeat();
            fail("Should not be able to repeat a search that has not started");
        } catch (IllegalStateException e) {
            // Expected
        }

        search.searchGuid = guid1;
        search.processingStarted.set(true);
        search.addSearchListener(listener);
        search.repeat();
        
        context.assertIsSatisfied();
    }
}
