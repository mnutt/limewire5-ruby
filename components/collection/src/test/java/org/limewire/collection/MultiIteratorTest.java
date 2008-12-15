
package org.limewire.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.limewire.util.BaseTestCase;

import junit.framework.Test;



@SuppressWarnings("unchecked")
public class MultiIteratorTest extends BaseTestCase {
	
	public MultiIteratorTest(String name) {
		super(name);
	}
	   
    public static Test suite() {
        return buildTestSuite(MultiIteratorTest.class);
    }  
    
	public void testSeqTraversal() throws Exception {
		List l1 = new ArrayList();
    	for (int i =0;i < 10; i++)
    		l1.add(i);
    	
    	List l2 = new ArrayList();
    	for (int i = 10;i < 20; i++)
    		l2.add(i);
    	
    	List l3 = new ArrayList();
    	for (int i = 20;i < 30; i++)
    		l3.add(i);
    	
    	List control = new ArrayList();
    	control.addAll(l1);
    	control.addAll(l2);
    	control.addAll(l3);
    	
    	List test = new ArrayList();
    	for(Iterator iter = new MultiIterator(
                l1.iterator(),
                Collections.EMPTY_LIST.iterator(),
                l2.iterator(),
                Collections.EMPTY_LIST.iterator(),
                l3.iterator(),
                Collections.EMPTY_LIST.iterator());
    		iter.hasNext();)
    		test.add(iter.next());
    	
    	assertEquals(control.size(),test.size());
    	for(int i = 0; i < control.size();i++)
    		assertEquals(control.get(i),test.get(i));
    	
	}
	
    public void testRRTraversal() throws Exception {
    	List l1 = new ArrayList();
    	for (int i =0;i < 10; i++)
    		l1.add(i);
    	
    	List l2 = new ArrayList();
    	for (int i = 10;i < 20; i++)
    		l2.add(i);
    	
    	List l3 = new ArrayList();
    	for (int i = 20;i < 30; i++)
    		l3.add(i);
    	
    	List control = new ArrayList();
    	for(int i = 0; i < 10; i++) {
    		control.add(l1.get(i));
    		control.add(l2.get(i));
    		control.add(l3.get(i));
    	}
    	
    	List test = new ArrayList();
    	for(Iterator iter = new MultiRRIterator(
    			new Iterator[]{l1.iterator(),
    					l2.iterator(),
    					l3.iterator(),
    					Collections.EMPTY_LIST.iterator()});
    		iter.hasNext();)
    		test.add(iter.next());
    	
    	assertEquals(control.size(),test.size());
    	for(int i = 0; i < control.size();i++)
    		assertEquals(control.get(i),test.get(i));
    }
    
    public void testRRRemoval() throws Exception {
    	List l1 = new ArrayList();
    	for (int i =0;i < 10; i++)
    		l1.add(i);
    	
    	List l2 = new ArrayList();
    	for (int i = 10;i < 20; i++)
    		l2.add(i);
    	
    	List l3 = new ArrayList();
    	for (int i = 20;i < 30; i++)
    		l3.add(i);
    	
    	int j = 6;
    	for(Iterator iter = new MultiRRIterator(
    			new Iterator[]{l1.iterator(),
    					l2.iterator(),
    					l3.iterator(),
    					Collections.EMPTY_LIST.iterator()});
    		iter.hasNext() && j > 0;) {
    		iter.next();
    		iter.remove();
    		j--;
    	}
    	
    	assertEquals(8,l1.size());
    	for (int i = 2; i < 10; i++)
    		assertTrue(l1.contains(new Integer(i)));
    	
    	assertEquals(8,l2.size());
    	for (int i = 12; i < 20; i++)
    		assertTrue(l2.contains(new Integer(i)));
    	
    	assertEquals(8,l3.size());
    	for (int i = 22; i < 30; i++)
    		assertTrue(l3.contains(new Integer(i)));
    }
    
    public void testDefaultConstructor() {
        MultiIterator<Object> iter = new MultiIterator<Object>();
        assertFalse(iter.hasNext());
    }
}
