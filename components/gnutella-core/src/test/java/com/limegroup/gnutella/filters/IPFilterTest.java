package com.limegroup.gnutella.filters;

import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.settings.FilterSettings;
import org.limewire.io.IP;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.filters.IPFilter.IPFilterCallback;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.PushRequestImpl;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.LimeTestCase;

public class IPFilterTest extends LimeTestCase {

    private byte[] whiteListedAddress;
    private byte[] blackListedAddress;
    private InetAddress whiteListedIP;
    private InetAddress blackListedIP;
    
    private Mockery context;
    
    private PingReply pingReply;
    private QueryReply queryReply;
    private QueryRequest queryRequest;
    private IPFilter hostileFilter;
    
    private IPFilter filter;

    public IPFilterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(IPFilterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private Injector injector;
    
    @Override
    protected void setUp() throws Exception {
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(new String[] {
                "18.239.0.*", "13.0.0.0" });
        FilterSettings.WHITE_LISTED_IP_ADDRESSES
                .setValue(new String[] { "18.239.0.144" });
        
        
        whiteListedAddress = new byte[] { (byte) 18, (byte) 239, (byte) 0, (byte) 144 };
        blackListedAddress = new byte[] { (byte) 18, (byte) 239, (byte) 0, (byte) 143 };
        
        whiteListedIP = InetAddress.getByAddress(whiteListedAddress);
        blackListedIP = InetAddress.getByAddress(blackListedAddress);
        
        context = new Mockery();

        pingReply    = context.mock(PingReply.class);
        queryReply   = context.mock(QueryReply.class);
        queryRequest = context.mock(QueryRequest.class);
        hostileFilter = context.mock(IPFilter.class);

        Module m = new AbstractModule() {
            @Override
            public void configure() {
                bind(IPFilter.class).annotatedWith(Names.named("hostileFilter")).toInstance(hostileFilter);
            }
        };
        context.checking(new Expectations(){{
            atLeast(1).of(hostileFilter).refreshHosts();
        }});
	
        injector = LimeTestUtils.createInjector(m);
        filter = injector.getInstance(IPFilter.class);
        final CountDownLatch loaded = new CountDownLatch(1);
        IPFilterCallback ipfc = new IPFilter.IPFilterCallback() {
            public void ipFiltersLoaded() {
                loaded.countDown();
            }
        };
        filter.refreshHosts(ipfc);
        loaded.await();
        context.assertIsSatisfied();
    }
    
    public void testFilterByAddress() {
        context.checking(new Expectations() {{
            one(hostileFilter).allow(new IP("18.240.0.0"));
            will(returnValue(Boolean.TRUE));
            one(hostileFilter).allow(new IP("13.0.0.1"));
            will(returnValue(Boolean.TRUE));
        }});
        assertTrue(filter.allow("18.240.0.0"));
        assertFalse(filter.allow("18.239.0.142"));
        assertTrue(filter.allow("18.239.0.144"));
        assertFalse(filter.allow("13.0.0.0"));
        assertTrue(filter.allow("13.0.0.1"));
        context.assertIsSatisfied();
    }
    
    public void testDelegatesToHostile() {
        context.checking(new Expectations() {{
            one(hostileFilter).allow(new IP("18.240.0.0"));
            will(returnValue(Boolean.FALSE));
            one(hostileFilter).allow(new IP("13.0.0.1"));
            will(returnValue(Boolean.FALSE));
        }});
        assertFalse(filter.allow("18.240.0.0"));
        assertFalse(filter.allow("18.239.0.142"));
        assertTrue(filter.allow("18.239.0.144"));
        assertFalse(filter.allow("13.0.0.0"));
        assertFalse(filter.allow("13.0.0.1"));
        context.assertIsSatisfied();
    }
    
    public void testNotDelegatesToHostile() {
        FilterSettings.USE_NETWORK_FILTER.setValue(false);
        assertTrue(filter.allow("18.240.0.0"));
        assertFalse(filter.allow("18.239.0.142"));
        assertTrue(filter.allow("18.239.0.144"));
        assertFalse(filter.allow("13.0.0.0"));
        assertTrue(filter.allow("13.0.0.1"));
        context.assertIsSatisfied();
    }

    public void testFilterByPingReplyWhiteListed() {
     
        context.checking(new Expectations()
        {{ allowing (pingReply).getTTL();
           will(returnValue(3));
           allowing (pingReply).getPort();
           will(returnValue(6346));
           atLeast(1).of(pingReply).getAddress();
           will(returnValue(whiteListedIP.getHostAddress()));
           allowing(pingReply).isUltrapeer();
           will(returnValue(false));
        }});
        
        assertTrue(filter.allow(pingReply));
        context.assertIsSatisfied();
    }
    
    public void testFilterByPingReplyBlackListed() {
        
        context.checking(new Expectations()
        {{ allowing (pingReply).getTTL();
           will(returnValue(3));
           allowing (pingReply).getPort();
           will(returnValue(6346));
           atLeast(1).of(pingReply).getAddress();
           will(returnValue(blackListedIP.getHostAddress()));
           allowing(pingReply).isUltrapeer();
           will(returnValue(false));
        }});
        
        assertFalse(filter.allow(pingReply));
        context.assertIsSatisfied();
    }

    /** 
     *  Checks that only queries that desire oob are filtered.
     */
    public void testFilterByQueryRequest() {
        context.checking(new Expectations()
        {{ 
            one(queryRequest).desiresOutOfBandReplies();
            will(returnValue(false));
        }});
        
        assertTrue(filter.allow(queryRequest));
        context.assertIsSatisfied();
        
        final byte[] guid = new byte[16];
        guid[0] = 1;
        final IP toMatch = new IP("1.0.0.0");
        context.checking(new Expectations()
        {{ 
            one(queryRequest).desiresOutOfBandReplies();
            will(returnValue(true));
            one(queryRequest).getGUID();
            will(returnValue(guid));
            one(hostileFilter).allow(toMatch);
            will(returnValue(true));
        }});
        
        assertTrue(filter.allow(queryRequest));
        context.assertIsSatisfied();
        
        guid[0] = 13;
        context.checking(new Expectations()
        {{ 
            one(queryRequest).desiresOutOfBandReplies();
            will(returnValue(true));
            one(queryRequest).getGUID();
            will(returnValue(guid));
        }});
        assertFalse(filter.allow(queryRequest));
        context.assertIsSatisfied();
    }
    
    public void testFilterByQuery() {
        
        context.checking(new Expectations()
        {{ allowing (queryReply).getTTL();
           will(returnValue(3));
           allowing (queryReply).getPort();
           will(returnValue(6346));
           atLeast(1).of(queryReply).getIPBytes();
           will(returnValue(blackListedAddress));
           allowing(queryReply).getIP();
           will(returnValue(blackListedIP.getHostAddress()));
           allowing(queryReply).isMulticast();
           will(returnValue(false));
        }});
                
        assertFalse(filter.allow(queryReply));
        context.assertIsSatisfied();
    }

    public void testFilterByPushRequest() {

        PushRequest push1 = new PushRequestImpl(new byte[16], (byte) 3,
                new byte[16], 0l, whiteListedAddress, 6346);
        assertTrue(filter.allow(push1));

        PushRequest push2 = new PushRequestImpl(new byte[16], (byte) 3,
                new byte[16], 0l, blackListedAddress, 6346);
        assertFalse(filter.allow(push2));
    }

    public void testWhitelistGivesMaximumLogMinDistance() {
        assertEquals(32, filter.logMinDistanceTo(new IP(whiteListedAddress)));
    }
}
