package org.limewire.ui.swing.search;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.io.Address;
import org.limewire.io.ConnectableImpl;
import org.limewire.net.address.AddressFactory;
import org.limewire.ui.swing.library.nav.LibraryNavigator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class P2PLinkSearchHandler implements SearchHandler {
    
    private final RemoteLibraryManager remoteLibraryManager;
    private final LibraryNavigator libraryNavigator;
    private final AddressFactory addressFactory;

    @Inject
    P2PLinkSearchHandler(RemoteLibraryManager remoteLibraryManager, LibraryNavigator libraryNavigator,
                         AddressFactory addressFactory) {
        this.remoteLibraryManager = remoteLibraryManager;
        this.libraryNavigator = libraryNavigator;
        this.addressFactory = addressFactory;
    }
    
    @Override
    public void doSearch(SearchInfo info) {
        String q = info.getQuery().substring("p2p://".length());
        final FriendPresence presence = createFriendPresence(q);
        remoteLibraryManager.addPresenceLibrary(presence);
        // Run this later, to allow the library a bit of time to render the friend.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                libraryNavigator.selectFriendLibrary(presence.getFriend());
            }
        });
    }
    
    private FriendPresence createFriendPresence(String info) {
        return new SimpleFriendPresence(info, addressFactory);
    }
    
    private static class SimpleFriendPresence implements FriendPresence {
        private final String id;
        private final String name;
        private final Map<URI, Feature> features;
        
        public SimpleFriendPresence(String info, AddressFactory addressFactory) {
            Address address;
            try {
                 address = addressFactory.deserialize(info);
            } catch(IOException ioe) {
                address = ConnectableImpl.INVALID_CONNECTABLE;
            }
            
            Map<URI, Feature> map = new HashMap<URI, Feature>();
            map.put(AddressFeature.ID, new AddressFeature(address));
            this.features = Collections.unmodifiableMap(map);
            this.id = info;
            this.name = address.getAddressDescription();
        }
        
        @Override
        public void addFeature(Feature feature) {
        }

        @Override
        public Feature getFeature(URI id) {
            return features.get(id);
        }

        @Override
        public Collection<Feature> getFeatures() {
            return features.values();
        }

        @Override
        public Friend getFriend() {
            return new SimpleFriend(this, name);
        }

        @Override
        public String getPresenceId() {
            return id;
        }

        @Override
        public boolean hasFeatures(URI... id) {
            for(URI uri : id) {
                if(!features.containsKey(uri)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void removeFeature(URI id) {
        }
    }
    
    private static class SimpleFriend implements Friend {
        private final String name;
        private final FriendPresence presence;
        
        public SimpleFriend(FriendPresence presence, String name) {
            this.presence = presence;
            this.name = name;
        }

        @Override
        public Map<String, FriendPresence> getFriendPresences() {
            Map<String, FriendPresence> map = new HashMap<String, FriendPresence>();
            map.put(presence.getPresenceId(), presence);
            return map;
        }

        @Override
        public String getId() {
            return presence.getPresenceId();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Network getNetwork() {
            return null;
        }

        @Override
        public String getRenderName() {
            return name;
        }

        @Override
        public String getFirstName() {
            return name;
        }

        @Override
        public boolean isAnonymous() {
            return true;
        }

        @Override
        public void setName(String name) {
        }
    }

}
