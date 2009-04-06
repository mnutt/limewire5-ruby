package com.limegroup.gnutella.dht;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import junit.framework.Test;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.settings.DHTSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.mojito.KUID;

import com.google.inject.Injector;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;

public class DHTManagerTest extends DHTTestCase {
    
    
    private Injector injector;
    private DHTControllerFactory dhtControllerFactory;

    public DHTManagerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(DHTManagerTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    protected void setUp() throws Exception {
        DHTSettings.FORCE_DHT_CONNECT.setValue(true);
        
        injector = LimeTestUtils.createInjector();
        dhtControllerFactory = injector.getInstance(DHTControllerFactory.class);
    }

    public void testLimeDHTManager() throws Exception{
        DHTSettings.PERSIST_ACTIVE_DHT_ROUTETABLE.setValue(true);
        DHTSettings.PERSIST_DHT_DATABASE.setValue(true);
        
        TestExecutor executor = new TestExecutor();
        DHTManagerImpl manager = new DHTManagerImpl(executor, dhtControllerFactory);
        
        try {
            assertFalse(manager.isRunning());
            assertFalse(manager.isBootstrapped());
            assertFalse(manager.isWaitingForNodes());
            assertEquals(0, manager.getActiveDHTNodes(10).size());
            
            manager.start(DHTMode.ACTIVE);
            assertEquals(1, executor.getRunners().size());
            Thread.sleep(200);
            assertTrue(manager.isRunning());
            assertEquals(DHTMode.ACTIVE, manager.getDHTMode());
            KUID activeLocalNodeID = manager.getMojitoDHT().getLocalNodeID();
            
            // Rry starting again
            manager.start(DHTMode.ACTIVE);
            Thread.sleep(200);
            assertEquals(activeLocalNodeID, manager.getMojitoDHT().getLocalNodeID());
            
            // Try switching mode
            manager.start(DHTMode.PASSIVE);
            Thread.sleep(200);
            assertEquals(DHTMode.PASSIVE, manager.getDHTMode());
            assertTrue(manager.isRunning());
            KUID passiveLocalNodeID = manager.getMojitoDHT().getLocalNodeID();
            assertNotEquals(activeLocalNodeID, passiveLocalNodeID);
            manager.start(DHTMode.PASSIVE);
            Thread.sleep(200);
            assertEquals(passiveLocalNodeID, manager.getMojitoDHT().getLocalNodeID());
            manager.addressChanged();
            Thread.sleep(200);
            assertEquals(passiveLocalNodeID, manager.getMojitoDHT().getLocalNodeID());
            
            // Try switching multiple times (does some Disk I/O)
            manager.start(DHTMode.ACTIVE);
            manager.start(DHTMode.PASSIVE);
            manager.start(DHTMode.ACTIVE);
            
            // Give it enough time --> previous starts were offloaded to threadpool
            Thread.sleep(10000);
            
            // We should be in active mode
            assertEquals(DHTMode.ACTIVE, manager.getDHTMode());
            
            // The Node ID should be something else than passiveLocalNodeID
            assertNotEquals(passiveLocalNodeID, manager.getMojitoDHT().getLocalNodeID());
            
            // The Node ID should be (but it's not guaranteed) equals to activeLocalNodeID
            assertEquals(activeLocalNodeID, manager.getMojitoDHT().getLocalNodeID());
        } finally {
            manager.stop();
        }
    }
    
    public void testStopStartLimeDHTManager() throws Exception{
        TestExecutor executor = new TestExecutor();
        DHTManagerImpl manager = new DHTManagerImpl(executor, dhtControllerFactory);
        try {
            manager.start(DHTMode.ACTIVE);
            manager.stop();
            Thread.sleep(200);
            assertFalse(manager.isRunning());
            manager.start(DHTMode.ACTIVE);
            Thread.sleep(200);
            assertTrue(manager.isRunning());
            assertEquals(DHTMode.ACTIVE, manager.getDHTMode());
            manager.start(DHTMode.PASSIVE);
            Thread.sleep(200);
            assertEquals(DHTMode.PASSIVE, manager.getDHTMode());
            assertTrue(manager.isRunning());
            manager.start(DHTMode.ACTIVE);
            manager.start(DHTMode.PASSIVE);
            manager.start(DHTMode.ACTIVE);
            manager.stop();
            assertFalse(manager.isRunning());
            Thread.sleep(500);
            assertFalse(manager.isRunning());
        } finally {
            manager.stop();
        }
    }
    
    private class TestExecutor implements Executor {
        private List<Runnable> runners = new ArrayList<Runnable>();
        private ExecutorService service = ExecutorsHelper.newProcessingQueue("DHT-TestExecutor");
        
        public List<Runnable> getRunners() {
            return runners;
        }

        public void execute(Runnable command) {
            runners.add(command);
            service.execute(command);
            
        }
    }
}
