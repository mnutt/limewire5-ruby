package com.limegroup.gnutella.downloader;

import java.util.List;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GUID;

import junit.framework.Test;

import com.google.inject.Injector;

public class PushListTest extends LimeTestCase {

    private PushList pushList;
    
    public PushListTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PushListTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        pushList = injector.getInstance(PushList.class);
    }

    public void testGetExactHost() {
        PushDetails details = new PushDetails(new GUID().bytes(), "addresss");
        HTTPConnectObserver observer = new HTTPConnectObserverStub();
        
        pushList.addPushHost(details, observer);
        
        HTTPConnectObserver returnObserver = pushList.getExactHostFor(details);
        
        assertNotNull(returnObserver);
        assertEquals(observer, returnObserver);
        
        returnObserver = pushList.getExactHostFor(details);
        
        // getExactHost should have removed the value, make sure nothing else exists in there.
        assertNull(returnObserver);
    }
    
    public void testGetHost() {
        PushDetails details = new PushDetails(new GUID().bytes(), "addresss");
        HTTPConnectObserver observer = new HTTPConnectObserverStub();
        
        pushList.addPushHost(details, observer);
        
        HTTPConnectObserver returnObserver = pushList.getHostFor(details.getClientGUID(), details.getAddress());
        
        assertNotNull(returnObserver);
        assertEquals(observer, returnObserver);
        
        returnObserver = pushList.getExactHostFor(details);
        
        // getExactHost should have removed the value, make sure nothing else exists in there.
        assertNull(returnObserver);
    }
    
    public void testGetAllAndClear() {
        PushDetails details = new PushDetails(new GUID().bytes(), "addresss");
        HTTPConnectObserver observer = new HTTPConnectObserverStub();
        
        PushDetails details2 = new PushDetails(new GUID().bytes(), "address2");
        HTTPConnectObserver observer2 = new HTTPConnectObserverStub();
        
        pushList.addPushHost(details, observer);
        pushList.addPushHost(details2, observer2);
        
        List<HTTPConnectObserver> list = pushList.getAllAndClear();

        assertEquals(list.size(), 2);
        
        assertTrue(list.contains(observer));
        assertTrue(list.contains(observer2));

        
        HTTPConnectObserver returnObserver = pushList.getExactHostFor(details);
        // getExactHost should have removed the value, make sure nothing else exists in there.
        assertNull(returnObserver);
        
        returnObserver = pushList.getExactHostFor(details2);
        // getExactHost should have removed the value, make sure nothing else exists in there.
        assertNull(returnObserver);
    }
    
    public void testGetExactHostMultipleConnectsToSameHost() {
        GUID guid = new GUID();
        String address = "address";
        
        PushDetails details = new PushDetails(guid.bytes(), address);
        PushDetails details1 = new PushDetails(guid.bytes(), address);
        PushDetails details2 = new PushDetails(guid.bytes(), address);
        HTTPConnectObserver observer = new HTTPConnectObserverStub();
        
        pushList.addPushHost(details, observer);
        pushList.addPushHost(details1, observer);
        pushList.addPushHost(details2, observer);
        
        HTTPConnectObserver returnObserver = pushList.getExactHostFor(details);     
        assertNotNull(returnObserver);
        assertEquals(observer, returnObserver);
        
        returnObserver = pushList.getExactHostFor(details);
        assertNull(returnObserver);
        
        returnObserver = pushList.getExactHostFor(details1);
        assertNotNull(returnObserver);
        assertEquals(observer, returnObserver);
        
        returnObserver = pushList.getExactHostFor(details2);
        assertNotNull(returnObserver);
        assertEquals(observer, returnObserver);
        
        returnObserver = pushList.getExactHostFor(details1);
        assertNull(returnObserver);
        
        returnObserver = pushList.getExactHostFor(details2);
        assertNull(returnObserver);
    }
    
    public void testGetHostMultipleConnectsToSameHost() {
        GUID guid = new GUID();
        String address = "address";
        
        PushDetails details = new PushDetails(guid.bytes(), address);
        PushDetails details1 = new PushDetails(guid.bytes(), address);
        PushDetails details2 = new PushDetails(guid.bytes(), address);
        HTTPConnectObserver observer = new HTTPConnectObserverStub();
        
        pushList.addPushHost(details, observer);
        pushList.addPushHost(details1, observer);
        pushList.addPushHost(details2, observer);
        
        HTTPConnectObserver returnObserver = pushList.getHostFor(details.getClientGUID(), details.getAddress());
        assertNotNull(returnObserver);
        assertEquals(observer, returnObserver);
        
        returnObserver = pushList.getExactHostFor(details);
        assertNull(returnObserver);
        
        returnObserver = pushList.getHostFor(details1.getClientGUID(), details1.getAddress());
        assertNotNull(returnObserver);
        assertEquals(observer, returnObserver);
        
        returnObserver = pushList.getHostFor(details2.getClientGUID(), details2.getAddress());
        assertNotNull(returnObserver);
        assertEquals(observer, returnObserver);
               
        returnObserver = pushList.getExactHostFor(details1);
        assertNull(returnObserver);
        
        returnObserver = pushList.getExactHostFor(details2);
        assertNull(returnObserver);
    }
    

}
