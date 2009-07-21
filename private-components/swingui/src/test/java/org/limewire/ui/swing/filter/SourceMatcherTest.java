package org.limewire.ui.swing.filter;

import java.util.Collections;
import java.util.Map;

import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.PresenceEvent;
import org.limewire.listener.EventListener;
import org.limewire.util.BaseTestCase;

/**
 * JUnit test case for SourceMatcher.
 */
public class SourceMatcherTest extends BaseTestCase {
    /** Instance of class to be tested. */
    private SourceMatcher<MockFilterableItem> sourceMatcher;
    
    /**
     * Constructs a test case for the specified method name.
     */
    public SourceMatcherTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sourceMatcher = new SourceMatcher<MockFilterableItem>(SourceItem.ANONYMOUS_SOURCE);
    }

    @Override
    protected void tearDown() throws Exception {
        sourceMatcher = null;
        super.tearDown();
    }

    /** Tests method to match item with anonymous source. */
    public void testMatchesAnonymous() {
        // Create test item.
        MockFilterableItem item = new MockFilterableItem("Test Item");
        
        // Verify anonymous item matches.
        boolean actualReturn = sourceMatcher.matches(item);
        assertTrue("matches anonymous", actualReturn);
    }

    /** Tests method to match item with any friend. */
    public void testMatchesAnyFriend() {
        // Create matcher for any friend.
        sourceMatcher = new SourceMatcher<MockFilterableItem>(SourceItem.ANY_FRIEND_SOURCE);
        
        // Create test item.
        MockFilterableItem item = new MockFilterableItem("Test Item");
        item.addFriend(new TestFriend("Vulcan", false));
        
        // Verify any friend matches.
        boolean actualReturn = sourceMatcher.matches(item);
        assertTrue("matches any friend", actualReturn);
    }

    /** Tests method to match item with specific friend. */
    public void testMatchesFriend() {
        // Create matcher for specific friend.
        SourceItem friendSource = new SourceItem(SourceItem.Type.FRIEND, "Vulcan");
        sourceMatcher = new SourceMatcher<MockFilterableItem>(friendSource);
        
        // Create test item.
        MockFilterableItem item = new MockFilterableItem("Test Item");
        item.addFriend(new TestFriend("Vulcan", false));
        
        // Verify same friend matches.
        boolean actualReturn = sourceMatcher.matches(item);
        assertTrue("matches same friend", actualReturn);
        
        // Create 2nd test item.
        MockFilterableItem item2 = new MockFilterableItem("Test Item");
        item2.addFriend(new TestFriend("Klingon", false));
        
        // Verify different friend does not match.
        actualReturn = sourceMatcher.matches(item2);
        assertFalse("matches different friend", actualReturn);
    }
    
    /**
     * Test friend.
     */
    private static class TestFriend implements Friend {
        private boolean anonymous;
        private String name;

        public TestFriend(String name, boolean anonymous) {
            this.name = name;
            this.anonymous = anonymous;
        }
        
        @Override
        public String getFirstName() {
            return name;
        }

        @Override
        public String getId() {
            return name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Network getNetwork() {
            return new Network() {
                @Override
                public String getCanonicalizedLocalID() {
                    return name;
                }
                
                @Override
                public String getNetworkName() {
                    return "";
                }

                @Override
                public Type getType() {
                    return null;
                }
            };
        }

        @Override
        public String getRenderName() {
            return name;
        }

        @Override
        public boolean isAnonymous() {
            return anonymous;
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public void addPresenceListener(EventListener<PresenceEvent> presenceListener) {
        }

        @Override
        public MessageWriter createChat(MessageReader reader) {
            return null;
        }

        @Override
        public FriendPresence getActivePresence() {
            return null;
        }

        @Override
        public Map<String, FriendPresence> getPresences() {
            return Collections.emptyMap();
        }

        @Override
        public boolean hasActivePresence() {
            return false;
        }

        @Override
        public boolean isSignedIn() {
            return false;
        }

        @Override
        public boolean isSubscribed() {
            return false;
        }

        @Override
        public void removeChatListener() {
        }

        @Override
        public void setChatListenerIfNecessary(IncomingChatListener listener) {
        }
    }
}
