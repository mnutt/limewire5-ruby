package org.limewire.core.impl.library;

import java.util.Collection;
import java.util.HashSet;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.util.BaseTestCase;

/** 
 * Ensures The FriendAutoCompleterFactoryImpl is able to generate working
 *  FriendAutoCompleter instances.
 */
public class FriendAutoCompleterFactoryImplTest extends BaseTestCase {

    public FriendAutoCompleterFactoryImplTest(String name) {
        super(name);
    }

    /**
     * Test the dictionary create method without a FileProperyKey.
     */
    public void testGetDictionary() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final FriendLibraries libraries = context.mock(FriendLibraries.class);
        final String searchTerm = "lunedì";
        final Collection<String> results = new HashSet<String>();
        results.add(searchTerm);
        
        FriendAutoCompleterFactoryImpl factory = new FriendAutoCompleterFactoryImpl(libraries);
        
        context.checking(new Expectations() {{
            exactly(1).of(libraries).getSuggestions(searchTerm, SearchCategory.OTHER);
            will(returnValue(results));
        }});
        
        AutoCompleteDictionary dictionary = factory.getDictionary(SearchCategory.OTHER);
        
        assertNotNull(dictionary);
        assertEquals(searchTerm, dictionary.lookup(searchTerm));
        
        context.assertIsSatisfied();
    }
    
    /**
     * Test the dictionary create method with a FileProperyKey.
     */
    public void testGetDictionaryWithFileProp() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final FriendLibraries libraries = context.mock(FriendLibraries.class);
        final String searchTerm = "alla mattina";
        final String foundTerm = "adasdsad";
        final Collection<String> results = new HashSet<String>();
        results.add(foundTerm);
        results.add(searchTerm + ", blah!");
        
        FriendAutoCompleterFactoryImpl factory = new FriendAutoCompleterFactoryImpl(libraries);
        
        context.checking(new Expectations() {{
            allowing(libraries).getSuggestions(searchTerm, SearchCategory.IMAGE, FilePropertyKey.COMPANY);
            will(returnValue(results));
        }});
        
        AutoCompleteDictionary dictionary = factory.getDictionary(SearchCategory.IMAGE, FilePropertyKey.COMPANY);
        
        assertNotNull(dictionary);
        assertTrue(dictionary.getPrefixedBy(searchTerm).contains(foundTerm));
        assertEquals(2, dictionary.getPrefixedBy(searchTerm).size());
        
        context.assertIsSatisfied();
    }
}
