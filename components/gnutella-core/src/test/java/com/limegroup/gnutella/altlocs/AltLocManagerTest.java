package com.limegroup.gnutella.altlocs;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GUID;

import junit.framework.Test;

import com.google.inject.Injector;
import com.limegroup.gnutella.helpers.UrnHelper;

public class AltLocManagerTest extends LimeTestCase {

    public AltLocManagerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(AltLocManagerTest.class);
    }
    
    private AltLocManager manager;
    private AlternateLocationFactory factory; 

    @Override
    public void setUp() {
        
        Injector injector = LimeTestUtils.createInjector();
        
        manager = injector.getInstance(AltLocManager.class);
        factory = injector.getInstance(AlternateLocationFactory.class);
    }
    
    public void testStorage() throws Exception {
        AlternateLocation direct = factory.create("1.2.3.4:5",UrnHelper.SHA1);
        GUID g = new GUID(GUID.makeGuid());
        GUID g2 = new GUID(GUID.makeGuid());
        AlternateLocation push = factory.create(g.toHexString()+";1.1.1.1:1",UrnHelper.SHA1);
        AlternateLocation pushFWT = factory.create(g2.toHexString()+";fwt/1.0;2:2.2.2.2;3.3.3.3:3",UrnHelper.SHA1);
        
        manager.add(direct, null);
        manager.add(push, null);
        manager.add(pushFWT, null);
        
        AlternateLocationCollection c = manager.getDirect(UrnHelper.SHA1);
        assertEquals(1,c.getAltLocsSize());
        assertTrue(c.contains(direct));
        
        c = manager.getPushNoFWT(UrnHelper.SHA1);
        assertEquals(1,c.getAltLocsSize());
        assertTrue(c.contains(push));
        
        
        c = manager.getPushFWT(UrnHelper.SHA1);
        assertEquals(1,c.getAltLocsSize());
        assertTrue(c.contains(pushFWT));
        
        manager.purge();
        assertEquals(AlternateLocationCollection.EMPTY,manager.getDirect(UrnHelper.SHA1));
        assertEquals(AlternateLocationCollection.EMPTY,manager.getPushNoFWT(UrnHelper.SHA1));
        assertEquals(AlternateLocationCollection.EMPTY,manager.getPushFWT(UrnHelper.SHA1));
    }
    
    
    public void testPromotionDemotion() throws Exception {
        AlternateLocation direct = factory.create("1.2.3.4:5",UrnHelper.SHA1);
        manager.add(direct, null);
        manager.remove(direct, null);
        AlternateLocationCollection c = manager.getDirect(UrnHelper.SHA1);
        assertTrue(c.contains(direct));
        assertTrue(direct.isDemoted());
        
        manager.add(direct, null);
        assertFalse(direct.isDemoted());
        
        manager.remove(direct, null);
        manager.remove(direct, null);
        assertEquals(AlternateLocationCollection.EMPTY,manager.getDirect(UrnHelper.SHA1));
    }
    
    public void testNotification() throws Exception {
        // test that a registered listener receives notification of an altloc
        Listener l = new Listener();
        AlternateLocation direct = factory.create("1.2.3.4:5",UrnHelper.SHA1);
        manager.addListener(UrnHelper.SHA1,l);
        manager.add(direct, null);
        assertEquals(direct,l.loc);
        
        // test that a listener does not receive notification that comes from itself
        l.loc = null;
        manager.remove(direct, null);manager.remove(direct, null);
        assertFalse(manager.hasAltlocs(UrnHelper.SHA1));
        manager.add(direct,l);
        assertNull(l.loc);
    }
    
    private static class Listener implements AltLocListener {
        public AlternateLocation loc;
        public void locationAdded(AlternateLocation loc) {
            this.loc = loc;
        }
    }
}
