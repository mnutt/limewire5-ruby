package com.limegroup.gnutella.filters;

import java.util.Vector;

import org.limewire.core.settings.FilterSettings;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
class SpamFilterFactoryImpl implements SpamFilterFactory {
    
    private final Provider<MutableGUIDFilter> mutableGUIDFilter;
    private final Provider<HostileFilter> hostileFilter;
    private final Provider<LocalIPFilter> ipFilter;
    private final Provider<URNFilter> urnFilter;

    @Inject
    public SpamFilterFactoryImpl(Provider<MutableGUIDFilter> mutableGUIDFilter, 
            Provider<HostileFilter> hostileFilter,
            Provider<LocalIPFilter> ipFilter, Provider<URNFilter> urnFilter) {
        this.mutableGUIDFilter = mutableGUIDFilter;
        this.hostileFilter = hostileFilter;
        this.ipFilter = ipFilter;
        this.urnFilter = urnFilter;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.filters.SpamFilterFactory#createPersonalFilter()
     */
    public SpamFilter createPersonalFilter() {
        
        Vector<SpamFilter> buf=new Vector<SpamFilter>();

        //1. IP-based techniques.
        LocalIPFilter ipFilter = this.ipFilter.get();
        if(ipFilter.hasBlacklistedHosts())
            buf.add(ipFilter);

        //2. Keyword-based techniques.
        String[] badWords = FilterSettings.BANNED_WORDS.getValue();
        String[] badExtensions = FilterSettings.BANNED_EXTENSIONS.getValue();
        
        boolean filterAdult = FilterSettings.FILTER_ADULT.getValue();
        
        if (badWords.length!=0 || badExtensions.length!=0 || filterAdult) {
            KeywordFilter kf=new KeywordFilter();
            for (String badWord : badWords)
                kf.disallow(badWord);
            for (String badExt : badExtensions)
                kf.disallow(badExt);
            if (filterAdult)
                kf.disallowAdult();

            buf.add(kf);
        }

        //3. Spammy Replies
        SpamReplyFilter spf=new SpamReplyFilter();
        buf.add(spf);
        
        //4. Mutable GUID-based filters.
        buf.add(mutableGUIDFilter.get());
        
        //5. URN filters
        if(FilterSettings.FILTER_URNS.getValue())
            buf.add(urnFilter.get());

        return compose(buf);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.filters.SpamFilterFactory#createRouteFilter()
     */
    public SpamFilter createRouteFilter() {
        //Assemble spam filters. Order matters a little bit.
        
        Vector<SpamFilter>  buf=new Vector<SpamFilter> ();

        //1. Eliminate old LimeWire requeries.
        buf.add(new RequeryFilter());        

        //1b. Eliminate runaway Qtrax queries.
        buf.add(new GUIDFilter());

        //2. Duplicate-based techniques.
        if (FilterSettings.FILTER_DUPLICATES.getValue())
            buf.add(new DuplicateFilter());

        //3. Greedy queries.  Yes, this is a route filter issue.
        if (FilterSettings.FILTER_GREEDY_QUERIES.getValue())
            buf.add(new GreedyQueryFilter());

        //4. Queries containing hash urns.
        if (FilterSettings.FILTER_HASH_QUERIES.getValue())
            buf.add(new HashFilter());
        
        // always filter hostiles
        buf.add(hostileFilter.get());

        return compose(buf);
    }

    /**
     * Returns a composite filter of the given filters.
     * @param filters a Vector of SpamFilter.
     */
    private static SpamFilter compose(Vector<? extends SpamFilter>  filters) {
        //As a minor optimization, we avoid a few method calls in
        //special cases.
        if (filters.size()==0)
            return new AllowFilter();
        else if (filters.size()==1)
            return filters.get(0);
        else {
            SpamFilter[] delegates=new SpamFilter[filters.size()];
            filters.copyInto(delegates);
            return new CompositeFilter(delegates);
        }
    }

}
