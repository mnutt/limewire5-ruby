package org.limewire.core.impl.library;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.util.BaseTestCase;

/**
 * Test set for FriendLibraryPropertyAutoCompleter, an AutoCompleter based on dataset
 *  contained in a FriendLibraries instance.
 */
public class FriendLibraryPropertyAutoCompleterTest extends BaseTestCase {

    public FriendLibraryPropertyAutoCompleterTest(String name) {
        super(name);
    }
    
    /**
     * These methods should be unsupported with this AutoCompleter type
     *  because it delegates to unmanaged FriendLibraries to provide the dataset. 
     */
    public void testAddRemoveAndClearEntry() {
        FriendLibraryPropertyAutoCompleter autoCompleter 
            = new FriendLibraryPropertyAutoCompleter(null,null,null);
        
        try {
            autoCompleter.addEntry("fail!");
            fail("addEntry() should not be a supported method!");
        } 
        catch (UnsupportedOperationException e) {
            // Expected
        }
        
        try {
            autoCompleter.removeEntry("also fail!");
            fail("removeEntry() should not be a supported method!");
        } 
        catch (UnsupportedOperationException e) {
            // Expected
        }
        
        try {
            autoCompleter.clear();
            fail("clear() should not be a supported method!");
        } 
        catch (UnsupportedOperationException e) {
            // Expected
        }
    }
    
    /**
     * Test lookup when matches are and are not returned.
     */
    public void testLookup() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final FriendLibraries libraries = context.mock(FriendLibraries.class);
        final String searchTerm = "lunedì";
        final Collection<String> results = new HashSet<String>();
        
        context.checking(new Expectations() {{
            exactly(2).of(libraries).getSuggestions(searchTerm, 
                    SearchCategory.OTHER, FilePropertyKey.LOCATION);
            will(returnValue(results));
        }});
        
        AutoCompleteDictionary dictionary
            = new FriendLibraryPropertyAutoCompleter(libraries,
                    SearchCategory.OTHER, FilePropertyKey.LOCATION);
        
        // No results
        assertNull(dictionary.lookup(searchTerm));
        
        // A result
        results.add(searchTerm);
        assertEquals(searchTerm, dictionary.lookup(searchTerm));
        
        context.assertIsSatisfied();
    }
    
    /**
     * Test the getPrefixedBy function and ensure it returns the right list and
     *  at least an equivalent one (if not the same which it does right now).
     */
    public void testGetPrefixedBy() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final FriendLibraries libraries = context.mock(FriendLibraries.class);
        final String searchTerm = "lunedì";
        final Collection<String> results = new HashSet<String>();
        results.add("a");
        results.add("b");
        results.add("c!");
        
        context.checking(new Expectations() {{
            exactly(1).of(libraries).getSuggestions(searchTerm, 
                    SearchCategory.VIDEO, FilePropertyKey.PLATFORM);
            will(returnValue(results));
        }});
        
        AutoCompleteDictionary dictionary
            = new FriendLibraryPropertyAutoCompleter(libraries,
                    SearchCategory.VIDEO, FilePropertyKey.PLATFORM);
        
        Collection<String> collection = dictionary.getPrefixedBy(searchTerm);
        assertNotNull(collection);
        assertContains(collection, "c!");
        assertEquals(3, collection.size());
        
        context.assertIsSatisfied();
    }
    
    /**
     * Ensures the iterator function takes its iterator from list of empty ("") prefixed results.
     * 
     * <P>Essentially this means an iterator over the entire data set.
     */
    public void testIterator() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final FriendLibraries libraries = context.mock(FriendLibraries.class);
        final Collection<String> results = new HashSet<String>();
        results.add("hello");
        
        context.checking(new Expectations() {{
            exactly(1).of(libraries).getSuggestions("", 
                    SearchCategory.VIDEO, FilePropertyKey.PLATFORM);
            will(returnValue(results));
        }});
        
        AutoCompleteDictionary dictionary
            = new FriendLibraryPropertyAutoCompleter(libraries,
                    SearchCategory.VIDEO, FilePropertyKey.PLATFORM);
        
        Iterator<String> iterator = dictionary.iterator();
        assertNotNull(iterator);
        assertEquals(iterator.next(), results.iterator().next());
        
        context.assertIsSatisfied();
    }

}
