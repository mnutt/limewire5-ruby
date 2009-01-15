package com.limegroup.gnutella.downloader;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import junit.framework.Test;

import org.limewire.io.ConnectableImpl;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Tests that the legacy ranker is properly selecting which 
 * RFDs it should return.
 */
@SuppressWarnings( { "unchecked" } )
public class LegacyRankerTest extends LimeTestCase {

    private ConcurrentMap<RemoteFileDesc, RemoteFileDescContext> contexts = new ConcurrentHashMap<RemoteFileDesc, RemoteFileDescContext>(); 
    
    public LegacyRankerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(LegacyRankerTest.class);
    }
    
    private static SourceRanker ranker;
    private RemoteFileDescFactory remoteFileDescFactory;
    
    @Override
    public void setUp() throws Exception {
        ranker = new LegacyRanker();
        
        Injector injector = LimeTestUtils.createInjector();
        remoteFileDescFactory = injector.getInstance(RemoteFileDescFactory.class);
        
    }
    
    /**
     * Tests that rfds that have urns are prefered to rfds without.
     */
    public void testPrefersRFDWithURN() throws Exception {
        ranker.addToPool(newRFD("1.2.3.4",3));
        ranker.addToPool(newRFDWithURN("1.2.3.4",3));
        
        RemoteFileDescContext selected = ranker.getBest();
        assertNotNull(selected.getSHA1Urn());
    }
    
    /**
     * tests that the ranker exhausts the list of rfds to try
     */
    public void testExhaustsRFDs() throws Exception {
        ranker.addToPool(newRFD("1.2.3.4",3));
        ranker.addToPool(newRFDWithURN("1.2.3.4",3));
        
        assertTrue(ranker.hasMore());
        ranker.getBest();
        ranker.getBest();
        assertFalse(ranker.hasMore());
    }
    
    /**
     * tests that the ranker does not allow duplicate rfds
     */
    public void testDisallowsDuplicates() throws Exception {
        RemoteFileDescContext rfd1, rfd2;
        rfd1 = newRFDWithURN("1.2.3.4",3);
        rfd2 = newRFDWithURN("1.2.3.4",3);
        assertTrue(rfd1.equals(rfd2));
        assertEquals(rfd1.hashCode(), rfd2.hashCode());
        assertSame(rfd1, rfd2);
        ranker.addToPool(rfd1);
        ranker.addToPool(rfd2);
        
        assertTrue(ranker.hasMore());
        ranker.getBest();
        assertFalse(ranker.hasMore());
    }
    
    public void testGetShareable() throws Exception {
        RemoteFileDescContext rfd1, rfd2;
        rfd1 = newRFD("1.2.3.4",3);
        rfd2 = newRFDWithURN("1.2.3.4",3);
        ranker.addToPool(rfd1);
        ranker.addToPool(rfd2);
        
        Collection c = ranker.getShareableHosts();
        assertTrue(c.contains(rfd1));
        assertTrue(c.contains(rfd2));
        assertEquals(2,c.size());
    }
    
    // TODO: add more tests, although this ranker will be used rarely. 
    
    private RemoteFileDescContext newRFD(String host, int speed) throws Exception {
        return toContext(remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl(host, 1, false), 0, "asdf", TestFile.length(), new byte[16],
                speed, false, 4, false, null, URN.NO_URN_SET, false, "", -1));
    }

    private RemoteFileDescContext newRFDWithURN(String host, int speed) throws Exception {
        Set set = new HashSet();
        try {
            // for convenience, don't require that they pass the urn.
            // assume a null one is the TestFile's hash.
            set.add(TestFile.hash());
        } catch(Exception e) {
            fail("SHA1 not created");
        }
        return toContext(remoteFileDescFactory.createRemoteFileDesc(new ConnectableImpl(host, 1, false), 0, "asdf", TestFile.length(), new byte[16],
                speed, false, 4, false, null, set, false, "", -1));
    }
    
    private RemoteFileDescContext toContext(RemoteFileDesc rfd) {
        RemoteFileDescContext newContext = new RemoteFileDescContext(rfd);
        RemoteFileDescContext oldContext = contexts.putIfAbsent(rfd, newContext);
        return oldContext != null ? oldContext : newContext;
    }

}
