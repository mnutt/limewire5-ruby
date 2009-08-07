package org.limewire.http.webservice;

import java.util.HashMap;

import org.cometd.Bayeux;
import org.cometd.Client;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.search.SearchListener;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.impl.search.SearchManagerImpl.SearchWithResults;
import org.mortbay.cometd.BayeuxService;
import org.mortbay.log.Log;

public class CometSearchService extends BayeuxService {
    private SearchManager searchManager;
    private LibraryManager libraryManager;
    
    public CometSearchService(Bayeux bayeux, SearchManager searchManager, LibraryManager libraryManager) {
        super(bayeux, "search");
        System.out.println("Setting up search...");
        subscribe("/search", "searchResults");
        this.searchManager = searchManager;
        this.libraryManager = libraryManager;
    }
    
    public Object searchResults(Client client, Object data) {
        System.out.println("ECHO from "+client+" "+data);

        try {
            String query = data.toString();
            SearchListener listener = new CometSearchListener(getBayeux().getChannel("/search", false), getClient(), this.libraryManager);
            SearchWithResults search = this.searchManager.createSearchFromQuery(query);
            search.start();
            
            search.getSearch().addSearchListener(listener);
            
            HashMap<String, Object> response = new HashMap<String,Object>();
            response.put("new_search", true);
            response.put("guid", search.getSearch().getQueryGuid());
            
            return response;
        } catch(Exception e) {
            Log.warn(e);
            return "bad results for " + data;
        }

    }
}