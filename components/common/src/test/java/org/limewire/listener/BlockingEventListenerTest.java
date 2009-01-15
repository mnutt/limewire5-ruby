package org.limewire.listener;

import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class BlockingEventListenerTest extends BaseTestCase {

    public BlockingEventListenerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(BlockingEventListenerTest.class);
    }
    
    /**
     * Ensures the last handled event is kept around.
     */
    public void testLastEventIsReturned() {
        BlockingEventListener<Object> listener = new BlockingEventListener<Object>();
        listener.handleEvent(new Object());
        Object object = new Object();
        listener.handleEvent(object);
        assertSame(object, listener.getEvent(0, TimeUnit.SECONDS));
    }

}
