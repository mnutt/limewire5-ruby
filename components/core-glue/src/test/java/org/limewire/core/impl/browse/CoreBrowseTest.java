package org.limewire.core.impl.browse;

import org.hamcrest.Description;
import org.hamcrest.core.IsAnything;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.QueryReplyListener;
import org.limewire.core.impl.search.QueryReplyListenerList;
import org.limewire.io.GUID;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.SearchServices;

public class CoreBrowseTest extends BaseTestCase {

    public CoreBrowseTest(String name) {
        super(name);
    }

    /**
     * Tests that the supplied browse listener is populated with search results
     * as handleBrowseResult is called on the interal BrowseListeer of the
     * CoreBrowse object.
     */
    public void testBasicBrowseListenerPopulation() {
        Mockery context = new Mockery();
        final FriendPresence friendPresence = context.mock(FriendPresence.class);
        final SearchServices searchServices = context.mock(SearchServices.class);
        final QueryReplyListenerList queryReplyListenerList = context
                .mock(QueryReplyListenerList.class);

        final FindInternalListenerAction findInternalListenerAction = new FindInternalListenerAction();
        final byte[] searchGuid = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                16 };

        context.checking(new Expectations() {
            {
                one(queryReplyListenerList).addQueryReplyListener(with(new IsAnything<byte[]>()),
                        with(new IsAnything<QueryReplyListener>()));
                exactly(2).of(queryReplyListenerList).removeQueryReplyListener(
                        with(new IsAnything<byte[]>()), with(new IsAnything<QueryReplyListener>()));
                one(searchServices).newQueryGUID();
                will(returnValue(searchGuid));
                one(searchServices).doAsynchronousBrowseHost(
                        with(new IsAnything<FriendPresence>()), with(new IsAnything<GUID>()),
                        with(new IsAnything<BrowseListener>()));
                will(findInternalListenerAction);
                exactly(2).of(searchServices).stopQuery(new GUID(searchGuid));
            }
        });
        CoreBrowse coreBrowse = new CoreBrowse(friendPresence, searchServices,
                queryReplyListenerList);

        TestBrowseListener testBrowseListener = new TestBrowseListener();
        coreBrowse.start(testBrowseListener);

        BrowseListener innerBrowseListener = findInternalListenerAction.getBrowseListener();
        assertNotNull(innerBrowseListener);

        SearchResult searchResult1 = context.mock(SearchResult.class);

        innerBrowseListener.handleBrowseResult(searchResult1);
        assertEquals(searchResult1, testBrowseListener.searchResult);
        testBrowseListener.reset();

        SearchResult searchResult2 = context.mock(SearchResult.class);
        innerBrowseListener.handleBrowseResult(searchResult2);
        assertEquals(searchResult2, testBrowseListener.searchResult);

        testBrowseListener.reset();

        innerBrowseListener.browseFinished(false);
        assertFalse(testBrowseListener.success);

        innerBrowseListener.browseFinished(true);
        assertTrue(testBrowseListener.success);

        context.assertIsSatisfied();
    }

    /**
     * Finds the internal BrowseListener to enable testing of internal calls.
     */
    private class FindInternalListenerAction implements Action {
        private BrowseListener browseListener = null;

        @Override
        public void describeTo(Description description) {
            description
                    .appendText("Finds the internal BrowseListener to enable testing of internal calls.");
        }

        @Override
        public Object invoke(Invocation invocation) throws Throwable {
            Object param3 = invocation.getParameter(2);
            browseListener = (BrowseListener) param3;
            return null;
        }

        public BrowseListener getBrowseListener() {
            return browseListener;
        }

    }

    /**
     * Helper Browselistener to enable us to find the most recent success and
     * search result values during testing.
     */
    private class TestBrowseListener implements BrowseListener {
        private Boolean success = null;

        private SearchResult searchResult = null;

        @Override
        public void browseFinished(boolean success) {
            this.success = success;
        }

        @Override
        public void handleBrowseResult(SearchResult searchResult) {
            this.searchResult = searchResult;
        }

        public void reset() {
            this.success = null;
            this.searchResult = null;
        }

    }
}
