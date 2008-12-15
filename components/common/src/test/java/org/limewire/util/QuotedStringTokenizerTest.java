package org.limewire.util;

import java.util.NoSuchElementException;

import junit.framework.Test;

public class QuotedStringTokenizerTest extends BaseTestCase {

    public QuotedStringTokenizerTest(String name) {
		super(name);
	}

	public static Test suite() {
        return buildTestSuite(QuotedStringTokenizerTest.class);
    }  
	
	public void testTokenizer() {
		QuotedStringTokenizer t = new QuotedStringTokenizer("a b  c  ");
		assertEquals(t.countTokens(), 3);
		assertEquals("a", t.nextToken());
		assertTrue(t.hasMoreTokens());
		assertEquals("b", t.nextToken());
		assertEquals("c", t.nextToken());
		assertFalse(t.hasMoreTokens());
		assertNoSuchElementExeception(t);
	}

	public void testReturnSeparators() {
		QuotedStringTokenizer t = new QuotedStringTokenizer("a b  c  ", " ",
				true);
		assertEquals(t.countTokens(), 8);
		assertEquals("a", t.nextToken());
		assertEquals(" ", t.nextToken());
		assertEquals("b", t.nextToken());
		assertEquals(" ", t.nextToken());
		assertEquals(" ", t.nextToken());
		assertEquals("c", t.nextToken());
		assertEquals(" ", t.nextToken());
		assertEquals(" ", t.nextToken());
		assertNoSuchElementExeception(t);
	}

	private void assertNoSuchElementExeception(QuotedStringTokenizer t) {
		try {
			String s = t.nextToken();
			fail("Expected NoSuchElementException, got + '" + s + "'");
		} catch (NoSuchElementException ignore) {
		}
	}

	public void testMultipleSeparators() {
		QuotedStringTokenizer t = new QuotedStringTokenizer("a;b c", "; ");
		assertEquals("a", t.nextToken());
		assertEquals("b", t.nextToken());
		assertEquals("c", t.nextToken());
	}

	public void testMultipleSeparatorsReturnSeparators() {
		QuotedStringTokenizer t = new QuotedStringTokenizer("a;b c;;", "; ",
				true);
		assertEquals("a", t.nextToken());
		assertEquals(";", t.nextToken());
		assertEquals("b", t.nextToken());
		assertEquals(" ", t.nextToken());
		assertEquals("c", t.nextToken());
		assertEquals(";", t.nextToken());
		assertEquals(";", t.nextToken());
	}

	public void testQuotedTokenizer() {
		QuotedStringTokenizer t = new QuotedStringTokenizer("\"a a\" b \"c d\"");
		assertEquals("a a", t.nextToken());
		assertEquals("b", t.nextToken());
		assertEquals("c d", t.nextToken());
	}

	public void testOpenQuote() {
		QuotedStringTokenizer t = new QuotedStringTokenizer("\"a a b \"c d\"");
		assertEquals("a a b c", t.nextToken());
		assertEquals("d", t.nextToken());

		t = new QuotedStringTokenizer("\"a a b \" c d\"");
		assertEquals("a a b ", t.nextToken());
		assertEquals("c", t.nextToken());
		assertEquals("d", t.nextToken());

		t = new QuotedStringTokenizer("\"a");
		assertEquals("a", t.nextToken());
	}

}
