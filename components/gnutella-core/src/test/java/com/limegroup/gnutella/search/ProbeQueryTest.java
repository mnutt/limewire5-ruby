package com.limegroup.gnutella.search;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.Injector;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.util.NewConnection;
import com.limegroup.gnutella.util.TestConnection;
import com.limegroup.gnutella.util.TestConnectionFactory;
import com.limegroup.gnutella.util.TestResultCounter;


/**
 * Tests the functionality of the <tt>QueryHandlerTest</tt> class.
 */
public final class ProbeQueryTest extends LimeTestCase {

    /**
     * Cached method for creating the probe lists.
     */
    private static Method CREATE_PROBE_LISTS;
    private TestConnectionFactory testConnectionFactory;
    private QueryHandlerFactory queryHandlerFactory;
    private QueryRequestFactory queryRequestFactory;

	public ProbeQueryTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ProbeQueryTest.class);
	}

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static void globalSetUp() throws Exception {
        Class[] params = new Class[]{List.class, QueryRequest.class};
		CREATE_PROBE_LISTS = 
            PrivilegedAccessor.getMethod(ProbeQuery.class, 
                                         "createProbeLists",
                                         params);        
    }

    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        testConnectionFactory = injector.getInstance(TestConnectionFactory.class);
        queryHandlerFactory = injector.getInstance(QueryHandlerFactory.class);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
    }

    /**
     * Tests the method for sending a probe query to make sure it is sent to
     * the number of nodes we think it's sent to and to make sure it's sent
     * with the proper TTL.
     */
    public void testSendProbe() throws Exception {
		Method m = 
            PrivilegedAccessor.getMethod(ProbeQuery.class, 
                                         "sendProbe",
                                         new Class[]{});
        List<NewConnection> connections = new LinkedList<NewConnection>();
        for(int i=0; i<15; i++) {
            connections.add(testConnectionFactory.createNewConnection(10));
        }

        QueryHandler handler = 
            queryHandlerFactory.createHandler(queryRequestFactory.createQuery("test"),
                                       testConnectionFactory.createNewConnection(8),
                                       new TestResultCounter(0));
        
        ProbeQuery probe = new ProbeQuery(connections, handler);
        m.invoke(probe, new Object[]{});

        int queriesSent = 0;
        int totalTTL = 0;
        Iterator iter = connections.iterator();
        while(iter.hasNext()) {
            TestConnection tc = (TestConnection)iter.next();
            queriesSent += tc.getNumQueries();
            totalTTL += tc.getTotalTTL();
        }

        assertEquals("should have only sent two queries", 3, queriesSent);
        assertEquals("should have sent 2 queries with TTL=2", 6, totalTTL);
    }

    /**
     * Test to make sure that the utility method for creating
     * our probe query lists is working as we expect it to.
     */
    @SuppressWarnings("unchecked")
    public void testCreateProbeListsForSomewhatPopular() throws Exception {
        List<TestConnection> connections = new LinkedList<TestConnection>();
        connections.clear();
        for(int i=0; i<20; i++) {
            connections.add(testConnectionFactory.createNewHitConnection());
        }           

        for(int i=0; i<5; i++) {
            connections.add(testConnectionFactory.createNewConnection());
        }           
        
        for(int i=0; i<5; i++) {
            connections.add(testConnectionFactory.createOldConnection(5));
        }                   

        QueryRequest query = queryRequestFactory.createQuery("test");
        List<List> queryLists = 
            (List<List>)CREATE_PROBE_LISTS.invoke(null, 
                                              new Object[]{connections, query}); 

        assertTrue("should not be any ttl=2 queries", queryLists.get(1).isEmpty());
        assertTrue("should not be too many ttl=1", queryLists.get(0).size() < 15);
    }

    /**
     * Test to make sure that the utility method for creating
     * our probe query lists is working as we expect it to when
     * the content we're searching for is very popular.
     */
    @SuppressWarnings("unchecked")
    public void testCreateProbeListsForPopularContent() throws Exception {
        List<TestConnection> connections = new LinkedList();
        for(int i=0; i<15; i++) {
            connections.add(testConnectionFactory.createNewHitConnection());
        }           
        
        for(int i=0; i<15; i++) {
            connections.add(testConnectionFactory.createOldConnection(5));
        }                   

        QueryRequest query = queryRequestFactory.createQuery("test");
        List<List> queryLists = 
            (List<List>)CREATE_PROBE_LISTS.invoke(null, 
                                              new Object[]{connections, query}); 

        assertTrue("should not be any ttl=2 queries", queryLists.get(1).isEmpty());
        assertEquals("should not be only 1 ttl=1 query", queryLists.get(0).size(), 1);
    }

    /**
     * Tests the <tt>QueryHandler</tt> utility method that takes
     * two lists and puts the desired number of elements in a
     * third list, prioritizing elements from one list over the
     * other.
     */
    public void testAddToList() throws Exception {
        Class[] paramTypes = 
            new Class[]{List.class, List.class, List.class, Integer.TYPE};
		Method m = 
            PrivilegedAccessor.getMethod(ProbeQuery.class, 
                                         "addToList",
                                         paramTypes);                

        List listToAddTo = new LinkedList();
        List<Integer> list1 = new LinkedList<Integer>();
        List<Integer> list2 = new LinkedList<Integer>();
        Integer numElements = new Integer(3);

        Object[] params = 
            new Object[] {listToAddTo, list1, list2, numElements};
        
        Integer one   = new Integer(1);
        Integer two   = new Integer(2);
        Integer three = new Integer(3);
        Integer four  = new Integer(4);

        List<Integer> testList = new LinkedList<Integer>();
        testList.add(one);
        testList.add(two);
        testList.add(three);        

        list1.add(one);
        list1.add(two);
        list1.add(three);

        m.invoke(null, params);
        assertEquals("lists should be equal", testList, listToAddTo);

        list1.clear();
        list2.clear();
        listToAddTo.clear();

        
        list2.add(one);
        list2.add(two);
        list2.add(three);

        m.invoke(null, params);
        assertEquals("lists should be equal", testList, listToAddTo);

        list1.clear();
        list2.clear();
        listToAddTo.clear();
        
        list1.add(one);
        list2.add(two);
        list2.add(three);

        m.invoke(null, params);
        assertEquals("lists should be equal", testList, listToAddTo);

        list1.clear();
        list2.clear();
        listToAddTo.clear();
        
        list1.add(one);
        list1.add(two);
        list2.add(three);
        list2.add(four);

        m.invoke(null, params);
        assertEquals("lists should be equal", testList, listToAddTo);
    }
}
