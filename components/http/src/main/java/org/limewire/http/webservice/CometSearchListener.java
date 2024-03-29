package org.limewire.http.webservice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.cometd.Channel;
import org.cometd.Client;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.search.sponsored.SponsoredResult;


public class CometSearchListener implements SearchListener {

    private Channel channel;
    private Client client;
    private LibraryManager libraryManager;

    public CometSearchListener(Channel channel, Client client, LibraryManager libraryManager) {
        this.channel = channel;
        this.client = client;
        this.libraryManager = libraryManager;
        System.out.println("instantiated listener.");
    }
    
    @Override
    public void handleSearchResult(org.limewire.core.api.search.Search search,
            SearchResult searchResult) {
        HashMap<String, Object> response = new HashMap<String, Object>();
        
        response.put("filename", searchResult.getFileName());
        response.put("magnet_url", searchResult.getMagnetURL());
        response.put("category", searchResult.getCategory());
        response.put("spam", searchResult.isSpam());
        response.put("sha1", searchResult.getUrn().toString().replaceFirst("urn:sha1:", ""));
        response.put("album", searchResult.getProperty(FilePropertyKey.ALBUM));
        response.put("name", searchResult.getProperty(FilePropertyKey.NAME));
        response.put("title", searchResult.getProperty(FilePropertyKey.TITLE));
        response.put("author", searchResult.getProperty(FilePropertyKey.AUTHOR));
        response.put("created_at", searchResult.getProperty(FilePropertyKey.DATE_CREATED));
        response.put("length", searchResult.getProperty(FilePropertyKey.LENGTH));
        response.put("genre", searchResult.getProperty(FilePropertyKey.GENRE));
        response.put("file_size", searchResult.getSize());
        if(this.libraryManager.getLibraryManagedList().contains(searchResult.getUrn())) {
            response.put("in_library", true);
        }

        ArrayList<HashMap> sources = new ArrayList<HashMap>();
        for(RemoteHost source : searchResult.getSources()) {
            HashMap<String, String> sourceProps = new HashMap<String, String>();
            sourceProps.put("guid", source.getFriendPresence().getPresenceId());
            sourceProps.put("location", source.getFriendPresence().getFriend().getName());
            sources.add(sourceProps);
        }
        response.put("sources", sources);
        
        channel.publish(this.client, response, searchResult.getUrn().toString());
        System.out.println("got result");
    }

    @Override
    public void handleSearchResults(org.limewire.core.api.search.Search search,
            Collection<? extends SearchResult> searchResults) {
        for(SearchResult result : searchResults) {
            this.handleSearchResult(search, result);
        }      
    }

    @Override
    public void handleSponsoredResults(org.limewire.core.api.search.Search search,
            List<SponsoredResult> sponsoredResults) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void searchStarted(org.limewire.core.api.search.Search search) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void searchStopped(org.limewire.core.api.search.Search search) {
        // TODO Auto-generated method stub
        
    }
    
}