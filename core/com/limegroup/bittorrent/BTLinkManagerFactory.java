package com.limegroup.bittorrent;

import com.google.inject.Singleton;

@Singleton
public class BTLinkManagerFactory {
    
    public BTLinkManager getLinkManager() {
        return new BTLinkManager();
    }
}
