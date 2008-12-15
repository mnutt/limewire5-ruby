
package org.limewire.collection;

import java.util.NoSuchElementException;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;


/**
 * a test for the round robin queues.
 */
@SuppressWarnings("unchecked")
public class RoundRobinQueueTest extends BaseTestCase {

	static Integer [] objects = new Integer[20];
	static RoundRobinQueue queue;
	static RoundRobinSetQueue setQueue;
	
	public RoundRobinQueueTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return buildTestSuite(RoundRobinQueueTest.class);
	}
	
	public static void globalSetUp() throws Exception {
		for (int i = 0;i<objects.length;i++)
			objects[i]= i;
	}
	
	@Override
    public void setUp() {
		queue = new RoundRobinQueue();
		setQueue = new RoundRobinSetQueue();
	}
	
	public void testAddAtOnce() throws Exception {
		
		
		//add all objects to the queue
        for (Integer object : objects) {
            queue.enqueue(object);
        }
		
		//iterate once
        for (Integer object : objects) {
            assertEquals(object, queue.next());
        }
		
		//get few more objects, we should start from the beginning now
		assertEquals(objects[0],queue.next());
		assertEquals(objects[1],queue.next());
		assertEquals(objects[2],queue.next());
	}
	
	public void testAddFewAtTime() throws Exception {
		
		
		//add 5 objects
		for (int i=0;i<5;i++)
			queue.enqueue(objects[i]);
		
		//queue should now be 0 1 2 3 4
		//rotate twice
		queue.next();queue.next();
		
		//queue should now be 2 3 4 0 1
		
		//add few more objects
		for (int i=5;i<10;i++)
			queue.enqueue(objects[i]);
		
		//queue should now be 2 3 4 0 1 5 6 7 8 9
		assertEquals(objects[2],queue.next());
		assertEquals(objects[3],queue.next());
		assertEquals(objects[4],queue.next());
		assertEquals(objects[0],queue.next());
		assertEquals(objects[1],queue.next());
		assertEquals(objects[5],queue.next());
		assertEquals(objects[6],queue.next());
		assertEquals(objects[7],queue.next());
		assertEquals(objects[8],queue.next());
		assertEquals(objects[9],queue.next());
	}
	
	public void testRemove() throws Exception {
		
		
		//add 5 objects
		for (int i=0;i<4;i++)
			queue.enqueue(objects[i]);
		queue.enqueue(objects[3]);
		
		//queue should now be 0 1 2 3 3
		//rotate twice
		queue.next();queue.next();
		
		//queue should now be 2 3 3 0 1
		
		//remove the first occurence of 3
		queue.remove(objects[3]);
		
		//queue should now be 2 3 0 1
		assertEquals(objects[2],queue.next());
		assertEquals(objects[3],queue.next());
		assertEquals(objects[0],queue.next());
		assertEquals(objects[1],queue.next());
	}
	
	public void testRemoveAll() throws Exception {
		testRemove();
		queue.enqueue(objects[3]);
		queue.enqueue(objects[4]);
		
		assertEquals(6,queue.size());
		//queue should now be 2 3 0 1 3 4
		
		//rotate once
		queue.next();
		
		//queue should now be 3 0 1 3 4 2
		
		//remove all occurences of 3
		queue.removeAllOccurences(objects[3]);
		
		//queue should now be 0 1 4 2
		assertEquals(objects[0],queue.next());
		assertEquals(objects[1],queue.next());
		assertEquals(objects[4],queue.next());
		assertEquals(objects[2],queue.next());
		
		assertEquals(4,queue.size());
	}
	
	public void testSetQueue() throws Exception {
		setQueue.enqueue(objects[0]);
		setQueue.enqueue(objects[1]);
		setQueue.enqueue(objects[2]);
		setQueue.enqueue(objects[1]);
		setQueue.enqueue(objects[3]);
		setQueue.enqueue(objects[0]);
		
		assertEquals(4,setQueue.size());
		//queue should be 0 1 2 3
		assertEquals(objects[0],setQueue.next());
		assertEquals(objects[1],setQueue.next());
		assertEquals(objects[2],setQueue.next());
		assertEquals(objects[3],setQueue.next());
		
		
		//move one position
		//queue should be 1 2 3 0
		setQueue.next();
		
		//remove element 3
		//the queue now should be 1 2 0
		setQueue.remove(objects[3]);
		
		assertEquals(objects[1],setQueue.next());
		assertEquals(objects[2],setQueue.next());
		assertEquals(objects[0],setQueue.next());
		assertEquals(objects[1],setQueue.next());
		assertEquals(3,setQueue.size());
		
	}
	
	public void testEmpty() throws Exception {
		try {
			queue.next();
			fail("dequeued succesfully from an empty list?");
		}catch (NoSuchElementException expected){}
		try {
			setQueue.next();
			fail("dequeued succesfully from an empty list?");
		}catch (NoSuchElementException expected){}
		
	}
}
