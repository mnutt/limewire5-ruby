package org.limewire.core.impl.friend;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.impl.friend.FriendPresenceLibraryAdder;
import org.limewire.core.impl.friend.FriendPresenceLibraryAdder.FeatureListener;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.AddressFeature;
import org.limewire.friend.api.feature.AuthTokenFeature;
import org.limewire.friend.api.feature.Feature;
import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.friend.api.feature.FeatureEvent.Type;
import org.limewire.listener.ListenerSupport;
import org.limewire.util.BaseTestCase;

public class FriendPresenceLibraryAdderTest extends BaseTestCase {


    public FriendPresenceLibraryAdderTest(String name) {
        super(name);
    }

    /**
     * Ensure that register adds a valid listener once.
     */
    @SuppressWarnings("unchecked")
    public void testRegister() {
        
        Mockery context = new Mockery();
        
        final ListenerSupport<FeatureEvent> featureSupport = context.mock(ListenerSupport.class);
        final FriendPresenceLibraryAdder adder = new FriendPresenceLibraryAdder(null);
        
        context.checking(new Expectations() {
            {  
                exactly(1).of(featureSupport).addListener(with(any(FeatureListener.class)));
            
            }});
        
        adder.register(featureSupport);
    }
    
    /**
     * Test the added event and make sure the presence is added when it has features.
     */
    public void testHandleEventWithAdded() {
        Mockery context = new Mockery();
        
        final FriendPresence presence1 = context.mock(FriendPresence.class);
        final FriendPresence presence2 = context.mock(FriendPresence.class);
        
        final RemoteLibraryManager manager = context.mock(RemoteLibraryManager.class);
        
        final FriendPresenceLibraryAdder adder = new FriendPresenceLibraryAdder(manager);
        final FeatureListener listener = adder.new FeatureListener();
        final FeatureEvent e1 = new FeatureEvent(presence1, Type.ADDED, new Feature<Object>(new Object(), null));
        final FeatureEvent e2 = new FeatureEvent(presence2, Type.ADDED, new Feature<Object>(new Object(), null));
                
        context.checking(new Expectations() {
            {
                allowing(presence1).hasFeatures(AddressFeature.ID, AuthTokenFeature.ID);
                will(returnValue(true));
                allowing(presence2).hasFeatures(AddressFeature.ID, AuthTokenFeature.ID);
                will(returnValue(false));
                
                exactly(1).of(manager).addPresenceLibrary(presence1);
            }
        });
        
        listener.handleEvent(e1);
        listener.handleEvent(e2);
    }
    
    /**
     * Test the removed event and make sure the presence is removed when it has not features.
     */
    public void testHandleEventWithRemoved() {
        Mockery context = new Mockery();
        
        final FriendPresence presence1 = context.mock(FriendPresence.class);
        final FriendPresence presence2 = context.mock(FriendPresence.class);
        
        final RemoteLibraryManager manager = context.mock(RemoteLibraryManager.class);
        
        final FriendPresenceLibraryAdder adder = new FriendPresenceLibraryAdder(manager);
        final FeatureListener listener = adder.new FeatureListener();
        final FeatureEvent e1 = new FeatureEvent(presence1, Type.REMOVED, new Feature<Object>(new Object(), null));
        final FeatureEvent e2 = new FeatureEvent(presence2, Type.REMOVED, new Feature<Object>(new Object(), null));
                
        context.checking(new Expectations() {
            {
                allowing(presence1).hasFeatures(AddressFeature.ID, AuthTokenFeature.ID);
                will(returnValue(true));
                allowing(presence2).hasFeatures(AddressFeature.ID, AuthTokenFeature.ID);
                will(returnValue(false));
                
                exactly(1).of(manager).removePresenceLibrary(presence2);
            }
        });
        
        listener.handleEvent(e1);
        listener.handleEvent(e2);
    }
    
    /**
     * Fire the event handler with a forced null event and ensure no actions
     * are made on presences.
     */
    public void testHandleEventWithOther() {
        
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        
        final FeatureEvent event = context.mock(FeatureEvent.class);
        
        final RemoteLibraryManager manager = context.mock(RemoteLibraryManager.class);
        
        final FriendPresenceLibraryAdder adder = new FriendPresenceLibraryAdder(manager);
        final FeatureListener listener = adder.new FeatureListener();
        
                
        context.checking(new Expectations() {
            {
               allowing(event).getType();
               will(returnValue(null));
               
               allowing(event).getSource();
               will(returnValue(null));
                
               // Nothing else should happen
            }
        });
        
        listener.handleEvent(event);
    }
    
}
