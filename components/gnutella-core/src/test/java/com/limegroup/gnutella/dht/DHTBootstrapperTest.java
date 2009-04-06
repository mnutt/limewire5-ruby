package com.limegroup.gnutella.dht;

import java.net.InetSocketAddress;
import java.util.concurrent.Future;

import junit.framework.Test;

import org.limewire.core.settings.DHTSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.MojitoFactory;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.ContactFactory;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;

public class DHTBootstrapperTest extends DHTTestCase {
    
    private static Context dhtContext;
    
    private DHTBootstrapperImpl bootstrapper;

    private MojitoDHT bootstrapDHT;
    
    public DHTBootstrapperTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(DHTBootstrapperTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        DHTTestUtils.setSettings(PORT);
        MojitoDHT dht = MojitoFactory.createDHT();
        
        Injector injector = LimeTestUtils.createInjector(Stage.PRODUCTION);

        bootstrapDHT = startBootstrapDHT(injector.getInstance(LifecycleManager.class));
        
        DHTBootstrapperFactory dhtBootstrapperFactory = injector.getInstance(DHTBootstrapperFactory.class);
        bootstrapper = (DHTBootstrapperImpl)dhtBootstrapperFactory.createBootstrapper(new DHTControllerStub(dht, DHTMode.ACTIVE));
        dhtContext = (Context)dht;
        dhtContext.bind(new InetSocketAddress(2000));
        dhtContext.start();
    }

    @Override
    protected void tearDown() throws Exception {
        bootstrapper.stop();
        dhtContext.getRouteTable().clear();
        dhtContext.close();
        
        bootstrapDHT.close();
    }
    
    public void testAddBootstrapHost() throws Exception{
        fillRoutingTable(dhtContext.getRouteTable(), 2);
        //should be bootstrapping from routing table
        bootstrapper.bootstrap();
        DHTFuture future = bootstrapper.getPingFuture();
        Thread.sleep(300);
        assertTrue(bootstrapper.isBootstrappingFromRouteTable());
        
        // Now emulate reception of a DHT node from the Gnutella network
        bootstrapper.addBootstrapHost(bootstrapDHT.getContactAddress());
        assertTrue("ping future should be cancelled", future.isCancelled());
        
        Thread.sleep(200);
        future = bootstrapper.getBootstrapFuture();
        assertFalse("Should not be waiting", bootstrapper.isWaitingForNodes());
        assertNotNull(future);
        
        //should be bootstrapping
        assertTrue(dhtContext.isBootstrapping() || dhtContext.isBootstrapped());
        
        //now try adding more hosts -- should keep them but not bootstrap
        for(int i = 0; i < 30; i++) {
            bootstrapper.addBootstrapHost(new InetSocketAddress("1.2.3.4.5", i));
        }
        assertFalse(bootstrapper.isWaitingForNodes());
        assertFalse(future.isCancelled());
        
        //finish bootstrap
        future.get();
        assertTrue(dhtContext.isBootstrapped());
    }
    
    public void testNotCancelBootstrap() throws Exception {
        //now try bootstrapping of hostlist and see if it cancels (it should not)
        bootstrapper.bootstrap();
        Thread.sleep(100);
        assertTrue("Should be waiting", bootstrapper.isWaitingForNodes());
        bootstrapper.addBootstrapHost(new InetSocketAddress("localhost",5000));
        Future future = bootstrapper.getPingFuture();
        assertNotNull("Should be pinging", future);
        assertFalse(bootstrapper.isBootstrappingFromRouteTable());
        Thread.sleep(100);
        //now add other host: it should not cancel the previous attempt
        bootstrapper.addBootstrapHost(new InetSocketAddress("localhost",6000));
        assertFalse(future.isCancelled());
    }
    
    public void testGetSimppHosts() throws Exception{
        //set up SIMPP hosts
        String[] hosts = new String[] {
                "1.0.0.0.3:100","2.0.0.0.3:200","3.0.0.0.3:300"};
        
        DHTSettings.DHT_BOOTSTRAP_HOSTS.set(hosts);
        
        //only first hex counts
        KUID id = KUID.createWithHexString("03ED9650238A6C576C987793C01440A0EA91A1FB");
        Contact localNode = ContactFactory.createLocalContact(Vendor.UNKNOWN, Version.ZERO, id, 0, false);
        PrivilegedAccessor.setValue(dhtContext.getRouteTable(), "localNode", localNode);
        
        bootstrapper.bootstrap();
        InetSocketAddress addr = (InetSocketAddress) bootstrapper.getSimppHost();
        String address = addr.getHostName();
        int port = addr.getPort();
        assertEquals(address+":"+port, hosts[0]);
        
        //change first hex. Should point to last elem of the list
        id = KUID.createWithHexString("F3ED9650238A6C576C987793C01440A0EA91A1FB");
        localNode = ContactFactory.createLocalContact(Vendor.UNKNOWN, Version.ZERO, id, 0, false);
        PrivilegedAccessor.setValue(dhtContext.getRouteTable(), "localNode", localNode);
        addr = (InetSocketAddress) bootstrapper.getSimppHost();
        address = addr.getHostName();
        port = addr.getPort();
        assertEquals(address+":"+port, hosts[2]);
    }
    
    public void testListenToSimppHosts() throws Exception {
        bootstrapper.bootstrap();
        Thread.sleep(200);
        assertTrue(bootstrapper.isWaitingForNodes());
        //add bootstrap host post- bootstrap()
        DHTSettings.DHT_BOOTSTRAP_HOSTS.set(new String[] {"127.0.0.1:"+BOOTSTRAP_DHT_PORT});
        //this should trigger bootstrapping
        bootstrapper.simppUpdated(0);
        Thread.sleep(2000);
        assertFalse(bootstrapper.isWaitingForNodes());
        assertTrue(dhtContext.isBootstrapped());
    }
}
