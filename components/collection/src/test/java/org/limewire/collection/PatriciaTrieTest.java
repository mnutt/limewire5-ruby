package org.limewire.collection;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Map.Entry;

import junit.framework.Test;

import org.limewire.collection.PatriciaTrie.KeyAnalyzer;
import org.limewire.collection.Trie.Cursor;
import org.limewire.util.BaseTestCase;
import org.limewire.util.TestUtils;


public class PatriciaTrieTest extends BaseTestCase {

    public PatriciaTrieTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PatriciaTrieTest.class);
    }

    public void testSimple() {
        PatriciaTrie<Integer, String> intTrie = new PatriciaTrie<Integer, String>(new IntegerKeyCreator());
        assertTrue(intTrie.isEmpty());
        assertEquals(0, intTrie.size());
        
        intTrie.put(1, "One");
        assertFalse(intTrie.isEmpty());
        assertEquals(1, intTrie.size());
        
        assertEquals("One", intTrie.remove(1));
        assertNull(intTrie.remove(1));
        assertTrue(intTrie.isEmpty());
        assertEquals(0, intTrie.size());
        
        intTrie.put(1, "One");
        assertEquals("One", intTrie.get(1));
        assertEquals("One", intTrie.put(1, "NotOne"));
        assertEquals(1, intTrie.size());
        assertEquals("NotOne", intTrie.get(1));
        assertEquals("NotOne", intTrie.remove(1));
        assertNull(intTrie.put(1, "One"));
    }
    
    public void testCeilingEntry() {
        PatriciaTrie<Character, String> charTrie = new PatriciaTrie<Character, String>(new AlphaKeyCreator());
        charTrie.put('c', "c");
        charTrie.put('p', "p");
        charTrie.put('l', "l");
        charTrie.put('t', "t");
        charTrie.put('k', "k");
        charTrie.put('a', "a");
        charTrie.put('y', "y");
        charTrie.put('r', "r");
        charTrie.put('u', "u");
        charTrie.put('o', "o");
        charTrie.put('w', "w");
        charTrie.put('i', "i");
        charTrie.put('e', "e");
        charTrie.put('x', "x");
        charTrie.put('q', "q");
        charTrie.put('b', "b");
        charTrie.put('j', "j");
        charTrie.put('s', "s");
        charTrie.put('n', "n");
        charTrie.put('v', "v");
        charTrie.put('g', "g");
        charTrie.put('h', "h");
        charTrie.put('m', "m");
        charTrie.put('z', "z");
        charTrie.put('f', "f");
        charTrie.put('d', "d");
        
        Object[] results = new Object[] {
            'a', "a", 'b', "b", 'c', "c", 'd', "d", 'e', "e",
            'f', "f", 'g', "g", 'h', "h", 'i', "i", 'j', "j",
            'k', "k", 'l', "l", 'm', "m", 'n', "n", 'o', "o",
            'p', "p", 'q', "q", 'r', "r", 's', "s", 't', "t",
            'u', "u", 'v', "v", 'w', "w", 'x', "x", 'y', "y", 
            'z', "z"
        };
        
        for(int i = 0; i < results.length; i++) {
            Map.Entry<Character, String> found = charTrie.ceilingEntry((Character)results[i]);
            assertNotNull(found);
            assertEquals(results[i], found.getKey());
            assertEquals(results[++i], found.getValue());
        }
        
        // Remove some & try again...
        charTrie.remove('a');
        charTrie.remove('z');
        charTrie.remove('q');
        charTrie.remove('l');
        charTrie.remove('p');
        charTrie.remove('m');
        charTrie.remove('u');
        
        Map.Entry<Character, String> found = charTrie.ceilingEntry('u');
        assertNotNull(found);
        assertEquals((Character)'v', found.getKey());
        
        found = charTrie.ceilingEntry('a');
        assertNotNull(found);
        assertEquals((Character)'b', found.getKey());
        
        found = charTrie.ceilingEntry('z');
        assertNull(found);
        
        found = charTrie.ceilingEntry('q');
        assertNotNull(found);
        assertEquals((Character)'r', found.getKey());
        
        found = charTrie.ceilingEntry('l');
        assertNotNull(found);
        assertEquals((Character)'n', found.getKey());
        
        found = charTrie.ceilingEntry('p');
        assertNotNull(found);
        assertEquals((Character)'r', found.getKey());
        
        found = charTrie.ceilingEntry('m');
        assertNotNull(found);
        assertEquals((Character)'n', found.getKey());
        
        found = charTrie.ceilingEntry('\0');
        assertNotNull(found);
        assertEquals((Character)'b', found.getKey());
        
        charTrie.put('\0', "");
        found = charTrie.ceilingEntry('\0');
        assertNotNull(found);
        assertEquals((Character)'\0', found.getKey());      
    }
    
    public void testLowerEntry() {
        PatriciaTrie<Character, String> charTrie = new PatriciaTrie<Character, String>(new AlphaKeyCreator());
        charTrie.put('c', "c");
        charTrie.put('p', "p");
        charTrie.put('l', "l");
        charTrie.put('t', "t");
        charTrie.put('k', "k");
        charTrie.put('a', "a");
        charTrie.put('y', "y");
        charTrie.put('r', "r");
        charTrie.put('u', "u");
        charTrie.put('o', "o");
        charTrie.put('w', "w");
        charTrie.put('i', "i");
        charTrie.put('e', "e");
        charTrie.put('x', "x");
        charTrie.put('q', "q");
        charTrie.put('b', "b");
        charTrie.put('j', "j");
        charTrie.put('s', "s");
        charTrie.put('n', "n");
        charTrie.put('v', "v");
        charTrie.put('g', "g");
        charTrie.put('h', "h");
        charTrie.put('m', "m");
        charTrie.put('z', "z");
        charTrie.put('f', "f");
        charTrie.put('d', "d");
        
        Object[] results = new Object[] {
            'a', "a", 'b', "b", 'c', "c", 'd', "d", 'e', "e",
            'f', "f", 'g', "g", 'h', "h", 'i', "i", 'j', "j",
            'k', "k", 'l', "l", 'm', "m", 'n', "n", 'o', "o",
            'p', "p", 'q', "q", 'r', "r", 's', "s", 't', "t",
            'u', "u", 'v', "v", 'w', "w", 'x', "x", 'y', "y", 
            'z', "z"
        };
        
        for(int i = 0; i < results.length; i+=2) {
            //System.out.println("Looking for: " + results[i]);
            Map.Entry<Character, String> found = charTrie.lowerEntry((Character)results[i]);
            if(i == 0) {
                assertNull(found);
            } else {
                assertNotNull(found);
                assertEquals(results[i-2], found.getKey());
                assertEquals(results[i-1], found.getValue());
            }
        }

        Map.Entry<Character, String> found = charTrie.lowerEntry((char)('z' + 1));
        assertNotNull(found);
        assertEquals((Character)'z', found.getKey());
        
        
        // Remove some & try again...
        charTrie.remove('a');
        charTrie.remove('z');
        charTrie.remove('q');
        charTrie.remove('l');
        charTrie.remove('p');
        charTrie.remove('m');
        charTrie.remove('u');
        
        found = charTrie.lowerEntry('u');
        assertNotNull(found);
        assertEquals((Character)'t', found.getKey());
        
        found = charTrie.lowerEntry('v');
        assertNotNull(found);
        assertEquals((Character)'t', found.getKey());
        
        found = charTrie.lowerEntry('a');
        assertNull(found);
        
        found = charTrie.lowerEntry('z');
        assertNotNull(found);
        assertEquals((Character)'y', found.getKey());
        
        found = charTrie.lowerEntry((char)('z'+1));
        assertNotNull(found);
        assertEquals((Character)'y', found.getKey());
        
        found = charTrie.lowerEntry('q');
        assertNotNull(found);
        assertEquals((Character)'o', found.getKey());
        
        found = charTrie.lowerEntry('r');
        assertNotNull(found);
        assertEquals((Character)'o', found.getKey());
        
        found = charTrie.lowerEntry('p');
        assertNotNull(found);
        assertEquals((Character)'o', found.getKey());
        
        found = charTrie.lowerEntry('l');
        assertNotNull(found);
        assertEquals((Character)'k', found.getKey());
        
        found = charTrie.lowerEntry('m');
        assertNotNull(found);
        assertEquals((Character)'k', found.getKey());
        
        found = charTrie.lowerEntry('\0');
        assertNull(found);
        
        charTrie.put('\0', "");
        found = charTrie.lowerEntry('\0');
        assertNull(found);      
    }
    
    public void testIteration() {
        PatriciaTrie<Integer, String> intTrie = new PatriciaTrie<Integer, String>(new IntegerKeyCreator());
        intTrie.put(1, "One");
        intTrie.put(5, "Five");
        intTrie.put(4, "Four");
        intTrie.put(2, "Two");
        intTrie.put(3, "Three");
        intTrie.put(15, "Fifteen");
        intTrie.put(13, "Thirteen");
        intTrie.put(14, "Fourteen");
        intTrie.put(16, "Sixteen");
        
        TestCursor cursor = new TestCursor(
                1, "One", 2, "Two", 3, "Three", 4, "Four", 5, "Five", 13, "Thirteen",
                14, "Fourteen", 15, "Fifteen", 16, "Sixteen");

        cursor.starting();
        intTrie.traverse(cursor);
        cursor.finished();
        
        cursor.starting();
        for (Map.Entry<Integer, String> entry : intTrie.entrySet())
            cursor.select(entry);
        cursor.finished();
        
        cursor.starting();
        for (Integer integer : intTrie.keySet())
            cursor.checkKey(integer);
        cursor.finished();
        
        cursor.starting();
        for (String string : intTrie.values())
            cursor.checkValue(string);
        cursor.finished();

        PatriciaTrie<Character, String> charTrie = new PatriciaTrie<Character, String>(new AlphaKeyCreator());
        charTrie.put('c', "c");
        charTrie.put('p', "p");
        charTrie.put('l', "l");
        charTrie.put('t', "t");
        charTrie.put('k', "k");
        charTrie.put('a', "a");
        charTrie.put('y', "y");
        charTrie.put('r', "r");
        charTrie.put('u', "u");
        charTrie.put('o', "o");
        charTrie.put('w', "w");
        charTrie.put('i', "i");
        charTrie.put('e', "e");
        charTrie.put('x', "x");
        charTrie.put('q', "q");
        charTrie.put('b', "b");
        charTrie.put('j', "j");
        charTrie.put('s', "s");
        charTrie.put('n', "n");
        charTrie.put('v', "v");
        charTrie.put('g', "g");
        charTrie.put('h', "h");
        charTrie.put('m', "m");
        charTrie.put('z', "z");
        charTrie.put('f', "f");
        charTrie.put('d', "d");
        cursor = new TestCursor('a', "a", 'b', "b", 'c', "c", 'd', "d", 'e', "e",
                'f', "f", 'g', "g", 'h', "h", 'i', "i", 'j', "j",
                'k', "k", 'l', "l", 'm', "m", 'n', "n", 'o', "o",
                'p', "p", 'q', "q", 'r', "r", 's', "s", 't', "t",
                'u', "u", 'v', "v", 'w', "w", 'x', "x", 'y', "y", 
                'z', "z");
        
        cursor.starting();
        charTrie.traverse(cursor);
        cursor.finished();

        cursor.starting();
        for (Map.Entry<Character, String> entry : charTrie.entrySet())
            cursor.select(entry);
        cursor.finished();
        
        cursor.starting();
        for (Character character : charTrie.keySet())
            cursor.checkKey(character);
        cursor.finished();
        
        cursor.starting();
        for (String string : charTrie.values())
            cursor.checkValue(string);
        cursor.finished();
    }
    
    public void testSelect() {
        PatriciaTrie<Character, String> charTrie = new PatriciaTrie<Character, String>(new AlphaKeyCreator());
        charTrie.put('c', "c");
        charTrie.put('p', "p");
        charTrie.put('l', "l");
        charTrie.put('t', "t");
        charTrie.put('k', "k");
        charTrie.put('a', "a");
        charTrie.put('y', "y");
        charTrie.put('r', "r");
        charTrie.put('u', "u");
        charTrie.put('o', "o");
        charTrie.put('w', "w");
        charTrie.put('i', "i");
        charTrie.put('e', "e");
        charTrie.put('x', "x");
        charTrie.put('q', "q");
        charTrie.put('b', "b");
        charTrie.put('j', "j");
        charTrie.put('s', "s");
        charTrie.put('n', "n");
        charTrie.put('v', "v");
        charTrie.put('g', "g");
        charTrie.put('h', "h");
        charTrie.put('m', "m");
        charTrie.put('z', "z");
        charTrie.put('f', "f");
        charTrie.put('d', "d");
        TestCursor cursor = new TestCursor(
                'd', "d", 'e', "e", 'f', "f", 'g', "g",
                'a', "a", 'b', "b", 'c', "c",  
                'l', "l", 'm', "m", 'n', "n", 'o', "o",
                'h', "h", 'i', "i", 'j', "j", 'k', "k", 
                't', "t", 'u', "u", 'v', "v", 'w', "w",
                'p', "p", 'q', "q", 'r', "r", 's', "s", 
                'x', "x", 'y', "y", 'z', "z");
                
        assertEquals(26, charTrie.size());
        
        cursor.starting();
        charTrie.select('d', cursor);
        cursor.finished();
    }
    
    public void testTraverseCursorRemove() {
        PatriciaTrie<Character, String> charTrie = new PatriciaTrie<Character, String>(new AlphaKeyCreator());
        charTrie.put('c', "c");
        charTrie.put('p', "p");
        charTrie.put('l', "l");
        charTrie.put('t', "t");
        charTrie.put('k', "k");
        charTrie.put('a', "a");
        charTrie.put('y', "y");
        charTrie.put('r', "r");
        charTrie.put('u', "u");
        charTrie.put('o', "o");
        charTrie.put('w', "w");
        charTrie.put('i', "i");
        charTrie.put('e', "e");
        charTrie.put('x', "x");
        charTrie.put('q', "q");
        charTrie.put('b', "b");
        charTrie.put('j', "j");
        charTrie.put('s', "s");
        charTrie.put('n', "n");
        charTrie.put('v', "v");
        charTrie.put('g', "g");
        charTrie.put('h', "h");
        charTrie.put('m', "m");
        charTrie.put('z', "z");
        charTrie.put('f', "f");
        charTrie.put('d', "d");
        TestCursor cursor = new TestCursor('a', "a", 'b', "b", 'c', "c", 'd', "d", 'e', "e",
                'f', "f", 'g', "g", 'h', "h", 'i', "i", 'j', "j",
                'k', "k", 'l', "l", 'm', "m", 'n', "n", 'o', "o",
                'p', "p", 'q', "q", 'r', "r", 's', "s", 't', "t",
                'u', "u", 'v', "v", 'w', "w", 'x', "x", 'y', "y", 
                'z', "z");
        
        cursor.starting();
        charTrie.traverse(cursor);
        cursor.finished();
        
        // Test removing both an internal & external node.
        // 'm' is an example External node in this Trie, and 'p' is an internal.
        
        assertEquals(26, charTrie.size());
        
        Object[] toRemove = new Object[] { 'g', 'd', 'e', 'm', 'p', 'q', 'r', 's' };
        cursor.addToRemove(toRemove);
        
        cursor.starting();
        charTrie.traverse(cursor);
        cursor.finished();
            
        assertEquals(26 - toRemove.length, charTrie.size());

        cursor.starting();
        charTrie.traverse(cursor);
        cursor.finished();
        
        cursor.starting();
        for (Entry<Character, String> entry : charTrie.entrySet()) {
            cursor.select(entry);
            if (Arrays.asList(toRemove).contains(entry.getKey()))
                fail("got an: " + entry);
        }
        cursor.finished();
    }
    
    public void testIteratorRemove() {
        PatriciaTrie<Character, String> charTrie = new PatriciaTrie<Character, String>(new AlphaKeyCreator());
        charTrie.put('c', "c");
        charTrie.put('p', "p");
        charTrie.put('l', "l");
        charTrie.put('t', "t");
        charTrie.put('k', "k");
        charTrie.put('a', "a");
        charTrie.put('y', "y");
        charTrie.put('r', "r");
        charTrie.put('u', "u");
        charTrie.put('o', "o");
        charTrie.put('w', "w");
        charTrie.put('i', "i");
        charTrie.put('e', "e");
        charTrie.put('x', "x");
        charTrie.put('q', "q");
        charTrie.put('b', "b");
        charTrie.put('j', "j");
        charTrie.put('s', "s");
        charTrie.put('n', "n");
        charTrie.put('v', "v");
        charTrie.put('g', "g");
        charTrie.put('h', "h");
        charTrie.put('m', "m");
        charTrie.put('z', "z");
        charTrie.put('f', "f");
        charTrie.put('d', "d");
        TestCursor cursor = new TestCursor('a', "a", 'b', "b", 'c', "c", 'd', "d", 'e', "e",
                'f', "f", 'g', "g", 'h', "h", 'i', "i", 'j', "j",
                'k', "k", 'l', "l", 'm', "m", 'n', "n", 'o', "o",
                'p', "p", 'q', "q", 'r', "r", 's', "s", 't', "t",
                'u', "u", 'v', "v", 'w', "w", 'x', "x", 'y', "y", 
                'z', "z");
        
        // Test removing both an internal & external node.
        // 'm' is an example External node in this Trie, and 'p' is an internal.
        
        assertEquals(26, charTrie.size());
        
        Object[] toRemove = new Object[] { 'e', 'm', 'p', 'q', 'r', 's' };
        
        cursor.starting();
        for(Iterator<Map.Entry<Character, String>> i = charTrie.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<Character,String> entry = i.next();
            cursor.select(entry);
            if(Arrays.asList(toRemove).contains(entry.getKey()))
                i.remove();            
        }
        cursor.finished();
            
        assertEquals(26 - toRemove.length, charTrie.size());
        
        cursor.remove(toRemove);

        cursor.starting();
        for (Entry<Character, String> entry : charTrie.entrySet()) {
            cursor.select(entry);
            if (Arrays.asList(toRemove).contains(entry.getKey()))
                fail("got an: " + entry);
        }
        cursor.finished();
    }
    
    public void testHamlet() throws Exception {
        // Make sure that Hamlet is read & stored in the same order as a SortedSet.
        List<String> original = new ArrayList<String>();
        List<String> control = new ArrayList<String>();
        SortedMap<String, String> sortedControl = new TreeMap<String, String>();
        PatriciaTrie<String, String> trie = new PatriciaTrie<String, String>(new CharSequenceKeyAnalyzer());
        
        File hamlet = TestUtils.getResourceFile("org/limewire/collection/hamlet.txt");
        BufferedReader reader = new BufferedReader(new FileReader(hamlet));
        String read = null;
        while( (read = reader.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(read);
            while(st.hasMoreTokens()) {
                String token = st.nextToken();
                original.add(token);
                sortedControl.put(token, token);
                trie.put(token, token);
            }
        }
        control.addAll(sortedControl.values());

        assertEquals(control.size(), sortedControl.size());
        assertEquals(sortedControl.size(), trie.size());
        Iterator<String> iter = trie.values().iterator();
        for (String aControl : control) {
            assertEquals(aControl, iter.next());
        }
        
        Random rnd = new Random();
        int item = 0;
        iter = trie.values().iterator();
        int removed = 0;
        for(; item < control.size(); item++) {
            assertEquals(control.get(item), iter.next());
            if(rnd.nextBoolean()) {
                iter.remove();
                removed++;
            }
        }
        
        assertEquals(control.size(), item);
        assertGreaterThan(0, removed);
        assertEquals(control.size(), trie.size() + removed);
        
        // reset hamlet
        trie.clear();
        for (String anOriginal : original) {
            trie.put(anOriginal, anOriginal);
        }
        
        assertEquals(sortedControl.values().toArray(), trie.values().toArray());
        assertEquals(sortedControl.keySet().toArray(), trie.keySet().toArray());
        assertEquals(sortedControl.entrySet().toArray(), trie.entrySet().toArray());
        
        assertEquals(sortedControl.firstKey(), trie.firstKey());
        assertEquals(sortedControl.lastKey(), trie.lastKey());
        
        SortedMap<String, String> sub = trie.headMap(control.get(523));
        assertEquals(523, sub.size());
        for(int i = 0; i < control.size(); i++) {
            if(i < 523)
                assertTrue(sub.containsKey(control.get(i)));
            else
                assertFalse(sub.containsKey(control.get(i)));
        }
        // Too slow to check values on all, so just do a few.
        assertTrue(sub.containsValue(control.get(522)));
        assertFalse(sub.containsValue(control.get(523)));
        assertFalse(sub.containsValue(control.get(524)));
        
        try {
            sub.headMap(control.get(524));
            fail("should have thrown IAE");
        } catch(IllegalArgumentException expected) {}
        
        assertEquals(sub.lastKey(), control.get(522));
        assertEquals(sub.firstKey(), control.get(0));
        
        sub = sub.tailMap(control.get(234));
        assertEquals(289, sub.size());
        assertEquals(control.get(234), sub.firstKey());
        assertEquals(control.get(522), sub.lastKey());
        for(int i = 0; i < control.size(); i++) {
            if(i < 523 && i > 233)
                assertTrue(sub.containsKey(control.get(i)));
            else
                assertFalse(sub.containsKey(control.get(i)));
        }

        try {
            sub.tailMap(control.get(232));
            fail("should have thrown IAE");
        } catch(IllegalArgumentException expected) {}
        
        sub = sub.subMap(control.get(300), control.get(400));
        assertEquals(100, sub.size());
        assertEquals(control.get(300), sub.firstKey());
        assertEquals(control.get(399), sub.lastKey());
        
        for(int i = 0; i < control.size(); i++) {
            if(i < 400 && i > 299)
                assertTrue(sub.containsKey(control.get(i)));
            else
                assertFalse(sub.containsKey(control.get(i)));
        }
    }
    
    public void testPrefixedBy() {
        PatriciaTrie<String, String> trie 
            = new PatriciaTrie<String, String>(new CharSequenceKeyAnalyzer());
        
        final String[] keys = new String[]{
                "", 
                "Albert", "Xavier", "XyZ", "Anna", "Alien", "Alberto",
                "Alberts", "Allie", "Alliese", "Alabama", "Banane",
                "Blabla", "Amber", "Ammun", "Akka", "Akko", "Albertoo",
                "Amma"
        };

        for (String key : keys) {
            trie.put(key, key);
        }
        
        SortedMap<String, String> map;
        Iterator<String> iterator;
        Iterator<Map.Entry<String, String>> entryIterator;
        Map.Entry<String, String> entry;
        
        map = trie.getPrefixedBy("Al");
        assertEquals(8, map.size());
        assertEquals("Alabama", map.firstKey());
        assertEquals("Alliese", map.lastKey());
        assertEquals("Albertoo", map.get("Albertoo"));
        assertNotNull(trie.get("Xavier"));
        assertNull(map.get("Xavier"));
        assertNull(trie.get("Alice"));
        assertNull(map.get("Alice"));
        iterator = map.values().iterator();
        assertEquals("Alabama", iterator.next());
        assertEquals("Albert", iterator.next());
        assertEquals("Alberto", iterator.next());
        assertEquals("Albertoo", iterator.next());
        assertEquals("Alberts", iterator.next());
        assertEquals("Alien", iterator.next());
        assertEquals("Allie", iterator.next());
        assertEquals("Alliese", iterator.next());
        assertFalse(iterator.hasNext());
        
        map = trie.getPrefixedBy("Albert");
        iterator = map.keySet().iterator();
        assertEquals("Albert", iterator.next());
        assertEquals("Alberto", iterator.next());
        assertEquals("Albertoo", iterator.next());
        assertEquals("Alberts", iterator.next());
        assertFalse(iterator.hasNext());
        assertEquals(4, map.size());
        assertEquals("Albert", map.firstKey());
        assertEquals("Alberts", map.lastKey());
        assertNull(trie.get("Albertz"));
        map.put("Albertz", "Albertz");
        assertEquals("Albertz", trie.get("Albertz"));
        assertEquals(5, map.size());
        assertEquals("Albertz", map.lastKey());
        iterator = map.keySet().iterator();
        assertEquals("Albert", iterator.next());
        assertEquals("Alberto", iterator.next());
        assertEquals("Albertoo", iterator.next());
        assertEquals("Alberts", iterator.next());
        assertEquals("Albertz", iterator.next());
        assertFalse(iterator.hasNext());
        assertEquals("Albertz", map.remove("Albertz"));
        
        map = trie.getPrefixedBy("Alberto");
        assertEquals(2, map.size());
        assertEquals("Alberto", map.firstKey());
        assertEquals("Albertoo", map.lastKey());
        entryIterator = map.entrySet().iterator();
        entry = entryIterator.next();
        assertEquals("Alberto", entry.getKey());
        assertEquals("Alberto", entry.getValue());
        entry = entryIterator.next();
        assertEquals("Albertoo", entry.getKey());
        assertEquals("Albertoo", entry.getValue());
        assertFalse(entryIterator.hasNext());
        trie.put("Albertoad", "Albertoad");
        assertEquals(3, map.size());
        assertEquals("Alberto", map.firstKey());
        assertEquals("Albertoo", map.lastKey());
        entryIterator = map.entrySet().iterator();
        entry = entryIterator.next();
        assertEquals("Alberto", entry.getKey());
        assertEquals("Alberto", entry.getValue());
        entry = entryIterator.next();
        assertEquals("Albertoad", entry.getKey());
        assertEquals("Albertoad", entry.getValue());
        entry = entryIterator.next();
        assertEquals("Albertoo", entry.getKey());
        assertEquals("Albertoo", entry.getValue());
        assertFalse(entryIterator.hasNext());
        assertEquals("Albertoo", trie.remove("Albertoo"));
        assertEquals("Alberto", map.firstKey());
        assertEquals("Albertoad", map.lastKey());
        assertEquals(2, map.size());
        entryIterator = map.entrySet().iterator();
        entry = entryIterator.next();
        assertEquals("Alberto", entry.getKey());
        assertEquals("Alberto", entry.getValue());
        entry = entryIterator.next();
        assertEquals("Albertoad", entry.getKey());
        assertEquals("Albertoad", entry.getValue());
        assertFalse(entryIterator.hasNext());
        assertEquals("Albertoad", trie.remove("Albertoad"));
        trie.put("Albertoo", "Albertoo");
        
        map = trie.getPrefixedBy("X");
        assertEquals(2, map.size());
        assertFalse(map.containsKey("Albert"));
        assertTrue(map.containsKey("Xavier"));
        assertFalse(map.containsKey("Xalan"));
        iterator = map.values().iterator();
        assertEquals("Xavier", iterator.next());
        assertEquals("XyZ", iterator.next());
        assertFalse(iterator.hasNext());
        
        map = trie.getPrefixedBy("An");
        assertEquals(1, map.size());
        assertEquals("Anna", map.firstKey());
        assertEquals("Anna", map.lastKey());
        iterator = map.keySet().iterator();
        assertEquals("Anna", iterator.next());
        assertFalse(iterator.hasNext());
        
        map = trie.getPrefixedBy("Ban");
        assertEquals(1, map.size());
        assertEquals("Banane", map.firstKey());
        assertEquals("Banane", map.lastKey());
        iterator = map.keySet().iterator();
        assertEquals("Banane", iterator.next());
        assertFalse(iterator.hasNext());
        
        map = trie.getPrefixedBy("Am");
        assertFalse(map.isEmpty());
        assertEquals(3, map.size());
        assertEquals("Amber", trie.remove("Amber"));
        iterator = map.keySet().iterator();
        assertEquals("Amma", iterator.next());
        assertEquals("Ammun", iterator.next());
        assertFalse(iterator.hasNext());
        iterator = map.keySet().iterator();
        map.put("Amber", "Amber");
        assertEquals(3, map.size());
        try {
            iterator.next();
            fail("CME expected");
        } catch(ConcurrentModificationException expected) {}
        assertEquals("Amber", map.firstKey());
        assertEquals("Ammun", map.lastKey());
        
        map = trie.getPrefixedBy("Ak\0");
        assertTrue(map.isEmpty());
        
        map = trie.getPrefixedBy("Ak");
        assertEquals(2, map.size());
        assertEquals("Akka", map.firstKey());
        assertEquals("Akko", map.lastKey());
        map.put("Ak", "Ak");
        assertEquals("Ak", map.firstKey());
        assertEquals("Akko", map.lastKey());
        assertEquals(3, map.size());
        trie.put("Al", "Al");
        assertEquals(3, map.size());
        assertEquals("Ak", map.remove("Ak"));
        assertEquals("Akka", map.firstKey());
        assertEquals("Akko", map.lastKey());
        assertEquals(2, map.size());
        iterator = map.keySet().iterator();
        assertEquals("Akka", iterator.next());
        assertEquals("Akko", iterator.next());
        assertFalse(iterator.hasNext());
        assertEquals("Al", trie.remove("Al"));
        
        map = trie.getPrefixedBy("Akka");
        assertEquals(1, map.size());
        assertEquals("Akka", map.firstKey());
        assertEquals("Akka", map.lastKey());
        iterator = map.keySet().iterator();
        assertEquals("Akka", iterator.next());
        assertFalse(iterator.hasNext());
        
        map = trie.getPrefixedBy("Ab");
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        try {
            Object o = map.firstKey();
            fail("got a first key: " + o);
        } catch(NoSuchElementException nsee) {}
        try {
            Object o = map.lastKey();
            fail("got a last key: " + o);
        } catch(NoSuchElementException nsee) {}
        iterator = map.values().iterator();
        assertFalse(iterator.hasNext());
        
        map = trie.getPrefixedBy("Albertooo");
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        try {
            Object o = map.firstKey();
            fail("got a first key: " + o);
        } catch(NoSuchElementException nsee) {}
        try {
            Object o = map.lastKey();
            fail("got a last key: " + o);
        } catch(NoSuchElementException nsee) {}
        iterator = map.values().iterator();
        assertFalse(iterator.hasNext());
        
        map = trie.getPrefixedBy("");
        assertSame(trie, map); // stricter than necessary, but a good check
        
        map = trie.getPrefixedBy("\0");
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        try {
            Object o = map.firstKey();
            fail("got a first key: " + o);
        } catch(NoSuchElementException nsee) {}
        try {
            Object o = map.lastKey();
            fail("got a last key: " + o);
        } catch(NoSuchElementException nsee) {}
        iterator = map.values().iterator();
        assertFalse(iterator.hasNext());
    }
    
    public void testPrefixByOffsetAndLength() {
        PatriciaTrie<String, String> trie 
            = new PatriciaTrie<String, String>(new CharSequenceKeyAnalyzer());
        
        final String[] keys = new String[]{
                "Albert", "Xavier", "XyZ", "Anna", "Alien", "Alberto",
                "Alberts", "Allie", "Alliese", "Alabama", "Banane",
                "Blabla", "Amber", "Ammun", "Akka", "Akko", "Albertoo",
                "Amma"
        };
    
        for (String key : keys) {
            trie.put(key, key);
        }
        
        SortedMap<String, String> map;
        Iterator<String> iterator;
        
        map = trie.getPrefixedBy("Alice", 2);
        assertEquals(8, map.size());
        assertEquals("Alabama", map.firstKey());
        assertEquals("Alliese", map.lastKey());
        assertEquals("Albertoo", map.get("Albertoo"));
        assertNotNull(trie.get("Xavier"));
        assertNull(map.get("Xavier"));
        assertNull(trie.get("Alice"));
        assertNull(map.get("Alice"));
        iterator = map.values().iterator();
        assertEquals("Alabama", iterator.next());
        assertEquals("Albert", iterator.next());
        assertEquals("Alberto", iterator.next());
        assertEquals("Albertoo", iterator.next());
        assertEquals("Alberts", iterator.next());
        assertEquals("Alien", iterator.next());
        assertEquals("Allie", iterator.next());
        assertEquals("Alliese", iterator.next());
        assertFalse(iterator.hasNext());
        
        map = trie.getPrefixedBy("BAlice", 1, 2);
        assertEquals(8, map.size());
        assertEquals("Alabama", map.firstKey());
        assertEquals("Alliese", map.lastKey());
        assertEquals("Albertoo", map.get("Albertoo"));
        assertNotNull(trie.get("Xavier"));
        assertNull(map.get("Xavier"));
        assertNull(trie.get("Alice"));
        assertNull(map.get("Alice"));
        iterator = map.values().iterator();
        assertEquals("Alabama", iterator.next());
        assertEquals("Albert", iterator.next());
        assertEquals("Alberto", iterator.next());
        assertEquals("Albertoo", iterator.next());
        assertEquals("Alberts", iterator.next());
        assertEquals("Alien", iterator.next());
        assertEquals("Allie", iterator.next());
        assertEquals("Alliese", iterator.next());
        assertFalse(iterator.hasNext());
    }
    
    public void testPrefixedByRemoval() {
        PatriciaTrie<String, String> trie 
            = new PatriciaTrie<String, String>(new CharSequenceKeyAnalyzer());
        
        final String[] keys = new String[]{
                "Albert", "Xavier", "XyZ", "Anna", "Alien", "Alberto",
                "Alberts", "Allie", "Alliese", "Alabama", "Banane",
                "Blabla", "Amber", "Ammun", "Akka", "Akko", "Albertoo",
                "Amma"
        };

        for (String key : keys) {
            trie.put(key, key);
        }
        
        SortedMap<String, String> map = trie.getPrefixedBy("Al");
        assertEquals(8, map.size());
        Iterator<String> iter = map.keySet().iterator();
        assertEquals("Alabama", iter.next());
        assertEquals("Albert", iter.next());
        assertEquals("Alberto", iter.next());
        assertEquals("Albertoo", iter.next());
        assertEquals("Alberts", iter.next());
        assertEquals("Alien", iter.next());
        iter.remove();
        assertEquals(7, map.size());
        assertEquals("Allie", iter.next());
        assertEquals("Alliese", iter.next());
        assertFalse(iter.hasNext());
        
        map = trie.getPrefixedBy("Ak");
        assertEquals(2, map.size());
        iter = map.keySet().iterator();
        assertEquals("Akka", iter.next());
        iter.remove();
        assertEquals(1, map.size());
        assertEquals("Akko", iter.next());
        if(iter.hasNext())
            fail("shouldn't have next (but was: " + iter.next() + ")");
        assertFalse(iter.hasNext());
    }

    public void testTraverseWithAllNullBitKey() {
        PatriciaTrie<String, String> trie 
            = new PatriciaTrie<String, String>(new CharSequenceKeyAnalyzer());
        
        //
        // One entry in the Trie
        // Entry is stored at the root
        //
        
        // trie.put("", "All Bits Are Zero");
        trie.put("\0", "All Bits Are Zero");
        
        //
        //  / ("")   <-- root
        //  \_/  \
        //       null
        //
        
        final List<String> strings = new ArrayList<String>();
        trie.traverse(new Cursor<String, String>() {
            public SelectStatus select(Entry<? extends String, ? extends String> entry) {
                strings.add(entry.getValue());
                return SelectStatus.CONTINUE;
            }
        });
        
        assertEquals(1, strings.size());
        
        strings.clear();
        for (String s : trie.values()) {
            strings.add(s);
        }
        assertEquals(1, strings.size());
    }
    
    @SuppressWarnings({"MismatchedQueryAndUpdateOfCollection"})
    public void testSelectWithAllNullBitKey() {
        PatriciaTrie<String, String> trie 
            = new PatriciaTrie<String, String>(new CharSequenceKeyAnalyzer());
        
        // trie.put("", "All Bits Are Zero");
        trie.put("\0", "All Bits Are Zero");
        
        final List<String> strings = new ArrayList<String>();
        trie.select("Hello", new Cursor<String, String>() {
            public SelectStatus select(Entry<? extends String, ? extends String> entry) {
                strings.add(entry.getValue());
                return SelectStatus.CONTINUE;
            }
        });
        assertEquals(1, strings.size());
    }
    
    private static class TestCursor implements Cursor<Object, Object> {
        private List<Object> keys;
        private List<Object> values;
        private Object selectFor;
        private List<Object> toRemove;
        private int index = 0;
        
        TestCursor(Object... objects) {
            if(objects.length % 2 != 0)
                throw new IllegalArgumentException("must be * 2");
            
            keys = new ArrayList<Object>(objects.length / 2);
            values = new ArrayList<Object>(keys.size());
            toRemove = Collections.emptyList();
            for(int i = 0; i < objects.length; i++) {
                keys.add(objects[i]);
                values.add(objects[++i]);
            }
        }
        
        void selectFor(Object object) {
            selectFor = object;
        }
        
        void addToRemove(Object... objects) {
            toRemove = new ArrayList<Object>(Arrays.asList(objects));
        }
        
        void remove(Object... objects) {
            for (Object object : objects) {
                int idx = keys.indexOf(object);
                keys.remove(idx);
                values.remove(idx);
            }
        }
        
        void starting() {
            index = 0;
        }
        
        public void checkKey(Object k) {
            assertEquals(keys.get(index++), k);
        }
        
        public void checkValue(Object o) {
            assertEquals(values.get(index++), o);
        }

        public SelectStatus select(Entry<?, ?> entry) {
          //  System.out.println("Scanning: " + entry.getKey());
            assertEquals(keys.get(index), entry.getKey());
            assertEquals(values.get(index), entry.getValue());
            index++;
            
            if(toRemove.contains(entry.getKey())) {
              // System.out.println("Removing: " + entry.getKey());
                index--;
                keys.remove(index);
                values.remove(index);
                toRemove.remove(entry.getKey());
                return SelectStatus.REMOVE;
            } 
            
            if(selectFor != null && selectFor.equals(entry.getKey()))
                return SelectStatus.EXIT;
            else
                return SelectStatus.CONTINUE;
        }
        
        void finished() {
            assertEquals(keys.size(), index);
        }
    }

    private static class IntegerKeyCreator implements KeyAnalyzer<Integer> {

        public static int[] createIntBitMask(final int bitCount) {
            int[] bits = new int[bitCount];
            for (int i = 0; i < bitCount; i++) {
                bits[i] = 1 << (bitCount - i - 1);
            }
            return bits;
        }

        private static final int[] BITS = createIntBitMask(32);

        public int length(Integer key) {
            return 32;
        }

        public boolean isBitSet(Integer key, int keyLength, int bitIndex) {
            return (key & BITS[bitIndex]) != 0;
        }

        public int bitIndex(Integer key,   int keyOff, int keyLength,
                            Integer found, int foundOff, int foundKeyLength) {
            if (found == null)
                found = 0;
            
            if(keyOff != 0 || foundOff != 0)
                throw new IllegalArgumentException("offsets must be 0 for fixed-size keys");

            boolean allNull = true;
            
            int length = Math.max(keyLength, foundKeyLength);
            
            for (int i = 0; i < length; i++) {
                int a = key & BITS[i];
                int b = found & BITS[i];

                if (allNull && a != 0) {
                    allNull = false;
                }

                if (a != b) {
                    return i;
                }
            }

            if (allNull) {
                return KeyAnalyzer.NULL_BIT_KEY;
            }

            return KeyAnalyzer.EQUAL_BIT_KEY;
        }

        public int compare(Integer o1, Integer o2) {
            return o1.compareTo(o2);
        }

        public int bitsPerElement() {
            return 1;
        }
        
        public boolean isPrefix(Integer prefix, int offset, int length, Integer key) {
            int addr1 = prefix;
            int addr2 = key;
            addr1 = addr1 << offset;
            
            int mask = 0;
            for(int i = 0; i < length; i++) {
                mask |= (0x1 << i);
            }
            
            addr1 &= mask;
            addr2 &= mask;
            
            return addr1 == addr2;
        }
    }

    private static class AlphaKeyCreator implements KeyAnalyzer<Character> {

        public static int[] createIntBitMask(final int bitCount) {
            int[] bits = new int[bitCount];
            for (int i = 0; i < bitCount; i++) {
                bits[i] = 1 << (bitCount - i - 1);
            }
            return bits;
        }

        private static final int[] BITS = createIntBitMask(16);

        public int length(Character key) {
            return 16;
        }

        public boolean isBitSet(Character key, int keyLength, int bitIndex) {
            return (key & BITS[bitIndex]) != 0;
        }

        public int bitIndex(Character key,   int keyOff, int keyLength,
                            Character found, int foundOff, int foundKeyLength) {
            if (found == null)
                found = (char)0;
            
            if(keyOff != 0 || foundOff != 0)
                throw new IllegalArgumentException("offsets must be 0 for fixed-size keys");
            
            int length = Math.max(keyLength, foundKeyLength);

            boolean allNull = true;
            for (int i = 0; i < length; i++) {
                int a = key & BITS[i];
                int b = found & BITS[i];

                if (allNull && a != 0) {
                    allNull = false;
                }

                if (a != b) {
                    return i;
                }
            }

            if (allNull) {
                return KeyAnalyzer.NULL_BIT_KEY;
            }

            return KeyAnalyzer.EQUAL_BIT_KEY;
        }

        public int compare(Character o1, Character o2) {
            return o1.compareTo(o2);
        }

        public int bitsPerElement() {
            return 1;
        }
        
        public boolean isPrefix(Character prefix, int offset, int length, Character key) {
            int addr1 = prefix;
            int addr2 = key;
            addr1 = addr1 << offset;
            
            int mask = 0;
            for(int i = 0; i < length; i++) {
                mask |= (0x1 << i);
            }
            
            addr1 &= mask;
            addr2 &= mask;
            
            return addr1 == addr2;
        }
        
        
    }

}
