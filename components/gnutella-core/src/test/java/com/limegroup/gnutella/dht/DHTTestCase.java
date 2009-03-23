package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Contact.State;
import org.limewire.mojito.routing.impl.RemoteContact;
import org.limewire.mojito.settings.ContextSettings;

import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.util.LimeTestCase;

public abstract class DHTTestCase extends LimeTestCase {
    
    protected static final int BOOTSTRAP_DHT_PORT = 3000;
    
    protected static final int PORT = 6667;
    
    private boolean bootstrapped = false;
    
    public DHTTestCase(String name) {
        super(name);
    }
    
    protected MojitoDHT startBootstrapDHT(LifecycleManager lifeCycleManager) throws Exception {
        assertFalse("bootstrap DHT already started", bootstrapped);
        bootstrapped = true;
        
        // setup bootstrap node
        MojitoDHT bootstrapDHT = MojitoFactory.createDHT("bootstrapNode");
        bootstrapDHT.bind(new InetSocketAddress(BOOTSTRAP_DHT_PORT));
        bootstrapDHT.start();
        
        org.limewire.core.settings.NetworkSettings.PORT.setValue(PORT);
        ConnectionSettings.FORCED_PORT.setValue(PORT);
        
        assertEquals("unexpected port", PORT, org.limewire.core.settings.NetworkSettings.PORT.getValue());
        
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        lifeCycleManager.start();
        
        return bootstrapDHT;
    }
    
    protected void fillRoutingTable(RouteTable rt, int numNodes) {
        for(int i = 0; i < numNodes; i++) {
            KUID kuid = KUID.createRandomID();
            RemoteContact node = new RemoteContact(
                    new InetSocketAddress("localhost",4000+i),
                    ContextSettings.getVendor(),
                    ContextSettings.getVersion(),
                    kuid,
                    new InetSocketAddress("localhost",4000+i),
                    0,
                    Contact.DEFAULT_FLAG,
                    State.UNKNOWN);
            rt.add(node);
        }
    }

}
