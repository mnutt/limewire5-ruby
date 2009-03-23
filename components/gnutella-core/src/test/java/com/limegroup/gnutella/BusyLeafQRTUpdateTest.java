package com.limegroup.gnutella;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.limegroup.gnutella.connection.ConnectionRoutingStatistics;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.routing.QRPUpdater;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class BusyLeafQRTUpdateTest extends LimeTestCase {
    
    private RoutedConnection leaf;
    private MessageRouterImpl mr;
    private Mockery mockery;
    
    @Override
    public void setUp() throws Exception {
        
        mockery = new Mockery();
        leaf = mockery.mock(RoutedConnection.class);
        
        final MyConnectionManagerStub mcms = new MyConnectionManagerStub();
        final QRPUpdaterStub qrtStub = new QRPUpdaterStub();
        Module module = new AbstractModule() {

            @Override
            protected void configure() {
                bind(ConnectionManager.class).toInstance(mcms);
                bind(QRPUpdater.class).toInstance(qrtStub);
            }
            
        };
        
        Injector injector = LimeTestUtils.createInjector(module);
        
        mr = (MessageRouterImpl) injector.getInstance(MessageRouter.class);
    }


    public void testBusyLeafExcluded() throws Exception {
        mockery.checking(new Expectations() {{
            one(leaf).isBusyLeaf();
            will(returnValue(true));
            never(leaf).getRoutedConnectionStatistics();
            allowing(leaf).getQRPLock();
            will(returnValue(new Object()));
        }});
        assertTrue(mr.createRouteTable().getPercentFull() == 0);
        mockery.assertIsSatisfied();
    }
    
    public void testNotBusyLeafIncluded() throws Exception {
        final ConnectionRoutingStatistics stats = mockery.mock(ConnectionRoutingStatistics.class);
        final QueryRouteTable received = new QueryRouteTable();
        received.addIndivisible("asdfasdfadsf");
        mockery.checking(new Expectations() {{
            one(leaf).isBusyLeaf();
            will(returnValue(false));
            one(leaf).getRoutedConnectionStatistics();
            will(returnValue(stats));
            one(stats).getQueryRouteTableReceived();
            will(returnValue(received));
            allowing(leaf).getQRPLock();
            will(returnValue(new Object()));
        }});
        assertEquals(received.getPercentFull(), mr.createRouteTable().getPercentFull());
        mockery.assertIsSatisfied();
    }
    
    /**
     * JUnit crap...
     */
    public BusyLeafQRTUpdateTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BusyLeafQRTUpdateTest.class);
    }
    
    private class MyConnectionManagerStub extends ConnectionManagerStub {
        public MyConnectionManagerStub() {
            super(null, null, null, null, null,
                    null, null, null, null,
                    null, null, null, null,
                    null, null);
        }
        
        @Override
        public List<RoutedConnection> getInitializedClientConnections() {
            List<RoutedConnection> ret = new ArrayList<RoutedConnection>();
            ret.add(leaf);
            return ret;
        }
    }
    
    private class QRPUpdaterStub extends QRPUpdater {

        public QRPUpdaterStub() {
            super(null, null, null);
        }

        @Override
        public synchronized QueryRouteTable getQRT() {
            return new QueryRouteTable();
        }
    }
}