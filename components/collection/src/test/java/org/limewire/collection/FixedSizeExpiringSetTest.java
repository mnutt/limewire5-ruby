package org.limewire.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;



/**
 * Unit tests for <code>FixedsizeForgetfulHashMap</code>.
 */
@SuppressWarnings("unchecked")
public class FixedSizeExpiringSetTest extends BaseTestCase {
	
	Collection empty1,empty2,nullColl;
	FixedSizeExpiringSet set, fastSet;
	Object nullObj;

	final int MAX_SIZE = 8;
    final long EXPIRE_TIME = 10 * 1000; // 10 seconds
	
    public FixedSizeExpiringSetTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(FixedSizeExpiringSetTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() {
    	//    	test all constructors
        empty1 = new FixedSizeExpiringSet();
        empty2 = new FixedSizeExpiringSet(MAX_SIZE);
        set =
            new FixedSizeExpiringSet(MAX_SIZE, EXPIRE_TIME);
        fastSet = new FixedSizeExpiringSet(MAX_SIZE, 50);
    	
    }
    public void testSet() throws Exception {
        
        
        

        
        
        empty1.add(nullObj);
        assertTrue(empty1.isEmpty());
        
        try{
        	empty1.addAll(nullColl);
        	fail("expected NullPointerException");
        }
        catch(NullPointerException e){}
        
        //add another empty set
        empty1.addAll(empty2);
        assertTrue(empty1.isEmpty());
        
        //test a fast-expiring set
        fastSet.add(empty1);
        Thread.sleep(51); // thanks to nanoTime we're accurate.
        assertFalse(fastSet.contains(empty1));

        // initialize a couple of objects
        String[] obj = new String[10];
        for (int i = 0; i < 10; i++) {
            obj[i] = "obj" + i;
        }

        // remember the times I added the objects
        long[] timeExpiring = new long[10];

        // test add, contains, remove
        for (int i = 0; i < 10; i++) {
            timeExpiring[i] = System.currentTimeMillis() + EXPIRE_TIME;
            set.add(obj[i]);
            Thread.sleep(150L);
        }

        // test fixed size 
        //set contains 2-9
        assertEquals(set.size(), MAX_SIZE);
        assertFalse(set.contains(obj[0]));
        assertFalse(set.contains(obj[1]));

        assertTrue(set.contains(obj[2]));
        set.remove(obj[2]);
        assertFalse(set.contains(obj[1]));

        set.add(obj[0]);
        //set contains  3-9, 0
        assertTrue(set.contains(obj[8]));

        assertTrue(set.contains(obj[3]));
        set.remove(obj[3]);
        //set contains 4-9, 0 (7 elements)
        assertFalse(set.contains(obj[3]));

        set.add(obj[1]);
        //set contains  and 4-9, 0-1
        assertTrue(set.contains(obj[1]));

        // test expiring!
        while (set.size() > 0) {
            for (int i = 4; i < 10; i++) {
                long now = System.currentTimeMillis();
                if (set.contains(obj[i])) {
                    assertGreaterThanOrEquals(now, 
                        timeExpiring[i]);
                } else {
                    assertGreaterThan(timeExpiring[i],
                        now+1); // round off to the next millisecond.
                }
            }
            Thread.sleep(100L);
        }
        
        // test addAll()
        Collection col = new ArrayList();
        col.addAll(Arrays.asList(obj));
        
        set.addAll(col);
        assertEquals(set.size(), MAX_SIZE);
        assertFalse(set.contains(obj[8]));
        assertFalse(set.contains(obj[9]));
        assertTrue(set.contains(obj[0]));
        assertTrue(set.contains(obj[1]));
        //set contains 0-7

        col.clear();
        
        // test removeAll()
        col.addAll(Arrays.asList(obj).subList(6, 10));
        
        set.removeAll(col);
        
        for (int i = 0; i < 6; i++)
            assertTrue(set.contains(obj[i]));

        for (int i = 6; i < 10; i++)
            assertFalse(set.contains(obj[i]));
        
        set.clear();
        assertEquals(set.size(), 0);
        
        // test retainAll
        for (int i = 0; i < 10; i++) {
            set.add(obj[i]);
            Thread.sleep(20);  //<--read the note in the implementation
        }
        
        
        for(int i = 2;i<10;i++)
        	assertTrue(set.contains(obj[i]));
        
        //set contains 2-9 col contains 6-9

        set.retainAll(col);
        
        
        for (int i = 0; i < 6; i++)
            assertFalse(set.contains(obj[i]));
        

        for (int i = 6; i < 9; i++)
            assertTrue(set.contains(obj[i]));
        
        assertEquals(set.size(),col.size());
        
        //as well as containsAll
        assertTrue(set.containsAll(col));
        
        //removeall
        set.removeAll(empty1);
        assertEquals(set.size(),col.size());
        set.removeAll(col);
        assertTrue(set.isEmpty());
        
        //test toArray methods
        Object[] array1 = set.toArray();
        assertEquals(array1.length,set.size());
        
        Object[] array2 = set.toArray(array1);
        assertEquals(array1.length,array2.length);
        

    }
}
