package org.limewire.core.impl.library;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.util.BaseTestCase;

public class FriendLibraryAutoCompleterTest extends BaseTestCase {

    public FriendLibraryAutoCompleterTest(String name) {
        super(name);
    }

    public void testAddEntry() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        final SearchCategory searchCategory = SearchCategory.AUDIO;
        final FriendLibraries friendLibraries = context.mock(FriendLibraries.class);

        FriendLibraryAutoCompleter friendLibraryAutoCompleter = new FriendLibraryAutoCompleter(
                friendLibraries, searchCategory);
        try {
            friendLibraryAutoCompleter.addEntry("test");
            fail("Should not be able to add entries to this auto completer");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        context.assertIsSatisfied();
    }

    public void testClear() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final SearchCategory searchCategory = SearchCategory.AUDIO;
        final FriendLibraries friendLibraries = context.mock(FriendLibraries.class);

        FriendLibraryAutoCompleter friendLibraryAutoCompleter = new FriendLibraryAutoCompleter(
                friendLibraries, searchCategory);

        try {
            friendLibraryAutoCompleter.clear();
            fail("Should not be able to clear this auto completer");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        context.assertIsSatisfied();
    }

    public void testGetByPrefix() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final String testPrefix1 = "testPrefix1";

        final String value1 = "1";
        final String value2 = "2";
        final String value3 = "3";

        final SearchCategory searchCategory = SearchCategory.AUDIO;
        final FriendLibraries friendLibraries = context.mock(FriendLibraries.class);
        context.checking(new Expectations() {
            {
                one(friendLibraries).getSuggestions(testPrefix1, searchCategory);
                will(returnValue(Arrays.asList(value1, value2, value3)));
            }
        });

        FriendLibraryAutoCompleter friendLibraryAutoCompleter = new FriendLibraryAutoCompleter(
                friendLibraries, searchCategory);

        Collection<String> result = friendLibraryAutoCompleter.getPrefixedBy(testPrefix1);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertContains(result, value1);
        assertContains(result, value2);
        assertContains(result, value3);
        context.assertIsSatisfied();
    }

    public void testLookup() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final String testPrefix1 = "testPrefix1";

        final String value1 = "1";
        final String value2 = "2";

        final SearchCategory searchCategory = SearchCategory.AUDIO;
        final FriendLibraries friendLibraries = context.mock(FriendLibraries.class);
        context.checking(new Expectations() {
            {
                one(friendLibraries).getSuggestions(testPrefix1, searchCategory);
                will(returnValue(Arrays.asList(value2, value1)));
            }
        });

        FriendLibraryAutoCompleter friendLibraryAutoCompleter = new FriendLibraryAutoCompleter(
                friendLibraries, searchCategory);

        String lookup = friendLibraryAutoCompleter.lookup(testPrefix1);
        assertNotNull(lookup);

        assertEquals(value2, lookup);
        context.assertIsSatisfied();
    }

    public void testInterator() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final String testPrefix1 = "";

        final String value1 = "1";
        final String value2 = "2";

        final SearchCategory searchCategory = SearchCategory.AUDIO;
        final FriendLibraries friendLibraries = context.mock(FriendLibraries.class);
        context.checking(new Expectations() {
            {
                one(friendLibraries).getSuggestions(testPrefix1, searchCategory);
                will(returnValue(Arrays.asList(value2, value1)));
            }
        });

        FriendLibraryAutoCompleter friendLibraryAutoCompleter = new FriendLibraryAutoCompleter(
                friendLibraries, searchCategory);

        Iterator<String> iterator = friendLibraryAutoCompleter.iterator();
        assertNotNull(iterator);

        assertEquals(value2, iterator.next());
        assertEquals(value1, iterator.next());
        assertFalse(iterator.hasNext());
        context.assertIsSatisfied();
    }
}
