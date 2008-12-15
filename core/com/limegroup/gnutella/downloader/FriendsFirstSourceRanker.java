package com.limegroup.gnutella.downloader;

import java.util.Collection;
import java.util.Collections;

import org.limewire.collection.MultiCollection;
import org.limewire.io.Address;
import org.limewire.io.Connectable;

import com.limegroup.gnutella.PushEndpoint;

/**
 * Delegates Gnutella rfds to {@link PingRanker} and non-gnutella rfds
 * to the {@link LegacyRanker}.
 */
public class FriendsFirstSourceRanker extends AbstractSourceRanker {

    private final LegacyRanker legacyRanker = new LegacyRanker();
    
    private final PingRanker pingRanker;
    
    public FriendsFirstSourceRanker(PingRanker pingRanker) {
        this.pingRanker = pingRanker;
    }
    
    @Override
    public boolean addToPool(Collection<? extends RemoteFileDescContext> hosts) {
        boolean added = false;
        for (RemoteFileDescContext rfdContext : hosts) {
            Address address = rfdContext.getAddress();
            if (address instanceof Connectable || address instanceof PushEndpoint) {
                added |= pingRanker.addToPool(rfdContext);
            } else {
                added |= legacyRanker.addToPool(rfdContext);
            }
        }
        return added;
    }

    @Override
    public boolean addToPool(RemoteFileDescContext host) {
        return addToPool(Collections.singleton(host));
    }

    @Override
    public RemoteFileDescContext getBest() {
        RemoteFileDescContext best = legacyRanker.getBest();
        return best != null ? best : pingRanker.getBest();
    }

    @Override
    public int getNumKnownHosts() {
        return pingRanker.getNumKnownHosts() + legacyRanker.getNumKnownHosts();
    }

    @Override
    protected Collection<RemoteFileDescContext> getPotentiallyBusyHosts() {
        return new MultiCollection<RemoteFileDescContext>(pingRanker.getPotentiallyBusyHosts(), legacyRanker.getPotentiallyBusyHosts());
    }

    @Override
    public Collection<RemoteFileDescContext> getShareableHosts() {
        return new MultiCollection<RemoteFileDescContext>(pingRanker.getShareableHosts(), legacyRanker.getShareableHosts());
    }

    @Override
    public boolean hasMore() {
        return pingRanker.hasMore() || legacyRanker.hasMore();
    }

}
