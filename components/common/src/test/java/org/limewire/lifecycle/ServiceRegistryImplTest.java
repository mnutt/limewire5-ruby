package org.limewire.lifecycle;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class ServiceRegistryImplTest extends BaseTestCase {
    
    private int count = 0;
    
    public ServiceRegistryImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServiceRegistryImplTest.class);
    }

    public void testServices() {
        ServiceStub a1 = new ServiceStub(1);
        ServiceStub a2 = new ServiceStub(2);
        ServiceStub a3 = new ServiceStub(3);
        ServiceStub a4 = new ServiceStub(4);
        ServiceStub a5 = new ServiceStub(5);
        ServiceStub a6 = new ServiceStub(6);
        ServiceStub a7 = new ServiceStub(7);
        ServiceStub a8 = new ServiceStub(8);
        ServiceStub a9 = new ServiceStub(9);
        ServiceStub a10 = new ServiceStub(10);
        ServiceStub a11 = new ServiceStub(11);
        
        ServiceRegistry registry = new ServiceRegistryImpl();
        registry.register(a8);
        registry.register(a7);
        registry.register(a6).in(ServiceStage.EARLY);
        registry.register(a5).in(ServiceStage.EARLY);
        registry.register(a4).in(ServiceStage.LATE);
        registry.register(a3).in(ServiceStage.LATE);
        registry.register(a2).in(ServiceStage.NORMAL);
        registry.register(a1).in(ServiceStage.NORMAL);
        registry.register(a9).in("SuperEarly");        
        registry.register(a10).in("LittleLater");        
        registry.register(a11).in("LittleLater");
        
        // Start @ 0.
        checkInit(0, 0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
        checkStart(0, 0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
        checkStop(0, 0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
        
        // Initializes everything.
        registry.initialize();
        checkInit(0, 1, a6, a5, a8, a7, a2, a1, a4, a3, a9, a10, a11);
        checkStart(0, 0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
        checkStop(0, 0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
        
        // Starts all things in stages (and unstaged)
        registry.start();
        checkInit(0, 1, a6, a5, a8, a7, a2, a1, a4, a3, a9, a10, a11);
        checkStart(11, 1, a6, a5, a8, a7, a2, a1, a4, a3);
        checkStart(0, 0, a9, a10, a11);
        checkStop(0, 0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
        
        // Starts only SuperEarly, nothing else.
        registry.start("SuperEarly");
        checkInit(0, 1, a6, a5, a8, a7, a2, a1, a4, a3, a9, a10, a11);
        checkStart(11, 1, a6, a5, a8, a7, a2, a1, a4, a3);
        checkStart(19, 0, a9);
        checkStart(0, 0, a10, a11);
        checkStop(0, 0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
        
        // Stop everything that started, but not the LittleLaters.
        registry.stop();
        checkInit(0, 1, a6, a5, a8, a7, a2, a1, a4, a3);
        checkStart(11, 1, a6, a5, a8, a7, a2, a1, a4, a3);
        checkStart(19, 0, a9);
        checkStart(0, 0, a10, a11);
        checkStop(20, 0, a9);
        checkStop(21, 1, a3, a4, a1, a2, a7, a8, a5, a6);
        checkStop(0, 0, a10, a11);
        
        // No change in init
        registry.initialize();
        checkInit(0, 1, a6, a5, a8, a7, a2, a1, a4, a3, a9, a10, a11);
        
        // Start only the LittleLaters
        registry.start("LittleLater");
        checkInit(0, 1, a6, a5, a8, a7, a2, a1, a4, a3);
        checkStart(11, 1, a6, a5, a8, a7, a2, a1, a4, a3);
        checkStart(19, 0, a9);
        checkStart(29, 1, a10, a11);
        checkStop(20, 0, a9);
        checkStop(21, 1, a3, a4, a1, a2, a7, a8, a5, a6);
        checkStop(0, 0, a10, a11);
        
        // Stop only the LittleLater ones that just started.
        registry.stop();
        checkInit(0, 1, a6, a5, a8, a7, a2, a1, a4, a3);
        checkStart(11, 1, a6, a5, a8, a7, a2, a1, a4, a3);
        checkStart(19, 0, a9);
        checkStart(29, 1, a10, a11);
        checkStop(20, 0, a9);
        checkStop(21, 1, a3, a4, a1, a2, a7, a8, a5, a6);
        checkStop(31, 1, a11, a10);
        
        // Expect no change.
        registry.start();
        registry.start("LittleLater");
        registry.start("SuperEarly");
        checkInit(0, 1, a6, a5, a8, a7, a2, a1, a4, a3);
        checkStart(11, 1, a6, a5, a8, a7, a2, a1, a4, a3);
        checkStart(19, 0, a9);
        checkStart(29, 1, a10, a11);
        checkStop(20, 0, a9);
        checkStop(21, 1, a3, a4, a1, a2, a7, a8, a5, a6);
        checkStop(31, 1, a11, a10);
        
    }
    
    public void testAsyncStop() {
        ASynchStopServiceStub a1 = new ASynchStopServiceStub(1, 5000);
        
        ServiceRegistry registry = new ServiceRegistryImpl();        
        registry.register(a1);        
        registry.initialize();        
        registry.start();
        
        long beforeStop = System.currentTimeMillis();
        registry.stop();
        assertGreaterThanOrEquals(5000, System.currentTimeMillis() - beforeStop);  
    }
    
    private void checkInit(int expected, int increment, ServiceStub... services) {
        for(ServiceStub stub : services) {
            assertEquals(stub.toString(), expected, stub.initCount);
            expected += increment;
        }
    }
    
    private void checkStart(int expected, int increment, ServiceStub... services) {
        for(ServiceStub stub : services) {
            assertEquals(stub.toString(), expected, stub.startCount);
            expected += increment;
        }
    }
    
    private void checkStop(int expected, int increment, ServiceStub... services) {
        for(ServiceStub stub : services) {
            assertEquals(stub.toString(), expected, stub.stopCount);
            expected += increment;
        }
    }
    
    private class ServiceStub implements Service {
        private int initCount;
        private int startCount;
        private int stopCount;
        private final int ID;
        
        ServiceStub(int id) {
            this.ID = id;
        }
        
        @Override
        public String toString() {
            return "stub id: " + ID;
        }
        
        public void initialize() {
            initCount = count++;
        }
        
        public void start() {
            startCount = count++;
        }
        
        public void stop() {
            stopCount = count++;
        }
        
        public String getServiceName() {
            // TODO Auto-generated method stub
            return null;
        }
    }
    
    private class ASynchStopServiceStub extends ServiceStub {
        private final long timeToSleep;

        ASynchStopServiceStub(int id, long timeToSleep) {
            super(id);
            this.timeToSleep = timeToSleep;
        }
        
        @Asynchronous 
        public void stop() {
            super.stop();
            try {
                // simulate long running task
                Thread.sleep(timeToSleep);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
}
