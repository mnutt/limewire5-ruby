package org.limewire.collection; 

import java.util.ArrayList;
import java.util.Collection;

import org.limewire.util.BaseTestCase;

import junit.framework.Test;

@SuppressWarnings("unchecked") 
public class MultiCollectionTest extends BaseTestCase { 

    public MultiCollectionTest(String name) { 
        super(name); 
    } 
    
    public static Test suite() { 
        return buildTestSuite(MultiCollectionTest.class); 
    } 
     
    public void testContainsAll() { 

        //make a collectin of [ [1] [] ] 
        Collection<Integer> a = new ArrayList<Integer>(); 
        a.add(1);
        Collection<Integer> b = new ArrayList<Integer>(); 
        Collection mc = new MultiCollection<Integer>(a,b); 
         
        // make a test collection of [1 2] 
        Collection<Integer> test = new ArrayList<Integer>(); 
        test.add(1); 
        test.add(2);
         
        // mc doesn't contain all 1 and 2 
        assertFalse(mc.containsAll(test)); 
    } 
} 
