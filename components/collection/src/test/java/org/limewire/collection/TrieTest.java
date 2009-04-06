package org.limewire.collection;

import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.PrivilegedAccessor;

/**
 * Tests Trie.
 */
@SuppressWarnings("unchecked")
public class TrieTest extends BaseTestCase {
    public TrieTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(TrieTest.class);
    }
    
    private static Locale defaultLocale;
    private static Locale turkishLocale;
    
    public static void globalSetUp() {
        defaultLocale = Locale.getDefault();
        turkishLocale = new Locale("tr"); // Turkish
    }
    
    // ensure we begin with a correct locale
    @Override
    public void setUp() {
        Locale.setDefault(defaultLocale);
    }
    
    // ensure with end with a correct locale
    @Override
    public void tearDown() {
        Locale.setDefault(defaultLocale);
    }

    private static final Object _Val    = "value for _";
    private static final Object aVal    = "value for a";
    private static final Object addVal  = "value for add";
    private static final Object anVal   = "value for an";
    private static final Object antVal  = "value for ant";
    private static final Object antVal0 = "another value for ant";
    
    public void testBasic() throws Throwable {
        Locale.setDefault(defaultLocale);
        tTrie_Basic();
    }
    
    public void testBasicInTurkish() throws Throwable {
        Locale.setDefault(turkishLocale);
        tTrie_Basic();
    }

    // Basic Trie operations
    private void tTrie_Basic() throws Throwable {
        StringTrie t = new StringTrie(false);
        Iterator iter = null;

	    //Note int Trie.match(String, int, int String) is private, call it
        //using the helper function
        assertEquals(-1, match(t, "abcde", 0, 5, "abc"));
        assertEquals(1, match(t, "abcde", 1, 5, "bXd"));
        assertEquals(2, match(t, "abcde", 1, 3, "bcd"));
        assertNull(t.get("a"));

        // additive insert
        assertNull(t.add("ant", antVal0));
        //System.out.println(t.toString());
        // splice insert
        assertNull(t.add("an", anVal));
        //System.out.println(t.toString());
        // split insert
        assertNull(t.add("add", addVal));
        //System.out.println(t.toString());
        // relabel insert (old -> new)
        assertSame(antVal0, t.add("ant", antVal));
        //System.out.println(t.toString());
        // relabel insert (NULL -> new)
        assertNull(t.add("a", aVal));
        //System.out.println(t.toString());
        // relabel insert (NULL -> new)
        assertNull(t.add("_", _Val));
        //System.out.println(t.toString());

        assertSame(_Val, t.get("_"));
        assertSame(aVal, t.get("a"));
        assertSame(anVal, t.get("an"));
        assertSame(antVal, t.get("ant"));
        assertSame(addVal, t.get("add"));
        assertNull(t.get("aDd"));

        // Yield all elements (in forward sort order)...
        assertIterator(iter = t.getPrefixedBy(""));
        expectNextSame(_Val, iter);
        expectNextSame(aVal, iter);
        expectNextSame(addVal, iter);
        expectNextSame(anVal, iter);
        expectNextSame(antVal, iter);
        assertEmpty(iter);

        // Yield no elements...
        assertEmpty(t.getPrefixedBy("ab"));
        assertEmpty(t.getPrefixedBy("ants"));

        // Yield 1 element...starting in middle of prefix
        assertIterator(iter = t.getPrefixedBy("ad"));
        expectNextSame(addVal, iter);
        assertEmpty(iter);

        // Yield many elements...
        iter = t.getPrefixedBy("a");
        assertIterator(iter);
        expectNextSame(aVal, iter);
        expectNextSame(addVal, iter);
        expectNextSame(anVal, iter);
        expectNextSame(antVal, iter);
        assertEmpty(iter);
    }

    public void testEmptyString() throws Throwable {
        Locale.setDefault(defaultLocale);
        tTrie_EmptyString();
    }
    
    public void testEmptyStringInTurkish() throws Throwable {
        Locale.setDefault(turkishLocale);
        tTrie_EmptyString();
    }

    // Empty string
    private void tTrie_EmptyString() {
        StringTrie t = new StringTrie(false);
        Iterator iter = null;

        assertNull(t.get(""));

        assertNull(t.add("", aVal));
        assertNull(t.add("an", anVal));
        assertEquals(aVal, t.get(""));

        assertIterator(iter = t.getPrefixedBy(""));
        expectNextSame(aVal, iter);
        expectNextSame(anVal, iter);
        assertEmpty(iter);
    }
    
    public void testPrefix() throws Throwable {
        Locale.setDefault(defaultLocale);
        tTrie_Prefix();
    }
    
    public void testPrefixInTurkish() throws Throwable {
        Locale.setDefault(turkishLocale);
        tTrie_Prefix();
    }    

    // Prefix and substring prefix tests
    private void tTrie_Prefix() {
        StringTrie t = new StringTrie(false);
        Iterator iter = null;

        assertNull(t.add("an", anVal));

        assertIterator(iter = t.getPrefixedBy("XanXX"));
        assertEmpty(iter);

        assertIterator(iter = t.getPrefixedBy(""));
        expectNextSame(anVal, iter);
        assertEmpty(iter);

        assertIterator(iter = t.getPrefixedBy("XanXX", 1, 4));
        assertEmpty(iter);

        assertIterator(iter = t.getPrefixedBy("XanXX", 1, 3));
        expectNextSame(anVal, iter);
        assertEmpty(iter);

        assertIterator(iter = t.getPrefixedBy("XanXX", 1, 2));
        expectNextSame(anVal, iter);
        assertEmpty(iter);
    }
    
    public void testRemove() throws Throwable {
        Locale.setDefault(defaultLocale);
        tTrie_Remove();
    }
    
    public void testRemoveInTurkish() throws Throwable {
        Locale.setDefault(turkishLocale);
        tTrie_Remove();
    }  
    
    // Remove tests
    private void tTrie_Remove() {
        StringTrie t = new StringTrie(false);

        assertNull(t.add("an", anVal));
        assertNull(t.add("ant", antVal));
        assertFalse(t.remove("x"));
        assertFalse(t.remove("a"));
        assertTrue(t.remove("an"));

        assertNull(t.get("an"));
        assertSame(antVal, t.get("ant"));
    }
    
    public void testIgnoreCase() throws Throwable {
        Locale.setDefault(defaultLocale);
        tTrie_IgnoreCase();
    }
    
    public void testIgnoreCaseInTurkish() throws Throwable {
        Locale.setDefault(turkishLocale);
        tTrie_IgnoreCase();
    }  
    
    // Case-insensitive tests
    private void tTrie_IgnoreCase() {
        StringTrie t = new StringTrie(true);
        Iterator iter = null;

        assertNull(t.add("an", anVal));
        assertSame(anVal, t.add("An", anVal));
        assertSame(anVal, t.add("aN", anVal));
        assertSame(anVal, t.add("AN", anVal));

        assertSame(anVal, t.get("an"));
        assertSame(anVal, t.get("An"));
        assertSame(anVal, t.get("aN"));
        assertSame(anVal, t.get("AN"));

        assertNull(t.add("ant", antVal));
        assertSame(antVal, t.get("ANT"));

        assertIterator(iter = t.getPrefixedBy("a"));
        expectNextSame(anVal, iter);
        expectNextSame(antVal, iter);
        assertEmpty(iter);
    }

    private static final Object ssVal0 = "initial value for ss";
    private static final Object ssVal = "value for ss";
    private static final Object strasVal = "value for stras";
    private static final Object strassVal = "value for strass";
    private static final Object strasseVal = "value for strasse";
    private static final Object strassenVal0 = "initial value for strassen";
    private static final Object strassenVal = "value for strassen";
    private static final Object hVal = "value for h";
    private static final Object iVal0 = "value for i (capital dotted)";
    private static final Object iVal = "value for i";
    private static final Object inVal = "value for in";
    private static final Object jVal = "value for j";

    public void testIgnoreCaseSpecial() throws Throwable {
        Locale.setDefault(defaultLocale);
        tTrie_IgnoreCaseSpecial();
    }
    
    public void testIgnoreCaseSpecialInTurkish() throws Throwable {
        Locale.setDefault(turkishLocale);
        tTrie_IgnoreCaseSpecial();
    }  

    // Case-insensitive with special casing tests for Java 1.1+.
    private void tTrie_IgnoreCaseSpecial() {
        StringTrie t = null;
        Iterator iter = null;

        // Test stability of Trie with special casing rules for strings.
        // 1. German keyword: test conversion of German Ess-Tsett (sharp s)
        // First test that sharp s is converted to a pair of uppercase S
        t = new StringTrie(true);
        assertNull(t.add("\u00df", ssVal0));
        //System.out.println("Should list 'ss':");
        //System.out.println(t.toString());
        assertSame(ssVal0, t.add("ss", ssVal)); // old value replaced
        assertSame(ssVal, t.get("\u00df")); // new value kept
        assertSame(ssVal, t.get("ss")); // new value kept
        assertSame(ssVal, t.get("SS"));
        assertSame(ssVal, t.get("Ss"));
        assertSame(ssVal, t.get("sS"));
        //System.out.println(t.toString());

        assertIterator(iter = t.getPrefixedBy(""));
        expectNextSame(ssVal, iter);
        assertEmpty(iter);

        // Special case conversion stability tests
        t = new StringTrie(true);
        assertNull(t.add("Stra\u00dfe", strasseVal));
        assertNull(t.add("stra\u00df", strassVal)); // split e
        assertSame(strasseVal, t.add("STRASSE", strasseVal)); // replace
        assertNull(t.add("Strassen", strassenVal0)); // splice
        assertSame(strassenVal0, t.add("STRA\u00dfEN", strassenVal)); //replace
        assertNull(t.add("stras", strasVal)); // split s
        //System.out.println(t.toString());

        assertIterator(iter = t.getPrefixedBy(""));
        expectNextSame(strasVal, iter);
        expectNextSame(strassVal, iter);
        expectNextSame(strasseVal, iter);
        expectNextSame(strassenVal, iter);
        assertEmpty(iter);

        // 2. Turkish keyword: test of i (with or without dot above)
        // First test if capital dotted I is converted to small ASCII i
        t = new StringTrie(true);
        assertNull(t.add("\u0130", iVal0)); // capital dotted I
        //System.out.println("Should list 'i':");
        //System.out.println(t.toString());

        assertNull(t.add("h", hVal)); // add before
        assertNull(t.add("in", inVal)); // splice after
        assertNull(t.add("j", jVal)); // add after
        //System.out.println(t.toString());

        assertSame(iVal0, t.get("i"));
        assertSame(iVal0, t.get("I"));
        assertSame(iVal0, t.get("\u0130")); // capital dotted I
        assertSame(iVal0, t.get("\u0131")); // small dotless i

        assertIterator(iter = t.getPrefixedBy(""));
        expectNextSame(hVal, iter);
        expectNextSame(iVal0, iter);
        expectNextSame(inVal, iter);
        expectNextSame(jVal, iter);
        assertEmpty(iter);

        // Replace value of node "i"
        assertSame(iVal0, t.add("i", iVal));
        //System.out.println(t.toString());

        assertSame(iVal, t.get("i"));
        assertSame(iVal, t.get("I"));
        assertSame(iVal, t.get("\u0130")); // capital dotted I
        assertSame(iVal, t.get("\u0131")); // small dotless i

        assertIterator(iter = t.getPrefixedBy(""));
        expectNextSame(hVal, iter);
        expectNextSame(iVal, iter);
        expectNextSame(inVal, iter);
        expectNextSame(jVal, iter);
        assertEmpty(iter);
    }

    // accessible helper for private int Trie.match(String, int, int, String)
    private static int match(final StringTrie trie, final String a,
                                   int startOffset, int stopOffset,
                                   final String b) throws Throwable {
        try {
            return (Integer)
                    PrivilegedAccessor.invokeMethod(trie, "match",
                            new Object[]{a,
                                    startOffset, //native
                                    stopOffset,  //native
                                    b},
                            new Class[]{String.class,
                                    int.class, //native
                                    int.class, //native
                                    String.class}); // unwrap the native int result
        } catch(Exception e) {
            if( e.getCause() != null )
                throw e.getCause();
            throw e;
        }
    }

    private void assertIterator(final Iterator iter) {
        assertNotNull("expected valid not null Iterator", iter);
    }

    private void assertEmpty(final Iterator iter) {
        assertFalse(iter.hasNext());
        try {
            iter.next();
            fail("expected NoSuchElementException");
        } catch (NoSuchElementException ignore) { }
    }

    private Object expectNextSame(final Object expected, final Iterator iter) {
        assertTrue(iter.hasNext());
        final Object value = iter.next();
        assertSame("expected same <" + expected + "> got <" + value + ">",
                   expected, value);
        return value;
    }    

}
