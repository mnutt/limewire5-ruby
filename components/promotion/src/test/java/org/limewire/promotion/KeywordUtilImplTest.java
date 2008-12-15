package org.limewire.promotion;

import java.util.List;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class KeywordUtilImplTest extends BaseTestCase {
    public KeywordUtilImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(KeywordUtilImplTest.class);
    }

    public void testGetHashValue() {
        KeywordUtilImpl keywordUtil = new KeywordUtilImpl();
        assertEquals(859844163007352795L, keywordUtil.getHashValue("foo"));
        assertEquals(859844163007352795L, keywordUtil.getHashValue("fOo"));
        assertNotEquals(859844163007352795L, keywordUtil.getHashValue("a foo"));

        assertEquals(7771131797414839755L, keywordUtil.getHashValue("big foo"));
        assertEquals(7771131797414839755L, keywordUtil.getHashValue("FOO BIG"));
        assertEquals(7771131797414839755L, keywordUtil.getHashValue("a big foo"));
        assertEquals(7771131797414839755L, keywordUtil.getHashValue("a foo big"));


        assertSameHash("a", "A");
        assertSameHash("big bird", "a big bird");
        assertSameHash("I shot the sherriff", "sherriff shot");
        assertSameHash("I shot the sherriff", "sherriff shot");
        assertSameHash("I shot the sherriff", "sherriff shot's");

        assertSameHash("The Low Lows", "low lows");
        assertSameHash("vacation Hawaii golf", "vacation Hawaii");
        assertSameHash("britney's going crazy", "britney is crazy");
        assertSameHash("britney's going crazy", "britney's crazy mom");
        assertSameHash("Beyonc\u00E9", "beyonce");

        assertSameHash(" ", " ");
        assertSameHash("", "");
        assertSameHash(" ", "");
        assertSameHash(" ", ",.");

        // Make sure we never get a negative value....
        for (int i = 0; i < 10000; i++) {
            assertGreaterThan(-1, keywordUtil.getHashValue("foo" + i));
        }
    }

    private void assertSameHash(String query1, String query2) {
        KeywordUtilImpl keywordUtil = new KeywordUtilImpl();
        long hash1 = keywordUtil.getHashValue(query1);
        long hash2 = keywordUtil.getHashValue(query2);

        assertTrue("Hashes for queries did not match.", hash1 == hash2);
    }

    public void testSortAlphabetically() {
        KeywordUtilImpl keywordUtil = new KeywordUtilImpl();
        assertEquals(new String[] { "a", "b", "c" }, keywordUtil.sortAlphabetically(new String[] {
                "b", "a", "c" }));
    }

    public void testSortByLength() {
        KeywordUtilImpl keywordUtil = new KeywordUtilImpl();
        assertEquals(new String[] { "a", "b", "c" }, keywordUtil.sortByLength(new String[] { "b",
                "a", "c" }));
        assertEquals(new String[] { "ccc", "bb", "a" }, keywordUtil.sortByLength(new String[] {
                "bb", "a", "ccc" }));
        assertEquals(new String[] { "aaa", "ccc", "bb", "a" }, keywordUtil
                .sortByLength(new String[] { "bb", "aaa", "a", "ccc" }));
    }

    public void testStripEnglishStopWords() {
        KeywordUtilImpl keywordUtil = new KeywordUtilImpl();
        assertEquals(new String[] { "b", "c" }, keywordUtil.stripEnglishStopWords(new String[] {
                "a", "b", "c" }));
        assertEquals(new String[] { "a", "b" }, keywordUtil.stripEnglishStopWords(new String[] {
                "a", "b" }));
        assertEquals(new String[] { "a", "a" }, keywordUtil.stripEnglishStopWords(new String[] {
                "a", "a" }));
        assertEquals(new String[] { "a" }, keywordUtil.stripEnglishStopWords(new String[] { "a" }));
    }

    public void testUnsplitString() {
        KeywordUtilImpl keywordUtil = new KeywordUtilImpl();
        assertEquals("a b c", keywordUtil.unsplitString(new String[] { "a", "b", "c" }));
    }

    public void testStripPunctuation() {
        KeywordUtilImpl keywordUtil = new KeywordUtilImpl();
        assertEquals("a b c", keywordUtil.stripPunctuation("a,b,c"));
        assertEquals("a b", keywordUtil.stripPunctuation("a'b"));
    }

    public void testSplitKeywords() {
        KeywordUtilImpl keywordUtil = new KeywordUtilImpl();
        List<String> words = keywordUtil.splitKeywords("foo\tBAR\tfoo bar");
        assertEquals("foo", words.get(0));
        assertEquals("bar", words.get(1));
        assertEquals("bar foo", words.get(2));
    }
}
