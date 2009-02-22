package org.limewire.core.impl.search.friend;

import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.impl.library.FriendLibraries;
import org.limewire.util.BaseTestCase;

public class FriendAutoCompletersImplTest extends BaseTestCase {

    public FriendAutoCompletersImplTest(String name) {
        super(name);
    }

    public void testGetDictionary() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        FriendLibraries friendLibraries = context.mock(FriendLibraries.class);
        FriendAutoCompletersImpl friendAutoCompletersImpl = new FriendAutoCompletersImpl(
                friendLibraries);
        
        for(SearchCategory searchCategory : SearchCategory.values()) {
            assertNotNull(friendAutoCompletersImpl.getDictionary(searchCategory));
        }
        context.assertIsSatisfied();
    }
}
