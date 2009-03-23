package com.limegroup.gnutella.dht;

import java.net.SocketAddress;
import java.util.List;

import org.limewire.io.IpPort;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.routing.Contact;

import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;

public class DHTControllerStub implements DHTController {
    
    private final MojitoDHT dht;

    private final DHTMode mode;
    
    public DHTControllerStub(MojitoDHT dht, DHTMode mode) {
        this.dht = dht;
        this.mode = mode;
    }

    public void addActiveDHTNode(SocketAddress hostAddress) {
    }
    
    public void addPassiveDHTNode(SocketAddress hostAddress) {
    }

    public List<IpPort> getActiveDHTNodes(int maxNodes) {
        return null;
    }
    
    public MojitoDHT getMojitoDHT() {
        return dht;
    }

    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
    }
    
    public boolean isRunning() {
        return false;
    }

    public boolean isWaitingForNodes() {
        return false;
    }

    public void sendUpdatedCapabilities() {
    }

    public void start() {
    }

    public void stop() {
    }

	public boolean isBootstrapped() {
		return dht.isBootstrapped();
	}

    public void addContact(Contact node) {
    }

    public DHTMode getDHTMode() {
        return mode;
    }
}
