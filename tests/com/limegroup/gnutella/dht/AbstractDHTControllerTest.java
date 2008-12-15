package com.limegroup.gnutella.dht;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import org.limewire.core.settings.DHTSettings;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTExecutorService;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueFactory;
import org.limewire.mojito.db.DHTValueFactoryManager;
import org.limewire.mojito.db.DHTValueType;
import org.limewire.mojito.db.Database;
import org.limewire.mojito.db.EvictorManager;
import org.limewire.mojito.db.StorableModelManager;
import org.limewire.mojito.io.MessageDispatcher;
import org.limewire.mojito.io.MessageDispatcherFactory;
import org.limewire.mojito.messages.MessageFactory;
import org.limewire.mojito.result.BootstrapResult;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.result.PingResult;
import org.limewire.mojito.result.StoreResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.RouteTable;
import org.limewire.mojito.routing.Vendor;
import org.limewire.mojito.routing.Version;
import org.limewire.mojito.statistics.DHTStats;
import org.limewire.mojito.util.HostFilter;
import org.limewire.security.MACCalculatorRepositoryManager;
import org.limewire.security.SecurityToken.TokenProvider;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.dht.AbstractDHTController.RandomNodeAdder;

public class AbstractDHTControllerTest extends DHTTestCase {

    private DHTControllerFactory dhtControllerFactory;

    public AbstractDHTControllerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AbstractDHTControllerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        DHTTestUtils.setSettings(PORT);
		Injector injector = LimeTestUtils.createInjector();
		dhtControllerFactory = injector.getInstance(DHTControllerFactory.class);
    }
    
    public void testRandomNodeAdder() throws Exception {
        DHTSettings.PERSIST_ACTIVE_DHT_ROUTETABLE.setValue(false);
        DHTSettings.PERSIST_DHT_DATABASE.setValue(false);
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        PrivilegedAccessor.setValue(DHTSettings.DHT_NODE_ADDER_DELAY, "value", 50L);
        
        DHTController controller = dhtControllerFactory.createActiveDHTNodeController(Vendor.valueOf(1),
                Version.valueOf(1), new DHTManagerStub());
        try {
            controller.start();
            MojitoDHTStub dht = new MojitoDHTStub();
            PrivilegedAccessor.setValue(controller, "dht", dht);
            
            for(int i = 0; i < 20; i++) {
                controller.addActiveDHTNode(new InetSocketAddress("localhost", 2000+i));
            }
            Thread.sleep(500);
            
            //should have started the Random node adder
            RandomNodeAdder nodeAdder = (RandomNodeAdder)PrivilegedAccessor.getValue(controller, "dhtNodeAdder");
            assertNotNull(nodeAdder);
            assertTrue("Node adder should be running", nodeAdder.isRunning());
            //should have pinged all hosts
            assertEquals(20, dht.getPingedNodesList().size());
            assertEquals(new InetSocketAddress("localhost", 2000), dht.getPingedNodesList().get(19));
            
            //try cancelling node adder and starting it again
            controller.stop();
            assertFalse("Node adder should not be running", nodeAdder.isRunning());
            controller.start();
            PrivilegedAccessor.setValue(controller, "dht", dht);
            for(int i = 0; i < 100; i++) {
                controller.addActiveDHTNode(new InetSocketAddress("localhost", 2100+i));
            }
            //see if the nodes were pinged
            Thread.sleep(500);
            assertEquals(50, dht.getPingedNodesList().size());
            assertEquals(new InetSocketAddress("localhost", 2199), dht.getPingedNodesList().get(20));
        } finally {
            controller.stop();
        }
    }
    
    private class MojitoDHTStub implements MojitoDHT {
        
        private List<SocketAddress> pingedList = new ArrayList<SocketAddress>(); 
        
        public boolean isBound() {
            return true;
        }

        public void bind(InetAddress addr, int port) throws IOException {}

        public void bind(int port) throws IOException {}

        public void bind(SocketAddress address) throws IOException {}

        public DHTFuture<FindValueResult> get(KUID key, DHTValueType valueType) {
            return null;
        }

        public SocketAddress getContactAddress() {
            return null;
        }

        public DHTStats getDHTStats() {
            return null;
        }

        public int getExternalPort() {
            return 0;
        }

        public SocketAddress getLocalAddress() {
            return null;
        }

        public Contact getLocalNode() {
            return null;
        }

        public KUID getLocalNodeID() {
            return null;
        }

        public String getName() {
            return null;
        }

        public Vendor getVendor() {
            return Vendor.UNKNOWN;
        }

        public Version getVersion() {
            return Version.ZERO;
        }

        public boolean isBootstrapping() {
            return false;
        }

        public boolean isBootstrapped() {
            return true;
        }

        public boolean isFirewalled() {
            return false;
        }

        public boolean isRunning() {
            return true;
        }

        public Set<KUID> keySet() {
            return null;
        }

        public DHTFuture<PingResult> ping(SocketAddress dst) {
            pingedList.add(dst);
            return null;
        }

        public DHTFuture<StoreResult> remove(KUID key) {
            return null;
        }

        public void setDatabase(Database database) {}

        public Database getDatabase() {
            return null;
        }
        
        public void setExternalPort(int port) {}

        public MessageDispatcher setMessageDispatcher(MessageDispatcherFactory messageDispatcherFactory) {
            return null;
        }

        public void setMessageFactory(MessageFactory messageFactory) {}

        public void setRouteTable(RouteTable routeTable) {}
        
        public RouteTable getRouteTable() {
            return null;
        }
        
        public BigInteger size() {
            return null;
        }

        public void start() {}

        public void stop() {}
        
        public void close() {}
        
        public void setHostFilter(HostFilter hostFilter) {
        }

        public HostFilter getHostFilter() {
            return null;
        }
        
        public List<SocketAddress> getPingedNodesList(){
            return pingedList;
        }

        public DHTExecutorService getDHTExecutorService() {
            return null;
        }

        public void setDHTExecutorService(DHTExecutorService executors) {
        }

        public DHTFuture<BootstrapResult> bootstrap(Contact node) {
            return null;
        }

        public DHTFuture<BootstrapResult> bootstrap(SocketAddress dst) {
            return null;
        }
        
        public DHTFuture<PingResult> findActiveContact() {
            return null;
        }

        public DHTFuture<StoreResult> put(KUID key, DHTValue value) {
            return null;
        }

        public DHTValueFactory getDHTValueFactory() {
            return null;
        }

        public KeyPair getKeyPair() {
            return null;
        }

        public void setDHTValueFactory(DHTValueFactory valueFactory) {
        }

        public void setKeyPair(KeyPair keyPair) {
        }

        public DHTFuture<FindValueResult> get(EntityKey entityKey) {
            return null;
        }

        public DHTValueFactoryManager getDHTValueFactoryManager() {
            return null;
        }

        public StorableModelManager getStorableModelManager() {
            return null;
        }

        public EvictorManager getEvictorManager() {
            return null;
        }

        public void setMACCalculatorRepositoryManager(MACCalculatorRepositoryManager manager) {
            
        }

        public void setSecurityTokenProvider(TokenProvider tokenProvider) {
            
        }
    }
    
    
}
