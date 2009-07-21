package org.limewire.xmpp.client.impl.features;

import java.util.concurrent.atomic.AtomicReference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureRegistry;
import org.limewire.friend.api.feature.LibraryChangedNotifierFeature;
import org.limewire.friend.impl.feature.LibraryChangedNotifierFeatureInitializer;
import org.limewire.util.BaseTestCase;

public class LibraryChangedNotifierFeatureInitializerTest extends BaseTestCase {

    private Mockery context;
    private FriendPresence friendPresence;

    public LibraryChangedNotifierFeatureInitializerTest(String name) {
        super(name);
    }
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        friendPresence = context.mock(FriendPresence.class);
    }
    
    public void testRegister() {
        final FeatureRegistry featureRegistry = context.mock(FeatureRegistry.class);
        final LibraryChangedNotifierFeatureInitializer initializer = new LibraryChangedNotifierFeatureInitializer();
        context.checking(new Expectations() {{
            one(featureRegistry).registerPublicInitializer(LibraryChangedNotifierFeature.ID, initializer);
        }});
        initializer.register(featureRegistry);
        context.assertIsSatisfied();
    }
    
    public void testInitializeFeature() {
        final AtomicReference<LibraryChangedNotifierFeature> feature = new AtomicReference<LibraryChangedNotifierFeature>();
        context.checking(new Expectations() {{
            one(friendPresence).addFeature(with(any(Feature.class)));
            will(new CustomAction("get hold of feature") {
                @Override
                public Object invoke(Invocation invocation) throws Throwable {
                    feature.set((LibraryChangedNotifierFeature) invocation.getParameter(0));
                    return null;
                }
            });
        }});
        LibraryChangedNotifierFeatureInitializer initializer = new LibraryChangedNotifierFeatureInitializer();
        initializer.initializeFeature(friendPresence);
        context.assertIsSatisfied();
    }

}
