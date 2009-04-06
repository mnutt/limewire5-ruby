package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.net.InetAddress;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.inject.Providers;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.GUID;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.util.ByteUtils;

import com.limegroup.gnutella.filters.IPFilter;

public class PushEndpointFactoryImplTest extends LimeTestCase {

    private Mockery context;
    private PushEndpointCache pushEndpointCache;
    private NetworkInstanceUtils networkInstanceUtils;
    private IPFilter hostilesFilter;
    private PushEndpointFactoryImpl pushEndpointFactory;

    public PushEndpointFactoryImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        pushEndpointCache = context.mock(PushEndpointCache.class);
        networkInstanceUtils = context.mock(NetworkInstanceUtils.class);
        hostilesFilter = context.mock(IPFilter.class);
        pushEndpointFactory = new PushEndpointFactoryImpl(Providers.of(pushEndpointCache), Providers.of((SelfEndpoint)null), networkInstanceUtils, Providers.of(hostilesFilter));  
    }
    
    public void testIsGoodPushProxy() throws Exception {
        
        final Connectable connectable = new ConnectableImpl("200.200.200.200:5555", false);
        
        context.checking(new Expectations() {{
            // first call
            one(networkInstanceUtils).isPrivateAddress(connectable.getInetAddress());
            will(returnValue(true));
            // second call
            one(networkInstanceUtils).isPrivateAddress(connectable.getInetAddress());
            will(returnValue(false));
            one(hostilesFilter).allow(connectable);
            will(returnValue(false));
            // third call
            one(networkInstanceUtils).isPrivateAddress(connectable.getInetAddress());
            will(returnValue(false));
            one(hostilesFilter).allow(connectable);
            will(returnValue(true));
        }});
        
        assertFalse(pushEndpointFactory.isGoodPushProxy(connectable));
        assertFalse(pushEndpointFactory.isGoodPushProxy(connectable));
        assertTrue(pushEndpointFactory.isGoodPushProxy(connectable));
        
        context.assertIsSatisfied();
    }

    public void testCreateFromHttpStringIgnoresHostilePushProxies() throws Exception {
        context.checking(new Expectations() {{
            allowing(networkInstanceUtils).isPrivateAddress(with(any(InetAddress.class)));
            will(returnValue(false));
            // disallow first proxy
            one(hostilesFilter).allow(with(any(Connectable.class)));
            will(returnValue(false));
            // allow second proxy
            one(hostilesFilter).allow(with(any(Connectable.class)));
            will(returnValue(true));
        }});
        
        PushEndpoint pushEndpoint = pushEndpointFactory.createPushEndpoint("FFB3EC3B9D93A8F9CE42AED28F674900;fwt/1;222.222.222.222:2222;10.10.10.10:5555");
        
        assertEquals(1, pushEndpoint.getProxies().size());
        assertContains(pushEndpoint.getProxies(), new ConnectableImpl("10.10.10.10:5555", false));
        
        context.assertIsSatisfied();
    }
    
    public void testCreateFromBytesIgnoresHostilePushProxies() throws Exception {
        context.checking(new Expectations() {{
            allowing(networkInstanceUtils).isPrivateAddress(with(any(InetAddress.class)));
            will(returnValue(false));
            // disallow first proxy
            one(hostilesFilter).allow(with(any(Connectable.class)));
            will(returnValue(false));
            // allow second proxy
            one(hostilesFilter).allow(with(any(Connectable.class)));
            will(returnValue(true));
        }});
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 2 proxies and fwt version 0
        out.write(2 | 0 << 3);
        out.write(new GUID().bytes());
        out.write(new byte[] { (byte)129, 12, 1, 1 });
        ByteUtils.short2leb((short)5555, out);
        out.write(new byte[] { 10, 10, 10, 10 });
        ByteUtils.short2leb((short)5555, out);
        
        PushEndpoint pushEndpoint = pushEndpointFactory.createFromBytes(new DataInputStream(new ByteArrayInputStream(out.toByteArray())));
        
        assertEquals(1, pushEndpoint.getProxies().size());
        assertContains(pushEndpoint.getProxies(), new ConnectableImpl("10.10.10.10:5555", false));
        
        context.assertIsSatisfied();
    }
}
