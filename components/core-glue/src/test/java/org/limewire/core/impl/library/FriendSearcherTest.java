package org.limewire.core.impl.library;

import java.util.ArrayList;
import java.util.Collection;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.FriendSearchListener;
import org.limewire.util.BaseTestCase;

public class FriendSearcherTest extends BaseTestCase {

    public FriendSearcherTest(String name) {
        super(name);
    }

    public void testDoSearch() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final FriendLibraries friendLibraries = context.mock(FriendLibraries.class);

        FriendSearcher friendSearcher = new FriendSearcher(friendLibraries);

        final SearchDetails searchDetails = context.mock(SearchDetails.class);
        final FriendSearchListener friendSearchListener = context.mock(FriendSearchListener.class);

        final Collection<SearchResult> results = new ArrayList<SearchResult>();

        context.checking(new Expectations() {
            {
                one(friendLibraries).getMatchingItems(searchDetails);
                will(returnValue(results));
                one(friendSearchListener).handleFriendResults(results);

            }
        });

        friendSearcher.doSearch(searchDetails, friendSearchListener);

        context.assertIsSatisfied();
    }
}
