package org.limewire.ui.swing.search;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.util.FilePropertyKeyUtils;
import org.limewire.ui.swing.util.Translator;
import org.limewire.util.BaseTestCase;

/**
 * Tests for {@link KeywordAssistedSearchBuilder}.
 */
public class AdvancedSearchBuilderTest extends BaseTestCase {
    
    public AdvancedSearchBuilderTest(String name) {
        super(name);
    }

    /**
     * Specifically test translation of the key separator works or almost all other tests
     *  in this package will fail.
     */
    public void testGetKeySeparator() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
     
        final Translator translator = context.mock(Translator.class);
        
        final KeywordAssistedSearchBuilder searchBuilder = new KeywordAssistedSearchBuilder(translator); 
     
        context.checking(new Expectations() {{
            allowing(translator).translateWithComment(with(any(String.class)), with(equal(":")));
            will(returnValue("-"));
        }});
        
        assertEquals("-", searchBuilder.getTranslatedKeySeprator());
    }

    
    /**
     * Tests {@link KeywordAssistedSearchBuilder#createCompositeQuery(Map)}.
     */
    public void testCreateCompositeQueryBasic() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
     
        final Translator translator = context.mock(Translator.class);
        
        final KeywordAssistedSearchBuilder searchBuilder = new KeywordAssistedSearchBuilder(translator); 
     
        context.checking(new Expectations() {{
            allowing(translator).translate(
                    FilePropertyKeyUtils.getUntraslatedDisplayName(FilePropertyKey.YEAR, SearchCategory.DOCUMENT));
            will(returnValue("hello"));
            
            allowing(translator).translate(
                    FilePropertyKeyUtils.getUntraslatedDisplayName(FilePropertyKey.AUTHOR, SearchCategory.DOCUMENT));
            will(returnValue("bye"));
            
            allowing(translator).translateWithComment(with(any(String.class)), with(equal(":")));
            will(returnValue(":"));
        }});
        
        assertEquals("", searchBuilder.createCompositeQuery(new HashMap<FilePropertyKey, String>(), SearchCategory.DOCUMENT));
        
        Map<FilePropertyKey,String> map = new HashMap<FilePropertyKey,String>();
        map.put(FilePropertyKey.YEAR, "1982");
        
        String query = searchBuilder.createCompositeQuery(map, SearchCategory.DOCUMENT);
        
        assertTrue(query.startsWith("hello"));
        assertTrue(query.endsWith("1982"));
        char seperator = query.charAt(5);
        assertEquals(':', seperator);
        
        map.put(FilePropertyKey.AUTHOR, "lpsadsac");
        
        query = searchBuilder.createCompositeQuery(map, SearchCategory.ALL);
        
        // Verify there is some sanity when constructing key/value list
        int firstKeyLen = query.indexOf(seperator);
        String firstKey = query.substring(0, firstKeyLen);
        if (firstKey.equals("bye")) {
            assertTrue(query.indexOf("lpsadsac") > 0);
            int secondKeyLen = query.indexOf(seperator, firstKeyLen+1);
            assertEquals("1982", query.substring(secondKeyLen+1));
        } else if (firstKey.equals("hello")) {
            assertTrue(query.indexOf("1982") > 0);
            int secondKeyLen = query.indexOf(seperator, firstKeyLen+1);
            assertEquals("lpsadsac", query.substring(secondKeyLen+1));
        } else {
            fail("Keys not found in composite search string");
        }
    }

    /**
     * Tests creations of a composite query String with a longer than one character
     *  key/value separator. 
     */
    public void testCreateCompositeQueryWithNewSeparator() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
     
        final Translator translator = context.mock(Translator.class);
        
        final KeywordAssistedSearchBuilder searchBuilder = new KeywordAssistedSearchBuilder(translator); 
     
        context.checking(new Expectations() {{
            allowing(translator).translate(
                    FilePropertyKeyUtils.getUntraslatedDisplayName(FilePropertyKey.YEAR, SearchCategory.DOCUMENT));
            will(returnValue("hello"));
            
            allowing(translator).translateWithComment(with(any(String.class)), with(equal(":")));
            will(returnValue("---"));
        }});
       
        Map<FilePropertyKey,String> map = new HashMap<FilePropertyKey,String>();
        map.put(FilePropertyKey.YEAR, "1982");
        
        String query = searchBuilder.createCompositeQuery(map, SearchCategory.DOCUMENT);
        
        assertTrue(query.startsWith("hello"));
        assertTrue(query.endsWith("1982"));
        String seperator = query.substring(5,8);
        assertEquals("---", seperator);
    }
    
    
    /**
     * Tests creations of a composite query String with right to left text.
     * 
     * <p> In this case Arabic is used.
     */
 /*   public void testCreateCompositeQueryWithRightToLeft() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
     
        final Translator translator = context.mock(Translator.class);
        
        final AdvancedSearchBuilder searchBuilder = new AdvancedSearchBuilder(translator); 
     
        context.checking(new Expectations() {{
            allowing(translator).translate(
                    FilePropertyKeyUtils.getUntraslatedDisplayName(FilePropertyKey.YEAR, SearchCategory.ALL));
            will(returnValue("العربية"));
            
            allowing(translator).translateWithComment(with(any(String.class)), with(equal(":")));
            will(returnValue("ل"));
        }});
        
        Map<FilePropertyKey,String> map = new HashMap<FilePropertyKey,String>();
        map.put(FilePropertyKey.YEAR, "لا أتكلم العربية");
        
        String query = searchBuilder.createCompositeQuery(map, SearchCategory.ALL);
        
        assertTrue(query.startsWith("العربية"));
        assertTrue(query.endsWith("لا أتكلم العربية"));
        char seperator = query.charAt(7);
        assertEquals('ل', seperator);
    }
    */
    
    /**
     * Tests creations of a composite query String with right to left and left to right text
     * mixed.
     * 
     * <p> In this case Arabic is used with the Roman colon.
     */
  /*  public void testCreateCompositeQueryWithRightToLeftAndLeftToRightMix() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
     
        final Translator translator = context.mock(Translator.class);
        
        final AdvancedSearchBuilder searchBuilder = new AdvancedSearchBuilder(translator); 
     
        context.checking(new Expectations() {{
            allowing(translator).translate(
                    FilePropertyKeyUtils.getUntraslatedDisplayName(FilePropertyKey.YEAR, SearchCategory.ALL));
            will(returnValue("العربية"));
            
            allowing(translator).translateWithComment(with(any(String.class)), with(equal(":")));
            will(returnValue(":"));
        }});
        
        Map<FilePropertyKey,String> map = new HashMap<FilePropertyKey,String>();
        map.put(FilePropertyKey.YEAR, "لا أتكلم العربية");
        
        String query = searchBuilder.createCompositeQuery(map, SearchCategory.ALL);
        
        assertTrue(query.startsWith("العربية"));
        assertTrue(query.endsWith("لا أتكلم العربية"));
        char seperator = query.charAt(7);
        assertEquals(':', seperator);
    }
    */
    
    /**
     * Test the entirety of {@link KeywordAssistedSearchBuilder#createAdvancedSearch(Map, SearchCategory)}. 
     */
    public void testCreateAdvancedSearch() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
     
        final Translator translator = context.mock(Translator.class);
        
        final KeywordAssistedSearchBuilder searchBuilder = new KeywordAssistedSearchBuilder(translator); 
     
        context.checking(new Expectations() {{
            allowing(translator).translate(
                    FilePropertyKeyUtils.getUntraslatedDisplayName(FilePropertyKey.YEAR, SearchCategory.DOCUMENT));
            will(returnValue("key"));
            
            allowing(translator).translateWithComment(with(any(String.class)), with(equal(":")));
            will(returnValue(":"));
        }});
        
        Map<FilePropertyKey,String> map = new HashMap<FilePropertyKey,String>();
        map.put(FilePropertyKey.YEAR, "value");
        
        SearchInfo search = searchBuilder.createAdvancedSearch(map, SearchCategory.AUDIO);
        
        assertEquals(SearchCategory.AUDIO, search.getSearchCategory());
        assertTrue(search.getTitle().indexOf("key") > -1);
    }
    
    /**
     * Test {@link KeywordAssistedSearchBuilder#attemptToCreateAdvancedSearch(String, SearchCategory)
     */
    public void testAttemptToCreateAdvancedSearchSingleTuple() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
     
        final Translator mockedTranslator = context.mock(Translator.class);
        final Translator translator = new MockableTranslator(mockedTranslator);        
        
        final KeywordAssistedSearchBuilder searchBuilder = new KeywordAssistedSearchBuilder(translator); 
     
        context.checking(new Expectations() {{
            allowing(mockedTranslator).isCurrentLanguageEnglish();
            will(returnValue(false));
            
            allowing(mockedTranslator).translate(
                    FilePropertyKeyUtils.getUntraslatedDisplayName(FilePropertyKey.YEAR, SearchCategory.DOCUMENT));
            will(returnValue("key"));

            allowing(mockedTranslator).translate(
                    FilePropertyKeyUtils.getUntraslatedDisplayName(FilePropertyKey.YEAR, SearchCategory.PROGRAM));
            will(returnValue("key"));
            
            allowing(mockedTranslator).translate(
                    FilePropertyKeyUtils.getUntraslatedDisplayName(FilePropertyKey.YEAR, SearchCategory.AUDIO));
            will(returnValue("key"));
            
            // Do not match any other translations
            allowing(mockedTranslator).translate(with(any(String.class)));
            will(returnValue("@$%@$#@SDFSDF@#%@#$DFD"));
            
            allowing(mockedTranslator).translateWithComment(with(any(String.class)), with(equal(":")));
            will(returnValue(":"));
        }});
        
        SearchInfo search1 = searchBuilder.attemptToCreateAdvancedSearch("key:value", SearchCategory.DOCUMENT);
        assertNotNull(search1);
        assertEquals(SearchCategory.DOCUMENT, search1.getSearchCategory());
        assertEquals("value", search1.getAdvancedDetails().get(FilePropertyKey.YEAR));
        
        SearchInfo search2 = searchBuilder.attemptToCreateAdvancedSearch("  key:   value  ", SearchCategory.PROGRAM);
        assertNotNull(search2);
        assertEquals(SearchCategory.PROGRAM, search2.getSearchCategory());
        assertEquals("value", search2.getAdvancedDetails().get(FilePropertyKey.YEAR));

        SearchInfo search3 = searchBuilder.attemptToCreateAdvancedSearch("key:   aartist:value:", SearchCategory.AUDIO);
        assertNotNull(search3);
        assertEquals(SearchCategory.AUDIO, search3.getSearchCategory());
        assertEquals("aartist:value:", search3.getAdvancedDetails().get(FilePropertyKey.YEAR));
        
        SearchInfo search4 = searchBuilder.attemptToCreateAdvancedSearch("  kEY:   value  ", SearchCategory.PROGRAM);
        assertNotNull(search4);
        assertEquals(SearchCategory.PROGRAM, search4.getSearchCategory());
        assertEquals("value", search4.getAdvancedDetails().get(FilePropertyKey.YEAR));
        
        SearchInfo search5 = searchBuilder.attemptToCreateAdvancedSearch("yEaR:   aartist:value:", SearchCategory.AUDIO);
        assertNotNull(search5);
        assertEquals(SearchCategory.AUDIO, search5.getSearchCategory());
        assertEquals("aartist:value:", search5.getAdvancedDetails().get(FilePropertyKey.YEAR));

        SearchInfo search6 = searchBuilder.attemptToCreateAdvancedSearch("GenrE:: asddsd sddafas: asdsad:: genre: classical", SearchCategory.AUDIO);
        assertNotNull(search6);
        assertEquals(SearchCategory.AUDIO, search6.getSearchCategory());
        assertEquals(": asddsd sddafas: asdsad:: genre: classical", search6.getAdvancedDetails().get(FilePropertyKey.GENRE));
        
        context.assertIsSatisfied();        
    }
    
    /**
     * Test a variety of cases where an advanced search can not and should not be parsed from a query String.
     */
    public void testAttemptToCreateAdvancedSearchImpossible() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
     
        final Translator mockedTranslator1 = context.mock(Translator.class);
        final Translator translator1 = new MockableTranslator(mockedTranslator1);        
        
        final Translator mockedTranslator2 = context.mock(Translator.class);
        final Translator translator2 = new MockableTranslator(mockedTranslator2);
        
        final KeywordAssistedSearchBuilder searchBuilder1 = new KeywordAssistedSearchBuilder(translator1);
        final KeywordAssistedSearchBuilder searchBuilder2 = new KeywordAssistedSearchBuilder(translator2);
     
        context.checking(new Expectations() {{
            allowing(mockedTranslator1).isCurrentLanguageEnglish();
            will(returnValue(false));
            allowing(mockedTranslator1).translate(
                    FilePropertyKeyUtils.getUntraslatedDisplayName(FilePropertyKey.YEAR, SearchCategory.AUDIO));
            will(returnValue("key"));
            // Do not match any other translations
            allowing(mockedTranslator1).translate(with(any(String.class)));
            will(returnValue("@$%@$#@SDFSDF@#%@#$DFD"));
            allowing(mockedTranslator1).translateWithComment(with(any(String.class)), with(equal(":")));
            will(returnValue(":"));
            
            allowing(mockedTranslator2).isCurrentLanguageEnglish();
            will(returnValue(false));
            allowing(mockedTranslator2).translate(
                    FilePropertyKeyUtils.getUntraslatedDisplayName(FilePropertyKey.YEAR, SearchCategory.AUDIO));
            will(returnValue("key"));
            // Do not match any other translations
            allowing(mockedTranslator2).translate(with(any(String.class)));
            will(returnValue("@$%@$#@SDFSDF@#%@#$DFD"));
            allowing(mockedTranslator2).translateWithComment(with(any(String.class)), with(equal(":")));
            will(returnValue("-"));
        }});
        
        SearchInfo search1 = searchBuilder1.attemptToCreateAdvancedSearch("not a key: not a value name: impossible", 
                SearchCategory.AUDIO);
        assertNull(search1);
        
        SearchInfo search2 = searchBuilder1.attemptToCreateAdvancedSearch("not a key, impossible", 
                SearchCategory.AUDIO);
        assertNull(search2);
        
        SearchInfo search3 = searchBuilder1.attemptToCreateAdvancedSearch("artist: is not a key in all", 
                SearchCategory.DOCUMENT);
        assertNull(search3);
        
        SearchInfo search4 = searchBuilder1.attemptToCreateAdvancedSearch("", 
                SearchCategory.AUDIO);
        assertNull(search4);
        
        SearchInfo search5 = searchBuilder1.attemptToCreateAdvancedSearch("    ", 
                SearchCategory.AUDIO);
        assertNull(search5);
        
        SearchInfo search6 = searchBuilder1.attemptToCreateAdvancedSearch("   name-insert my name here  ", 
                SearchCategory.AUDIO);
        assertNull(search6);
        
        SearchInfo search7 = searchBuilder1.attemptToCreateAdvancedSearch("   name-insert my name here  ", 
                SearchCategory.AUDIO);
        assertNull(search7);
        
        SearchInfo search8 = searchBuilder2.attemptToCreateAdvancedSearch("   artist-insert my name here  ", 
                SearchCategory.AUDIO);
        assertNull(search8);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests {@link KeywordAssistedSearchBuilder#attemptToCreateAdvancedSearch(String, SearchCategory)} with
     *  queries including multi character separators and multi token keys.
     */
    public void testAttemptToCreateAdvancedSearchWithComplexSeparatorsAndKeys() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
     
        final Translator mockedTranslator1 = context.mock(Translator.class);
        final Translator translator1 = new MockableTranslator(mockedTranslator1);        
        
        final KeywordAssistedSearchBuilder searchBuilder1 = new KeywordAssistedSearchBuilder(translator1);

     
        context.checking(new Expectations() {{
            allowing(mockedTranslator1).isCurrentLanguageEnglish();
            will(returnValue(false));
            allowing(mockedTranslator1).translate(
                    FilePropertyKeyUtils.getUntraslatedDisplayName(FilePropertyKey.YEAR, SearchCategory.AUDIO));
            will(returnValue("key hat cat rANDo-mAt:"));
            // Do not match any other translations
            allowing(mockedTranslator1).translate(with(any(String.class)));
            will(returnValue("@$%@$#@SDFSDF@#%@#$DFD"));
            allowing(mockedTranslator1).translateWithComment(with(any(String.class)), with(equal(":")));
            will(returnValue(":: a ::"));
            
        }});
        
        SearchInfo search1 = searchBuilder1.attemptToCreateAdvancedSearch("key hat cat rando-mat::: a ::hello", 
                SearchCategory.AUDIO);
        assertNotNull(search1);
        assertEquals(SearchCategory.AUDIO, search1.getSearchCategory());
        assertEquals("hello", search1.getAdvancedDetails().get(FilePropertyKey.YEAR));
            
        context.assertIsSatisfied();
    }
    
    /**
     * Test parsing advanced quieries with mutiple key/value pairs.
     */
    public void testAttemptToCreateAdvancedSearchMultiKeyQuery() {
     
       final KeywordAssistedSearchBuilder searchBuilder1 = new KeywordAssistedSearchBuilder(new Translator() {
           @Override
           public boolean isCurrentLanguageEnglish() {
               return true;
           }
       });

     
        
       SearchInfo search1 = searchBuilder1.attemptToCreateAdvancedSearch("genre:::::a title:cawr pawr artist:flabbats ", 
                SearchCategory.AUDIO);
       assertNotNull(search1);
       assertEquals(SearchCategory.AUDIO, search1.getSearchCategory());
       assertEquals("::::a", search1.getAdvancedDetails().get(FilePropertyKey.GENRE));
       assertEquals("cawr pawr", search1.getAdvancedDetails().get(FilePropertyKey.TITLE));
       assertEquals("flabbats", search1.getAdvancedDetails().get(FilePropertyKey.AUTHOR));
            
    }
    
    /**
     * Helper class to allow a partial overlay of {@link Translator}.
     */
    private class MockableTranslator extends Translator {
        private final Translator baseTranslator;
        private Locale caseLocale = Locale.US;
        
        public MockableTranslator(Translator baseTranslator) {
            this.baseTranslator = baseTranslator;
        }
        public void setCaseLocale(Locale locale) {
            caseLocale = locale;
        }
        
        @Override
        public String translate(String text) {
            return baseTranslator.translate(text);
        }
        @Override
        public String translateWithComment(String comment, String text) {
            return baseTranslator.translateWithComment(comment, text);
        }
        
        @Override
        public boolean isCurrentLanguageEnglish() {
            return baseTranslator.isCurrentLanguageEnglish();
        }
        @Override
        public String toLowerCaseEnglish(String text) {
            return text.toLowerCase(Locale.US);
        }
        @Override
        public String toLowerCaseCurrentLocale(String text) {
            return text.toLowerCase(caseLocale);
        }
    }
}
