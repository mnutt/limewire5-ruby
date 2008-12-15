package org.limewire.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

@SuppressWarnings("unchecked")
public class RRProcessingQueueTest extends BaseTestCase {
    public RRProcessingQueueTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RRProcessingQueueTest.class);
    } 
    
    public void testRRProcessing() throws Exception {
    	RRProcessingQueue queue = new RRProcessingQueue("test");
    	List order = Collections.synchronizedList(new ArrayList());
    	Runner a1 = new Runner(order);
    	Runner a2 = new Runner(order);
    	Runner b1 = new Runner(order);
    	Runner b2 = new Runner(order);
    	Runner c1 = new Runner(order);
    	
    	queue.execute(a1, "a");
    	queue.execute(a2, "a");
    	queue.execute(b1, "b");
    	queue.execute(b2, "b");
    	queue.execute(c1, "c");
    	assertEquals(5, queue.size());
    	
    	try {
    		Thread.sleep(1000);
    	} catch (InterruptedException ignored) {}
    	
    	// order of execution should be:
    	// a1, b1, c1, a2, b2
    	assertEquals(a1,order.get(0));
    	assertEquals(b1,order.get(1));
    	assertEquals(c1,order.get(2));
    	assertEquals(a2,order.get(3));
    	assertEquals(b2,order.get(4));
    	assertEquals(0, queue.size());
    }
    
    private class Runner implements Runnable {
    	final List toNotify;
    	Runner(List toNotify) {
    		this.toNotify = toNotify;
    	}
    	public void run() {
    		try {
    		Thread.sleep(150);
    		} catch (InterruptedException iex){
    			fail(iex);
    		}
    		toNotify.add(this);
    	}
    }
}
