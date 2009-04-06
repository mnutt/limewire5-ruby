package com.limegroup.gnutella.auth;

import junit.framework.Test;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.core.settings.ContentSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
 
public class ContentManagerTest extends LimeTestCase {
    
    private static final String S_URN_1 = "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB";
    private static final String S_URN_2 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKOZWUGZQYPFB";
    private static final String S_URN_3 = "urn:sha1:PABCDEFGAPOJTS5FJUPAKOZWUGZQYPFB";
    
    private static URN URN_1;
    private static URN URN_2;
    private static URN URN_3;
    
    private ContentManager mgr;
    private ContentResponse crOne;
    private ContentResponse crTwo;
    private Observer one;
    private Observer two;
    private Observer three;
    private IpPortContentAuthorityFactory ipPortContentAuthorityFactory;
    
    
    public ContentManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ContentManagerTest.class);
    }
    
    public static void globalSetUp() throws Exception {

    }
    
    @Override
    public void setUp() throws Exception {
		URN_1 = URN.createSHA1Urn(S_URN_1);
		URN_2 = URN.createSHA1Urn(S_URN_2);
		URN_3 = URN.createSHA1Urn(S_URN_3);

        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(true);
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(true);

        crOne = new ContentResponse(URN_1, true);
        crTwo = new ContentResponse(URN_2, false);
        one = new Observer();
        two = new Observer();
        three = new Observer();
		
		Injector injector = LimeTestUtils.createInjector();
		ipPortContentAuthorityFactory = injector.getInstance(IpPortContentAuthorityFactory.class);

		mgr = new ContentManager(ipPortContentAuthorityFactory);
        
        assertNull(mgr.getResponse(URN_1));
        assertNull(mgr.getResponse(URN_2));        
        assertNull(mgr.getResponse(URN_3));
        assertNull(one.urn);
        assertNull(two.urn);
        assertNull(one.response);
        assertNull(two.response);        
    }
    
    public void teardown() throws Exception {
        mgr.stop();
    }
    
    /**
     * Makes sure that content messages add responses.
     * 
     * @throws Exception
     */
    public void testResponseStored() throws Exception {
        mgr.request(URN_1, one, 0);
        mgr.handleContentResponse(crOne);
        assertNotNull(mgr.getResponse(URN_1));
        assertNull(mgr.getResponse(URN_2));

        ContentResponseData res = mgr.getResponse(URN_1);
        assertTrue(res.isOK());

        mgr.request(URN_2, two, 0);
        mgr.handleContentResponse(crTwo);
        assertNotNull(mgr.getResponse(URN_2));
        res = mgr.getResponse(URN_2);
        assertFalse(res.isOK());
    }
    
    /** Makes sure that handleResponse is called rightly. */
    public void testHandleResponseCalled() throws Exception {
        mgr.request(URN_1, one, 0);
        mgr.request(URN_2, two, 0);
        assertNull(one.urn);
        assertNull(two.urn);
        assertNull(one.response);
        assertNull(two.response);

        mgr.handleContentResponse(crOne);
        assertEquals(URN_1, one.urn);
        assertTrue(one.response.isOK());
        assertNull(two.urn);
        assertNull(two.response);

        mgr.handleContentResponse(crTwo);
        assertEquals(URN_2, two.urn);
        assertFalse(two.response.isOK());
    }
    
    /** Tests immediate response. */
    public void testImmediateHandleResponse() throws Exception {
        mgr.request(URN_1, one, 0);
        mgr.request(URN_2, two, 0);
        mgr.handleContentResponse(crOne);
        mgr.handleContentResponse(crTwo);

        one = new Observer();
        two = new Observer();
        mgr.request(URN_1, one, 0);
        mgr.request(URN_2, two, 0);

        assertEquals(URN_1, one.urn);
        assertTrue(one.response.isOK());
        assertEquals(URN_2, two.urn);
        assertFalse(two.response.isOK());
    }
    
    /** Makes sure that stuff times out. */
    public void testTimeout() throws Exception {
        mgr.start(); // must start timeout thread.
        mgr.request(URN_1, one, 1);
        mgr.request(URN_2, two, 1);
        
        Thread.sleep(5000);
        
        assertEquals(URN_1, one.urn);
        assertNull(one.response);
        assertEquals(URN_2, two.urn);
        assertNull(two.response);       
    }
    
    /** Makes sure that responses without requests aren't processed. */
    public void testResponseWithoutRequest() throws Exception {
        mgr.handleContentResponse(crOne);
        assertNull(mgr.getResponse(URN_1));
    }
    
    /** Makes sure that isVerified works */
    public void testIsVerified() throws Exception {
        mgr.start();
        assertFalse(mgr.isVerified(URN_1));
        mgr.request(URN_1, one, 2000);
        assertFalse(mgr.isVerified(URN_1));
        Thread.sleep(5000);
        assertTrue(mgr.isVerified(URN_1)); // verified by timeout (no response).
        
        assertFalse(mgr.isVerified(URN_2));
        mgr.request(URN_2, two, 2000);
        assertFalse(mgr.isVerified(URN_2));
        mgr.handleContentResponse(new ContentResponse(URN_2, true));
        assertTrue(mgr.isVerified(URN_2)); // verified by true response
        
        assertFalse(mgr.isVerified(URN_3));
        mgr.request(URN_3, one, 2000);
        assertFalse(mgr.isVerified(URN_3));        
        mgr.handleContentResponse(new ContentResponse(URN_3, false));
        assertTrue(mgr.isVerified(URN_3)); // verified by false response.
    }
    
    /** Checks blocking requests. */
    public void testBlockingRequest() throws Exception {
        mgr.start();
        
        Thread responder = ThreadExecutor.newManagedThread(new Runnable() {
            public void run() {
                ContentManagerTest.sleep(1000);
                mgr.handleContentResponse(crOne);
                ContentManagerTest.sleep(1000);
                mgr.handleContentResponse(crTwo);
            }
        });
        responder.setDaemon(true);
        responder.start();
        
        long now = System.currentTimeMillis();
        ContentResponseData rOne =  mgr.request(URN_1, 3000);
        assertNotNull(rOne);
        assertTrue(rOne.isOK());
        assertGreaterThan(600, System.currentTimeMillis() - now);
        
        now = System.currentTimeMillis();
        ContentResponseData rTwo = mgr.request(URN_2, 3000);
        assertNotNull(rTwo);
        assertFalse(rTwo.isOK());
        assertGreaterThan(600, System.currentTimeMillis() - now);
        
        now = System.currentTimeMillis();
        ContentResponseData rThree = mgr.request(URN_3, 1000);
        assertNull(rThree);
        assertGreaterThan(900, System.currentTimeMillis() - now);
    }
    
    public void testVaryingTimeouts() throws Exception {
        mgr.start();
        mgr.request(URN_1, one, 6000);
        mgr.request(URN_2, two, 2000);
        mgr.request(URN_3, three, 10000);
        
        assertNull(one.urn);
        assertNull(two.urn);
        assertNull(three.urn);
        
        Thread.sleep(4000);
        assertEquals(URN_2, two.urn);
        assertNull(two.response);
        assertNull(one.urn);
        assertNull(three.urn);
        
        Thread.sleep(4000);
        assertEquals(URN_1, one.urn);
        assertNull(one.response);
        assertNull(three.urn);
        
        Thread.sleep(4000);
        assertEquals(URN_3, three.urn);
        assertNull(three.response);
    }
    
    private static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch(InterruptedException ix) {
            throw new RuntimeException(ix);
        }
    }
    
    private static class Observer implements ContentResponseObserver {
        private URN urn;
        private ContentResponseData response;
        
        public void handleResponse(URN urn, ContentResponseData response) {
            this.urn = urn;
            this.response = response;
        }
    }
    
}
