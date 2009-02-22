package org.limewire.core.impl.monitor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.util.BaseTestCase;

public class CoreIncomingSearchManagerTest extends BaseTestCase {

    public CoreIncomingSearchManagerTest(String name) {
        super(name);
    }

    public void testHandlingQueries() {
        TestIncomingSearchListenerList incomingSearchListenerList = new TestIncomingSearchListenerList();
        CoreIncomingSearchManager coreIncomingSearchManager = new CoreIncomingSearchManager(
                incomingSearchListenerList);
        List<String> incomingSearchList = coreIncomingSearchManager.getIncomingSearchList();
        assertEmpty(incomingSearchList);

        String query1 = "query1";
        incomingSearchListenerList.handleQueryString(query1);
        assertEquals(1, incomingSearchList.size());
        assertContains(incomingSearchList, query1);

        String query2 = "query2";
        incomingSearchListenerList.handleQueryString(query2);
        assertEquals(2, incomingSearchList.size());
        assertContains(incomingSearchList, query1);
        assertContains(incomingSearchList, query2);
    }

    public void testDisablingEnablingCoreSearchManager() {
        TestIncomingSearchListenerList incomingSearchListenerList = new TestIncomingSearchListenerList();
        CoreIncomingSearchManager coreIncomingSearchManager = new CoreIncomingSearchManager(
                incomingSearchListenerList);
        List<String> incomingSearchList = coreIncomingSearchManager.getIncomingSearchList();
        assertEmpty(incomingSearchList);

        coreIncomingSearchManager.setListEnabled(false);
        String query1 = "query1";
        incomingSearchListenerList.handleQueryString(query1);
        assertEmpty(incomingSearchList);

        String query2 = "query2";
        incomingSearchListenerList.handleQueryString(query2);
        assertEmpty(incomingSearchList);
        
        coreIncomingSearchManager.setListEnabled(true);
        
        incomingSearchListenerList.handleQueryString(query1);
        assertEquals(1, incomingSearchList.size());
        assertContains(incomingSearchList, query1);

        incomingSearchListenerList.handleQueryString(query2);
        assertEquals(2, incomingSearchList.size());
        assertContains(incomingSearchList, query1);
        assertContains(incomingSearchList, query2);
    }
    
    public void testSettingListSize() {
        TestIncomingSearchListenerList incomingSearchListenerList = new TestIncomingSearchListenerList();
        CoreIncomingSearchManager coreIncomingSearchManager = new CoreIncomingSearchManager(
                incomingSearchListenerList);
        List<String> incomingSearchList = coreIncomingSearchManager.getIncomingSearchList();
        assertEmpty(incomingSearchList);

        coreIncomingSearchManager.setListSize(1);
        String query1 = "query1";
        incomingSearchListenerList.handleQueryString(query1);
        assertEquals(1, incomingSearchList.size());
        assertContains(incomingSearchList, query1);

        String query2 = "query2";
        incomingSearchListenerList.handleQueryString(query2);
        assertEquals(1, incomingSearchList.size());
        assertContains(incomingSearchList, query2);
        
        String query3 = "query3";
        incomingSearchListenerList.handleQueryString(query3);
        assertEquals(1, incomingSearchList.size());
        assertContains(incomingSearchList, query3);
        
        coreIncomingSearchManager.setListSize(2);
        
        incomingSearchListenerList.handleQueryString(query1);
        assertEquals(2, incomingSearchList.size());
        assertContains(incomingSearchList, query1);
        assertContains(incomingSearchList, query3);

        incomingSearchListenerList.handleQueryString(query2);
        assertEquals(2, incomingSearchList.size());
        assertContains(incomingSearchList, query2);
        assertContains(incomingSearchList, query1);
        
        incomingSearchListenerList.handleQueryString(query3);
        assertEquals(2, incomingSearchList.size());
        assertContains(incomingSearchList, query3);
        assertContains(incomingSearchList, query2);

        
    }

    private class TestIncomingSearchListenerList implements IncomingSearchListenerList,
            IncomingSearchListener {

        private final CopyOnWriteArrayList<IncomingSearchListener> listeners;

        public TestIncomingSearchListenerList() {
            this.listeners = new CopyOnWriteArrayList<IncomingSearchListener>();
        }

        @Override
        public void addIncomingSearchListener(IncomingSearchListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeIncomingSearchListener(IncomingSearchListener listener) {
            listeners.remove(listener);
        }

        @Override
        public void handleQueryString(String query) {
            for (IncomingSearchListener listener : listeners) {
                listener.handleQueryString(query);
            }
        }

    }
}
