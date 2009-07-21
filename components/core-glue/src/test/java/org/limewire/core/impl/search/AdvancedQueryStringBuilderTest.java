package org.limewire.core.impl.search;

import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.util.BaseTestCase;

/**
 * Tests the functionality included in {@link AdvancedQueryStringBuilder}.
 */
public class AdvancedQueryStringBuilderTest extends BaseTestCase {
    public AdvancedQueryStringBuilderTest(String name) {
        super(name);
    }
   
    /**
     * Tests {@link AdvancedQueryStringBuilder#createSimpleCompositeQuery(Map)}.
     */
    public void testCreateSimpleCompositeQuery() {
        AdvancedQueryStringBuilder compositeBuilder = new AdvancedQueryStringBuilder(null);
        
        Map<FilePropertyKey, String> searchMap = new HashMap<FilePropertyKey, String>();
        String composite = compositeBuilder.createSimpleCompositeQuery(searchMap);
        assertNotNull(composite);
        assertEquals("", composite);
        
        searchMap.put(FilePropertyKey.GENRE, "ab");
        composite = compositeBuilder.createSimpleCompositeQuery(searchMap);
        assertNotNull(composite);
        assertEquals("ab", composite);
        
        searchMap.put(FilePropertyKey.AUTHOR, "bc");
        composite = compositeBuilder.createSimpleCompositeQuery(searchMap);
        assertTrue("ab bc".equals(composite) || "bc ab".equals(composite));   
    }
}
