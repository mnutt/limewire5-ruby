package com.limegroup.gnutella;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.BucketQueue;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.gnutella.tests.LimeTestCase;

import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.util.MessageTestUtils;

/**
 * Tests the <tt>PongCacher</tt> class that maintains a cache of the best most
 * recent pongs seen.
 */
public final class PongCacherImplTest extends LimeTestCase {

    private PongCacherImpl pongCacher;
    private Mockery context;
    private ConnectionServices connectionServices;

    public PongCacherImplTest(String name) {
        super(name);        
    }

    public static Test suite() {
        return buildTestSuite(PongCacherImplTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    public void setUp() throws Exception {
        context = new Mockery();
        connectionServices = context.mock(ConnectionServices.class);
        
        pongCacher = new PongCacherImpl(connectionServices);

        // all test cases expect us to be an ultrapeer
        context.checking(new Expectations() {{
            allowing(connectionServices).isSupernode();
            will(returnValue(true));
        }});
    }
    
    /**
     * Test to make sure that expiring of pongs is working correctly.
     * @throws Exception
     */
    public void testPongExpiring() throws Exception {

        // Create a pong with the correct GGEP for our cacher to accept it.
        PingReply pr = context.mock(PingReply.class);
        MessageTestUtils.mockPongWithFreeLeafSlots(context, pr);
        
        pongCacher.addPong(pr);
        
        // Make sure we get the pong successfully.
        List pongs = pongCacher.getBestPongs(ApplicationSettings.LANGUAGE.get());
        assertEquals("should be 1 pong",1,pongs.size());
        Iterator iter = pongs.iterator();
        PingReply retrievedPong = (PingReply)iter.next();
        assertEquals("unexpected pong", pr, retrievedPong);

        // Make sure we still get the pong successfully on a second pass.
        pongs = pongCacher.getBestPongs(ApplicationSettings.LANGUAGE.get());
        assertEquals("should be 1 pong",1,pongs.size());
        iter = pongs.iterator();
        retrievedPong = (PingReply)iter.next();
        assertEquals("unexpected pong", pr, retrievedPong);

        // Create a second pong with the correct GGEP for our cacher
        // to accept it.
        final PingReply pr2 = context.mock(PingReply.class);
        context.checking(new Expectations() {{
            allowing(pr2).getHops();
            will(returnValue((byte)1));
        }});
        MessageTestUtils.mockPongWithFreeLeafSlots(context, pr2);
        pongCacher.addPong(pr2);
        
        // Make sure we get the 2 pongs successfully in the correct order.
        pongs = pongCacher.getBestPongs(ApplicationSettings.DEFAULT_LOCALE.get());
        assertEquals("should be 2 pongs",2,pongs.size());
        assertContains("no p2", pongs, pr2);
        assertContains("no p", pongs, pr);
        
        // Finally, make sure the pong expires on a sleep -- add a bit to the
        // sleep to avoid thread scheduling craziness.
        Thread.sleep(PongCacher.EXPIRE_TIME+800);
        pongs = pongCacher.getBestPongs(ApplicationSettings.LANGUAGE.get());
        assertEquals("list should be empty", 0, pongs.size());
    }

    private PingReply createPong(final int ttl, final int hops) {
        final PingReply pingReply = context.mock(PingReply.class);
        context.checking(new Expectations() {{
            allowing(pingReply).getTTL();
            will(returnValue((byte)ttl));
            allowing(pingReply).getHops();
            will(returnValue((byte)hops));
        }});
        MessageTestUtils.mockPongWithFreeLeafSlots(context, pingReply);
        return pingReply;
    }
    
    private PingReply createLocalePong(final String locale, final int hops) {
        final PingReply pingReply = context.mock(PingReply.class);
        context.checking(new Expectations() {{
            atLeast(1).of(pingReply).getClientLocale();
            will(returnValue(locale));
            allowing(pingReply).getHops();
            will(returnValue((byte)hops));
        }});
        MessageTestUtils.mockPongWithFreeLeafSlots(context, pingReply);
        return pingReply;
    }
    
    /**
     * Tests the method for getting the best set of pongs.
     */
    public void testGetBestPongs() throws Exception {
        
        List pongs = pongCacher.getBestPongs(ApplicationSettings.LANGUAGE.get());

        PingReply pong = createPong(5, 0);
        pongCacher.addPong(pong);        

        pongs = pongCacher.getBestPongs(ApplicationSettings.LANGUAGE.get());
        assertEquals("unexpected number of cached pongs", 
                     1, pongs.size());        

        pong = createPong(5, 0);
        pongCacher.addPong(pong);        

        pongs = pongCacher.getBestPongs(ApplicationSettings.LANGUAGE.get());
        assertEquals("unexpected number of cached pongs", 
                     1, pongs.size());  

        // fill up the pongs at the default hop
        for(int i=0; i<30; i++) {
            PingReply curPong = createPong(5, 0);
            pongCacher.addPong(curPong);
        }

        pongs = pongCacher.getBestPongs(ApplicationSettings.LANGUAGE.get());

        assertEquals("unexpected number of cached pongs", 
                     PongCacher.NUM_PONGS_PER_HOP, pongs.size());

        PingReply highHopPong = createPong(3, 2);
        
        pongCacher.addPong(highHopPong);

        //Thread.sleep(PongCacher.REFRESH_INTERVAL+200);
        pongs = pongCacher.getBestPongs(ApplicationSettings.LANGUAGE.get());
        assertEquals("unexpected number of cached pongs", 
                     PongCacher.NUM_PONGS_PER_HOP+1, pongs.size());


        Iterator iter = pongs.iterator();
        PingReply pr = (PingReply)iter.next();
        assertEquals("first pong should be high hops", highHopPong, pr); 

        PingReply highHopPong2 = createPong(1, 4);
        pongCacher.addPong(highHopPong2);

        //Thread.sleep(PongCacher.REFRESH_INTERVAL+200);
        pongs = pongCacher.getBestPongs(ApplicationSettings.LANGUAGE.get());
        assertEquals("unexpected number of cached pongs", 
                     PongCacher.NUM_PONGS_PER_HOP+2, pongs.size());   

        iter = pongs.iterator();
        pr = (PingReply)iter.next();
        assertEquals("first pong should be high hops", highHopPong2, pr); 
    }

    /**
     * Tests the method for adding a pong to the cacher.
     */
    public void testAddPong() throws Exception {

        PingReply pong = createPong(5, 0);
        pongCacher.addPong(pong);

        Map<String, BucketQueue<PingReply>> m = pongCacher.getPongMap();
        BucketQueue bq = m.get(pong.getClientLocale());
        assertEquals("unexpected bucket queue size", 1, bq.size());

        pong = createPong(5, 0);
        pongCacher.addPong(pong);
        assertEquals("unexpected bucket queue size", 1, bq.size());
        assertEquals("unexpected bucket queue size", 1, bq.size(0));

        for(int i=bq.size(0); i<PongCacher.NUM_PONGS_PER_HOP+2; i++) {
            pongCacher.addPong(pong);
        }
        
        assertEquals("unexpected bucket queue size", 
                     PongCacher.NUM_PONGS_PER_HOP, bq.size(0));
    }
    

    /**
     * Tests the locale preferencing of PongCacher.
     */
    public void testLocalePong() throws Exception {

        // Create a pong with the correct GGEP for our cacher to accept it.
        PingReply pr = createLocalePong("en", 1);
        pongCacher.addPong(pr);
        
        PingReply prj = createLocalePong("ja", 1);
        pongCacher.addPong(prj);
        
        PingReply prj2 = createLocalePong("ja", 2);
        pongCacher.addPong(prj2);

        // should only return en (en)
        List pongs = pongCacher.getBestPongs("en");
        assertEquals("unexpected size returned from PongCacher when asking for en locale pongs",
                     1, pongs.size());
        assertEquals("pong's locale doesn't match",
                     ((PingReply)pongs.get(0)).getClientLocale(),
                     "en");
        
        // should return "ja" pongs in the beggining (ja, ja, en)
        pongs = pongCacher.getBestPongs("ja");
        assertEquals("unexpected size returned from PongCacher when asking for ja locale pongs",
                     3, pongs.size());
        assertEquals("pong's locale doesn't match",
                     ((PingReply)pongs.get(0)).getClientLocale(),
                     "ja");
        assertEquals("pong's locale doesn't match",
                     ((PingReply)pongs.get(1)).getClientLocale(),
                     "ja");
        assertEquals("pong's locale doesn't match",
                     ((PingReply)pongs.get(2)).getClientLocale(),
                     "en");

        //expire default locale pong but the "ja" locale pongs should be
        //around
        Thread.sleep(PongCacher.EXPIRE_TIME+800);
        pongs = pongCacher.getBestPongs("ja");
        assertEquals("unexpected size returned from PongCacher when asking for ja locale pongs",
                     2, pongs.size());
        assertEquals("pong's locale doesn't match",
                     ((PingReply)pongs.get(0)).getClientLocale(),
                     "ja");
        assertEquals("pong's locale doesn't match",
                     ((PingReply)pongs.get(1)).getClientLocale(),
                     "ja");
    }
}










