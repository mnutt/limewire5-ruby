package org.limewire.core.impl.search;

import org.limewire.core.api.search.SearchFactory;
import org.limewire.core.api.search.friend.FriendAutoCompleters;
import org.limewire.core.impl.search.friend.FriendAutoCompletersImpl;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class CoreGlueSearchModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(SearchFactory.class).toProvider(FactoryProvider.newFactory(SearchFactory.class, CoreSearch.class));
        bind(FriendAutoCompleters.class).to(FriendAutoCompletersImpl.class);
    }

}
