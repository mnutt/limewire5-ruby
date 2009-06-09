package org.limewire.core.impl.search;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.settings.PromotionSettings;
import org.limewire.promotion.containers.PromotionMessageContainer;
import org.limewire.util.BaseTestCase;

/**
 * Tests the static methods provided by {@link SearchUrlUtils}.
 */
public class SearchUrlUtilsTest extends BaseTestCase {

    public SearchUrlUtilsTest(String name) {
        super(name);
    }

    /**
     * Tests {@link SearchUrlUtils#createPromotionUrl(PromotionMessageContainer, long)}.
     */
    public void testCreatePromotionUrl() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final String redirect = "http://blahh.om";
        final String url = "asdsafasf.cob";

        final PromotionMessageContainer container = context.mock(PromotionMessageContainer.class);
        
        context.checking(new Expectations() {{
            allowing(container).getURL();
            will(returnValue(url));
            
            allowing(container);
        }});
             
        PromotionSettings.REDIRECT_URL.set(redirect);
        
        assertNotNull(SearchUrlUtils.createPromotionUrl(container, 0));
        
        // Test consistency 
        assertEquals(SearchUrlUtils.createPromotionUrl(container, 1000), 
                SearchUrlUtils.createPromotionUrl(container, 1000));
        
        assertTrue(SearchUrlUtils.createPromotionUrl(container, 1000).startsWith(redirect));
        assertTrue(SearchUrlUtils.createPromotionUrl(container, 1000).indexOf(url) > 0);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests {@link SearchUrlUtils#stripUrl(String)}.
     */
    public void testStripUrl() {
        assertEquals("asdsadsda.com", SearchUrlUtils.stripUrl("http://asdsadsda.com//asdsad/sad"));
        assertEquals("asfdsaddsasda.cffom", SearchUrlUtils.stripUrl("htasdsatp://asfdsaddsasda.cffom////"));
        assertEquals("a.c", SearchUrlUtils.stripUrl("htasdsatp://a.c"));
        assertEquals("a.c", SearchUrlUtils.stripUrl("a.c/asdsadsadsadsad"));
        assertEquals("asfd.s.a.d.d.sasda.c", SearchUrlUtils.stripUrl("ftp://asfd.s.a.d.d.sasda.c/a/.d/f/?f"));
        assertEquals("a b.c", SearchUrlUtils.stripUrl("hta&sds-\\atp://a b.c////"));
    }
}
