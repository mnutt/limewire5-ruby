package com.limegroup.gnutella.downloader;

import org.limewire.core.settings.DownloadSettings;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.UDPPinger;

@Singleton
public class SourceRankerFactory {
    
    private final NetworkManager networkManager;
    private final Provider<UDPPinger> udpPingerFactory;
    private final Provider<MessageRouter> messageRouter;
    private final RemoteFileDescFactory remoteFileDescFactory;
    
    @Inject
    public SourceRankerFactory(NetworkManager networkManager,
                               Provider<UDPPinger> udpPingerFactory, 
                               Provider<MessageRouter> messageRouter,
                               RemoteFileDescFactory remoteFileDescFactory) {
        this.networkManager = networkManager;
        this.udpPingerFactory = udpPingerFactory;
        this.messageRouter = messageRouter;
        this.remoteFileDescFactory = remoteFileDescFactory;
    }

    PingRanker createPingRanker() {
        return new PingRanker(networkManager, udpPingerFactory.get(), messageRouter.get(), remoteFileDescFactory);
    }
    
    FriendsFirstSourceRanker createFriendsFirstSourceRanker() {
        return new FriendsFirstSourceRanker(createPingRanker());
    }
    
    /**
     * @return a ranker appropriate for our system's capabilities.
     */
    public SourceRanker getAppropriateRanker() {
        if (networkManager.canReceiveSolicited() && 
                DownloadSettings.USE_HEADPINGS.getValue())
            return createFriendsFirstSourceRanker();
        else 
            return new LegacyRanker();
    }

    /**
     * @param original the current ranker that we use
     * @return the ranker that should be used.  If different than the current one,
     * the current one is stopped.
     */
    public SourceRanker getAppropriateRanker(SourceRanker original) {
        if(original == null)
            return getAppropriateRanker();
        
        SourceRanker better;
        if (networkManager.canReceiveSolicited() && 
                DownloadSettings.USE_HEADPINGS.getValue()) {
            if (original instanceof FriendsFirstSourceRanker)
                return original;
            better = createFriendsFirstSourceRanker();
        }else {
            if (original instanceof LegacyRanker)
                return original;
            better = new LegacyRanker();
        }
        
        better.setMeshHandler(original.getMeshHandler());
        better.addToPool(original.getShareableHosts());
        original.stop();
        return better;
    }
}
