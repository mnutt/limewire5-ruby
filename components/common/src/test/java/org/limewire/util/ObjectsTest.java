package org.limewire.util;

import java.util.ArrayList;
import java.util.Collections;

import junit.framework.Assert;

public class ObjectsTest extends BaseTestCase {

    public ObjectsTest(String name) {
        super(name);
    }

    public void testCompareTo() {
        Assert.assertEquals(0, Objects.compareToNull(null, null));
        Assert.assertEquals(-1, Objects.compareToNull(null, ""));
        Assert.assertEquals(1, Objects.compareToNull("", null));

        String o1 = "test";
        String o2 = o1;

        Assert.assertEquals(0, Objects.compareToNull(o1, o2));
        
        o2 = "test";
        Assert.assertEquals(0, Objects.compareToNull(o1, o2));

        o2 = "test1";
        Assert.assertEquals(-1, Objects.compareToNull(o1, o2));

        o1 = "test1";
        o2 = "test";
        Assert.assertEquals(1, Objects.compareToNull(o1, o2));
        
        ArrayList<String> list = new ArrayList<String>();
        list.add(null);
        list.add("3");
        list.add("1");
        list.add(null);
        list.add("2");
        
        Collections.sort(list, Objects.getComparator(true));
        
        Assert.assertEquals(null, list.get(0));
        Assert.assertEquals(null, list.get(1));
        Assert.assertEquals("1", list.get(2));
        Assert.assertEquals("2", list.get(3));
        Assert.assertEquals("3", list.get(4));
        
        Collections.sort(list, Objects.getComparator(false));
        
        Assert.assertEquals("1", list.get(0));
        Assert.assertEquals("2", list.get(1));
        Assert.assertEquals("3", list.get(2));
        Assert.assertEquals(null, list.get(3));
        Assert.assertEquals(null, list.get(4));
        
    }
}
