package com.limegroup.gnutella.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.limewire.util.BaseTestCase;
import org.limewire.core.settings.SearchSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.GUID;

import junit.framework.Test;

import com.google.inject.Injector;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;

@SuppressWarnings("unchecked")
public class QueryUtilsTest extends BaseTestCase {
    
    private QueryRequestFactory queryRequestFactory;

    public QueryUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(QueryUtilsTest.class);
    }  

    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
    }
    
    public void testCreateQueryString() {
        QueryRequest qr;

        int maxQueryLength = SearchSettings.MAX_QUERY_LENGTH.getValue();
        String query = QueryUtils.createQueryString("file and 42-name_minus #numbers");
        containsAll("file name minus numbers", query);

        String tooLongString = 
                LimeTestUtils.generateRepeatingStringByLength("dummystring", maxQueryLength + 23);
        String tooLongStringTruncated = tooLongString.substring(0, maxQueryLength);

        query = QueryUtils.createQueryString(tooLongString);
        assertEquals(tooLongStringTruncated, query);

        // verify that we can create local & network queries out of the query string
        qr = queryRequestFactory.createQuery(query);
        queryRequestFactory.createMulticastQuery(GUID.makeGuid(), qr);
        
        //such query will fit any 2 out of 3 words in it.
        String thirdKeywordTooLong = "short one, " +
                LimeTestUtils.generateRepeatingStringByLength("dummystring", maxQueryLength - 7);
        query = QueryUtils.createQueryString(thirdKeywordTooLong);
        assertEquals(2,query.split(" ").length);
        qr = queryRequestFactory.createQuery(query);
        queryRequestFactory.createMulticastQuery(GUID.makeGuid(), qr);

        String firstThingWontFit_But_Others_Will =
                LimeTestUtils.generateRepeatingStringByLength("dummystring", maxQueryLength + 1) +
                ", but short others";
        query = QueryUtils.createQueryString(firstThingWontFit_But_Others_Will);
        containsAll("but short others", query);
        qr = queryRequestFactory.createQuery(query);
        queryRequestFactory.createMulticastQuery(GUID.makeGuid(), qr);
        
        query = QueryUtils.createQueryString("(5).jpg");
        assertEquals("5 jpg", query);
        qr = queryRequestFactory.createQuery(query);
        queryRequestFactory.createMulticastQuery(GUID.makeGuid(), qr);
        
        // test with allow numbers switched on
        assertEquals("500 jpg", QueryUtils.createQueryString("500 jpg", true));
        assertEquals("file 42 name minus numbers",
                QueryUtils.createQueryString("file and 42-name_minus #numbers", true));
        
        // test that string with a number + illegal characters + keyword too long
        // is truncating the keyword, not the whole string.
        String tooLongStringWithNumbersAtBeginning = "1920936_" + tooLongString;
        query = QueryUtils.createQueryString(tooLongStringWithNumbersAtBeginning);
        assertEquals(tooLongStringTruncated, query);
    }

    
    public void testRipExtension() {
        assertEquals("a", QueryUtils.ripExtension("a.b"));
        assertEquals("c.d", QueryUtils.ripExtension("c.d."));
        assertEquals("e.f.g", QueryUtils.ripExtension("e.f.g.h"));
    }
    
    public void testKeywords() {
        Set valid = new HashSet();
        valid.add("phish");
        valid.add("is");
        valid.add("dead");
        valid.add("long");
        valid.add("live");
        
        assertEquals(valid, QueryUtils.extractKeywordsFromFileName("Phish is dead! :( Long-live Phish. :)"));
        
        
        valid.clear();
        valid.add("quick");
        valid.add("foot");
        valid.add("brown");
        valid.add("fox");
        valid.add("jumped");
        valid.add("over");
        valid.add("fence");
        assertEquals(valid, QueryUtils.extractKeywordsFromFileName("THE 12 foot quick\\brown]\nfox[jumped [] # over-the _brown*fence_"));
        
        valid.clear();
        valid.add("sam");
        valid.add("born");
        assertEquals(valid, QueryUtils.extractKeywordsFromFileName("sam, 2.1.81. born."));
        
        valid.clear();
        valid.add("file");
        assertEquals(valid, QueryUtils.extractKeywordsFromFileName("a file.extension"));

        // test for allowNumers == true
        valid.clear();
        valid.add("11");
        valid.add("test");
        valid.add("13");
        valid.add("pg");
        // everything behind the last dot is removed by rip extension
        valid.add("3");
        assertEquals(valid, QueryUtils.extractKeywords(QueryUtils.ripExtension("11 test pg-13 3.1415947"), true));
    }

    private void containsAll(String match, String query) {
        Collection matchSet = Arrays.asList(match.split(" "));
        
        String[] array = query.split(" ");
        List set = new LinkedList();
        for(int i = 0; i < array.length; i++)
            set.add(array[i]);
        for(Iterator i = matchSet.iterator(); i.hasNext(); ) {
            String next = (String)i.next();
            assertContains(set, next);
            set.remove(next);
        }
        assertEquals(set.toString(), 0, set.size());
    }
    
    public void testIsDelimiter() {
        for (char c : QueryUtils.DELIMITERS.toCharArray()) {
            assertTrue(QueryUtils.isDelimiter(c));
        }
        for (char c : "203fklj\t|03fklj?=".toCharArray()) {
            assertFalse("considered a delimiter: " + c, QueryUtils.isDelimiter(c));
        }
    }
}
