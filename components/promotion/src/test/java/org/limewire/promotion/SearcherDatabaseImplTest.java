package org.limewire.promotion;

import java.util.Date;
import java.util.List;

import junit.framework.Test;

import org.limewire.promotion.containers.PromotionMessageContainer;
import org.limewire.util.BaseTestCase;

public class SearcherDatabaseImplTest extends BaseTestCase {
    public SearcherDatabaseImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SearcherDatabaseImplTest.class);
    }

    public void testQuery() throws Exception {
        SearcherDatabaseImpl searcherDatabase = new SearcherDatabaseImpl(new KeywordUtilImpl(),
                null, null, null);
        searcherDatabase.init();
        searcherDatabase.clear();

        PromotionMessageContainer promo = new PromotionMessageContainer();
        promo.setDescription("I'm a description");
        promo.setUniqueID(System.currentTimeMillis());
        promo.setKeywords("keyword1\tkeyword2\tjason");
        promo.setURL("http://limewire.com/");
        promo.setValidStart(new Date(0));
        promo.setValidEnd(new Date(Long.MAX_VALUE));

        searcherDatabase.ingest(promo, "foo" + System.currentTimeMillis());

        List<SearcherDatabase.QueryResult> results = searcherDatabase.query("jaSON");
        assertEquals(1, results.size());
        assertGreaterThan("Expected uniqueID to be > 1", 1, results.get(0)
                .getPromotionMessageContainer().getUniqueID());
        assertEquals("http://limewire.com/", results.get(0).getPromotionMessageContainer().getURL());

        results = searcherDatabase.query("jaSON2");
        assertEquals(0, results.size());
    }
}
