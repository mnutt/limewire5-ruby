package org.limewire.listener;

import junit.framework.TestCase;

public class CachingEventMulticasterTest extends TestCase {
    public CachingEventMulticasterTest(String name) {
        super(name);
    }

    public void testAddListener() {
        EventMulticaster<Integer> multicaster = new CachingEventMulticasterImpl<Integer>();
        IntListener intListener = new IntListener();
        multicaster.addListener(intListener);
        assertEquals(0, intListener.value);
        assertEquals(0, intListener.notifications);
        
        multicaster = new CachingEventMulticasterImpl<Integer>();
        intListener = new IntListener();
        multicaster.broadcast(5);
        multicaster.addListener(intListener);
        assertEquals(5, intListener.value);
        assertEquals(1, intListener.notifications);
    }
    
    public void testBroadcastBroadcastPolicyDefault() {
        EventMulticaster<Integer> multicaster = new CachingEventMulticasterImpl<Integer>();
        IntListener intListener = new IntListener();
        multicaster.addListener(intListener);
        multicaster.broadcast(10);
        assertEquals(10, intListener.value);
        assertEquals(1, intListener.notifications);
        multicaster.broadcast(10);
        assertEquals(10, intListener.value);
        assertEquals(2, intListener.notifications);
        multicaster.broadcast(15);
        assertEquals(15, intListener.value);
        assertEquals(3, intListener.notifications);
    }
    
    public void testBroadcastBroadcastPolicyAlways() {
        EventMulticaster<Integer> multicaster = new CachingEventMulticasterImpl<Integer>(BroadcastPolicy.ALWAYS);
        IntListener intListener = new IntListener();
        multicaster.addListener(intListener);
        multicaster.broadcast(10);
        assertEquals(10, intListener.value);
        assertEquals(1, intListener.notifications);
        multicaster.broadcast(10);
        assertEquals(10, intListener.value);
        assertEquals(2, intListener.notifications);
        multicaster.broadcast(15);
        assertEquals(15, intListener.value);
        assertEquals(3, intListener.notifications);
    }
    
    public void testBroadcastBroadcastPolicyIfNotEqual() {
        EventMulticaster<Integer> multicaster = new CachingEventMulticasterImpl<Integer>(BroadcastPolicy.IF_NOT_EQUALS);
        IntListener intListener = new IntListener();
        multicaster.addListener(intListener);
        multicaster.broadcast(10);
        assertEquals(10, intListener.value);
        assertEquals(1, intListener.notifications);
        multicaster.broadcast(10);
        assertEquals(10, intListener.value);
        assertEquals(1, intListener.notifications);
        multicaster.broadcast(15);
        assertEquals(15, intListener.value);
        assertEquals(2, intListener.notifications);
    }
    
    public void testRemoveListener() {
        EventMulticaster<Integer> multicaster = new CachingEventMulticasterImpl<Integer>();
        IntListener intListener = new IntListener();
        multicaster.addListener(intListener);
        assertEquals(0, intListener.value);
        assertEquals(0, intListener.notifications);
        
        multicaster = new CachingEventMulticasterImpl<Integer>();
        intListener = new IntListener();
        multicaster.broadcast(5);
        multicaster.addListener(intListener);
        assertEquals(5, intListener.value);
        assertEquals(1, intListener.notifications);
        
        multicaster.removeListener(intListener);
        multicaster.broadcast(10);
        assertEquals(5, intListener.value);
        assertEquals(1, intListener.notifications);
        
    }
    
    class IntListener implements EventListener<Integer> {
        private int value;
        private int notifications;

        @Override
        public void handleEvent(Integer event) {
            value = event;
            notifications++;
        }
    }
}
