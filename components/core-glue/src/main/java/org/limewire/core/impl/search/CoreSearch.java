package org.limewire.core.impl.search;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;
import org.limewire.core.api.search.sponsored.SponsoredResultTarget;
import org.limewire.core.impl.library.CoreRemoteFileItem;
import org.limewire.core.impl.library.FriendSearcher;
import org.limewire.core.impl.search.sponsored.CoreSponsoredResult;
import org.limewire.core.settings.PromotionSettings;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.promotion.PromotionSearcher;
import org.limewire.promotion.PromotionSearcher.PromotionSearchResultsCallback;
import org.limewire.promotion.containers.PromotionMessageContainer;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.geocode.CachedGeoLocation;
import com.limegroup.gnutella.messages.QueryReply;

public class CoreSearch implements Search {

    private final SearchDetails searchDetails;
    private final SearchServices searchServices;
    private final QueryReplyListenerList listenerList;
    private final PromotionSearcher promotionSearcher;
    private final FriendSearcher friendSearcher;
    private final CachedGeoLocation geoLocation;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<SearchListener> searchListeners = new CopyOnWriteArrayList<SearchListener>();
    private final QrListener qrListener = new QrListener();
    private final FriendSearchListener friendSearchListener = new FriendSearchListenerImpl();
    private final ScheduledExecutorService backgroundExecutor;
    
    private volatile byte[] searchGuid;

    @AssistedInject
    public CoreSearch(@Assisted
    SearchDetails searchDetails, SearchServices searchServices,
            QueryReplyListenerList listenerList, PromotionSearcher promotionSearcher,
            FriendSearcher friendSearcher,
            CachedGeoLocation geoLocation,
            @Named("backgroundExecutor")ScheduledExecutorService backgroundExecutor) {
        this.searchDetails = searchDetails;
        this.searchServices = searchServices;
        this.listenerList = listenerList;
        this.promotionSearcher = promotionSearcher;
        this.friendSearcher = friendSearcher;
        this.geoLocation = geoLocation;
        this.backgroundExecutor = backgroundExecutor;
    }
    
    @Override
    public SearchCategory getCategory() {
        return searchDetails.getSearchCategory();
    }
    
    @Override
    public void addSearchListener(SearchListener searchListener) {
        searchListeners.add(searchListener);
    }
    
    @Override
    public void removeSearchListener(SearchListener searchListener) {
        searchListeners.remove(searchListener);
    }

    @Override
    public void start() {
        if (started.getAndSet(true)) {
            throw new IllegalStateException("already started!");
        }
        
        for(SearchListener listener : searchListeners) {
            listener.searchStarted();
        }

        doSearch(true);
    }
    
    private void doSearch(boolean initial) {
        searchGuid = searchServices.newQueryGUID();
        listenerList.addQueryReplyListener(searchGuid, qrListener);
        
        switch(searchDetails.getSearchType()) {
        case KEYWORD:
            doKeywordSearch(initial);
            break;
        case WHATS_NEW:
            doWhatsNewSearch(initial);
            break;
        }
    }
    
    private void doWhatsNewSearch(boolean initial) {
        searchServices.queryWhatIsNew(searchGuid,
                MediaTypeConverter.toMediaType(searchDetails.getSearchCategory()));
        
        // TODO: Search friends too.
    }
    
    private void doKeywordSearch(boolean initial) {
        searchServices.query(searchGuid, searchDetails.getSearchQuery(), "",
                MediaTypeConverter.toMediaType(searchDetails.getSearchCategory()));
        
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() { 
                friendSearcher.doSearch(searchDetails, friendSearchListener);
            }
        });        
        
        if (initial && PromotionSettings.PROMOTION_SYSTEM_IS_ENABLED.getValue() && promotionSearcher.isEnabled()) {            
            final PromotionSearchResultsCallback callback = new PromotionSearchResultsCallback() {
                @Override
                public void process(PromotionMessageContainer result) {
                    SponsoredResultTarget target = result.getOptions().isOpenInStoreTab() ?
                        SponsoredResultTarget.STORE  : SponsoredResultTarget.EXTERNAL;
                    String title = result.getTitle();
                    String displayUrl = result.getDisplayUrl();
                    if(displayUrl.isEmpty()) {
                        displayUrl = stripURL(result.getURL());
                    }
                    if(title.isEmpty()) {
                        title = displayUrl;
                    }
                    CoreSponsoredResult coreSponsoredResult = new CoreSponsoredResult(
                            title, result.getDescription(),
                            displayUrl, result.getURL(),
                            target);
                    handleSponsoredResults(coreSponsoredResult);
                }
            };
            
            backgroundExecutor.execute(new Runnable() {
                @Override
                public void run() { 
                    promotionSearcher.search(searchDetails.getSearchQuery(), callback, geoLocation
                            .getGeocodeInformation());
                }
            });            
        }
    }
    
    /**
     * Strips "http://" and anything after ".com" (or .whatever) from the url
     */
    private String stripURL(String url){
        int dotIndex = url.indexOf('.');
        int endIndex = url.indexOf('/', dotIndex);
        endIndex = endIndex == -1 ? url.length() :  endIndex;
        int startIndex = url.indexOf("//");
        // this will either be 0 or the first character after "//"
        startIndex = startIndex == -1 ? 0 :  startIndex + 2;
        return url.substring(startIndex, endIndex);
    }
    
    @Override
    public void repeat() {
        if(!started.get()) {
            throw new IllegalStateException("must start!");
        }
        
        stop();
        
        for(SearchListener listener : searchListeners) {
            listener.searchStarted();
        }
        
        doSearch(false);
    }

    @Override
    public void stop() {
        if(!started.get()) {
            throw new IllegalStateException("must start!");
        }
        
        listenerList.removeQueryReplyListener(searchGuid, qrListener);
        searchServices.stopQuery(new GUID(searchGuid));
        
        for(SearchListener listener : searchListeners) {
            listener.searchStopped();
        }
    }


    public GUID getQueryGuid() {
        return new GUID(searchGuid);
    }

    
    private void handleSponsoredResults(SponsoredResult... sponsoredResults) {
        List<SponsoredResult> resultList =  Arrays.asList(sponsoredResults);
        for(SearchListener listener : searchListeners) {
            listener.handleSponsoredResults(resultList);
        }
    }
    
    private class QrListener implements QueryReplyListener {
        @Override
        public void handleQueryReply(RemoteFileDesc rfd, QueryReply queryReply,
                Set<? extends IpPort> locs) {
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResult(new RemoteFileDescAdapter(rfd, locs));
            }
        }
    }

    private class FriendSearchListenerImpl implements FriendSearchListener {
        public void handleFriendResults(Collection<RemoteFileItem> results) {
            for(RemoteFileItem remoteFileItem : results) {
                SearchResult searchResult = ((CoreRemoteFileItem)remoteFileItem).getSearchResult();
                for (SearchListener listener : searchListeners) {
                    listener.handleSearchResult(searchResult);
                }
            }            
        }
    }
}