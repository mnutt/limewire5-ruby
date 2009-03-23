package org.limewire.core.impl.lifecycle;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.lifecycle.LifeCycleEvent;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.LifecycleManager;

import junit.framework.TestCase;

public class LifeCycleManagerImplTest extends TestCase {
    @SuppressWarnings("unchecked")
    public void testBasics() {
        Mockery context = new Mockery();
        final LifecycleManager lifecycleManager = context.mock(LifecycleManager.class);
        final LifeCycleManagerImpl lifeCycleManagerImpl = new LifeCycleManagerImpl(lifecycleManager);
        final EventListener<LifeCycleEvent> eventListener = context.mock(EventListener.class);

        context.checking(new Expectations() {
            {
                one(lifecycleManager).addListener(eventListener);
            }
        });

        lifeCycleManagerImpl.addListener(eventListener);

        context.checking(new Expectations() {
            {
                one(lifecycleManager).removeListener(eventListener);
            }
        });

        lifeCycleManagerImpl.removeListener(eventListener);

        context.checking(new Expectations() {
            {
                one(lifecycleManager).isLoaded();
                will(returnValue(false));
            }
        });

        assertFalse(lifeCycleManagerImpl.isLoaded());
        
        
        context.checking(new Expectations() {
            {
                one(lifecycleManager).isStarted();
                will(returnValue(false));
            }
        });

        assertFalse(lifeCycleManagerImpl.isStarted());


        context.checking(new Expectations() {
            {
                one(lifecycleManager).isShutdown();
                will(returnValue(false));
            }
        });

        assertFalse(lifeCycleManagerImpl.isShutdown());

        context.checking(new Expectations() {
            {
                one(lifecycleManager).isLoaded();
                will(returnValue(true));
            }
        });

        assertTrue(lifeCycleManagerImpl.isLoaded());
        
        
        context.checking(new Expectations() {
            {
                one(lifecycleManager).isStarted();
                will(returnValue(true));
            }
        });

        assertTrue(lifeCycleManagerImpl.isStarted());


        context.checking(new Expectations() {
            {
                one(lifecycleManager).isShutdown();
                will(returnValue(true));
            }
        });

        assertTrue(lifeCycleManagerImpl.isShutdown());

        context.assertIsSatisfied();
    }
}
