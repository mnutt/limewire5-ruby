package org.limewire.security.certificate;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class HashLookupProviderDNSTXTTest extends BaseTestCase {
    public HashLookupProviderDNSTXTTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HashLookupProviderDNSTXTTest.class);
    }

    public void testBasicLookup() {
        HashLookupProvider provider = new HashLookupProviderDNSTXTImpl();
        assertEquals("I am a TXT record.", provider.lookup("certverify12345.store.limewire.com"));
    }

    public void test128ByteLookup() {
        HashLookupProvider provider = new HashLookupProviderDNSTXTImpl();
        assertEquals(128, provider.lookup("certverify128.store.limewire.com").length());
    }

    public void testStripLeadingTrailingQuotes() {
        HashLookupProviderDNSTXTImpl provider = new HashLookupProviderDNSTXTImpl();
        assertNull(provider.stripLeadingTrailingQuotes(null));
        assertEquals("", provider.stripLeadingTrailingQuotes(""));
        assertEquals("a", provider.stripLeadingTrailingQuotes("a"));
        assertEquals("ab", provider.stripLeadingTrailingQuotes("ab"));
        assertEquals("abc", provider.stripLeadingTrailingQuotes("abc"));

        assertEquals("a", provider.stripLeadingTrailingQuotes("\"a\""));
        assertEquals("ab", provider.stripLeadingTrailingQuotes("\"ab\""));
        assertEquals("abc", provider.stripLeadingTrailingQuotes("\"abc\""));

        assertEquals("a", provider.stripLeadingTrailingQuotes("\"a"));
        assertEquals("ab", provider.stripLeadingTrailingQuotes("\"ab"));
        assertEquals("abc", provider.stripLeadingTrailingQuotes("\"abc"));

        assertEquals("a", provider.stripLeadingTrailingQuotes("a\""));
        assertEquals("ab", provider.stripLeadingTrailingQuotes("ab\""));
        assertEquals("abc", provider.stripLeadingTrailingQuotes("abc\""));
    }
}
