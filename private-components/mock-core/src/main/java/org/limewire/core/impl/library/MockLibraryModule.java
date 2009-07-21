package org.limewire.core.impl.library;

import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.MetaDataManager;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.SharedFileListManager;

import com.google.inject.AbstractModule;


public class MockLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(LibraryManager.class).to(MockLibraryManager.class);
        bind(RemoteLibraryManager.class).to(MockRemoteLibraryManager.class);
        bind(SharedFileListManager.class).to(MockLibraryManager.class);
        bind(MagnetLinkFactory.class).to(MockMagnetLinkFactoryImpl.class);        
        bind(MetaDataManager.class).to(MockMetaDataManager.class);
        bind(FriendAutoCompleterFactory.class).to(MockFriendAutoCompleterFactory.class);
    }

}
