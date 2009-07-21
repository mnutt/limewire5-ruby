package org.limewire.core.impl.browse;

import java.util.HashSet;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.limewire.core.api.browse.BrowseListener;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.QueryReplyListener;
import org.limewire.core.impl.search.QueryReplyListenerList;
import org.limewire.friend.api.FriendPresence;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.util.BaseTestCase;
import org.limewire.util.MatchAndCopy;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.messages.QueryReply;

public class CoreBrowseTest extends BaseTestCase {

    public CoreBrowseTest(String name) {
        super(name);
    }

    /**
     * Tests that the supplied browse listener is populated with search results
     * as handleBrowseResult is called on the internal {@link BrowseListener} of the
     * {@link CoreBrowse} object.
     */
    public void testBasicBrowseListenerPopulation() {
        final Mockery context = new Mockery();
        final FriendPresence friendPresence = context.mock(FriendPresence.class);
        final SearchServices searchServices = context.mock(SearchServices.class);
        final QueryReplyListenerList queryReplyListenerList = context
                .mock(QueryReplyListenerList.class);

        final byte[] searchGuid = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
                16 };

        final BrowseListener testBrowseListener = context.mock(BrowseListener.class);
        
        final RemoteFileDesc rfd = context.mock(RemoteFileDesc.class); 
        final QueryReply queryReply = context.mock(QueryReply.class);
        
        final MatchAndCopy<SearchResult> searchResultCollector
            = new MatchAndCopy<SearchResult>(SearchResult.class);
        final MatchAndCopy<BrowseListener> browseListenerCollector 
            = new MatchAndCopy<BrowseListener>(BrowseListener.class);
        final MatchAndCopy<QueryReplyListener> queryReplyListenerCollector
            = new MatchAndCopy<QueryReplyListener>(QueryReplyListener.class);
        
        context.checking(new Expectations() {
            {
                one(queryReplyListenerList).addQueryReplyListener(with(any(byte[].class)),
                        with(queryReplyListenerCollector));
                exactly(2).of(queryReplyListenerList).removeQueryReplyListener(
                        with(any(byte[].class)), with(any(QueryReplyListener.class)));
                one(searchServices).newQueryGUID();
                will(returnValue(searchGuid));
                one(searchServices).doAsynchronousBrowseHost(
                        with(any(FriendPresence.class)), with(any(GUID.class)),
                        with(browseListenerCollector));
                exactly(2).of(searchServices).stopQuery(new GUID(searchGuid));
                
                exactly(2).of(testBrowseListener).handleBrowseResult(with(searchResultCollector));
                
                allowing(rfd).getClientGUID();
                will(returnValue(new byte[] {'x','x','x','x','x','x','x','x','x','x','x','x','x','x','x','x'}));
                allowing(rfd);
                allowing(queryReply);
                
                
                Sequence sequence1 = context.sequence("seq");
                exactly(1).of(testBrowseListener).browseFinished(false);
                inSequence(sequence1);
            }
        });

        CoreBrowse coreBrowse = new CoreBrowse(friendPresence, searchServices,
                queryReplyListenerList);

        coreBrowse.start(testBrowseListener);
        
        // Can't start twice
        try {
            coreBrowse.start(testBrowseListener);
            fail("Starting a browse twice did not throw an exception");
        } 
        catch (IllegalStateException e) {
            // Expected
        }

        BrowseListener innerBrowseListener = browseListenerCollector.getLastMatch();
        assertNotNull(innerBrowseListener);

        // Call handleBrowseResult directly
        SearchResult searchResult1 = context.mock(SearchResult.class);
        innerBrowseListener.handleBrowseResult(searchResult1);
        assertEquals(searchResult1, searchResultCollector.getLastMatch());

        // Call handleBrowseResult indirectly through BrowseResultAdapter
        queryReplyListenerCollector.getLastMatch().handleQueryReply(rfd, queryReply, new HashSet<IpPort>());

        innerBrowseListener.browseFinished(false);
        innerBrowseListener.browseFinished(true);

        context.assertIsSatisfied();
    }
}
