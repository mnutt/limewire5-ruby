package com.limegroup.gnutella.util;

import java.net.UnknownHostException;
import java.util.Set;
import java.util.TreeSet;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.ConnectionManager;

public class MockUtils {

    public static ConnectionManager createConnectionManagerWithPushProxies(Mockery context) throws UnknownHostException {
        Set<Connectable> proxies = new TreeSet<Connectable>(IpPort.COMPARATOR);
        proxies.add(new ConnectableImpl("192.168.0.1", 5555, false));
        proxies.add(new ConnectableImpl("192.168.0.2", 6666, true));
        return createConnectionManagerWithPushProxies(context, proxies);
    }
    
    public static ConnectionManager createConnectionManagerWithPushProxies(Mockery context, final Set<Connectable> pushProxies) {
        ConnectionManager connectionManager = context.mock(ConnectionManager.class);
        return mockWithPushProxies(context, connectionManager, pushProxies);
    }
    
    public static ConnectionManager mockWithPushProxies(Mockery context, final ConnectionManager connectionManager, final Set<Connectable> pushProxies) { 
        context.checking(new Expectations() {{
            allowing(connectionManager).getPushProxies();
            will(returnValue(pushProxies));
        }});
        return connectionManager;
    }
    
}
