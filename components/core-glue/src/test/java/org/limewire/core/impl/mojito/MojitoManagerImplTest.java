package org.limewire.core.impl.mojito;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.PrintWriter;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.mojito.MojitoDHT;
import org.limewire.util.BaseTestCase;
import org.limewire.util.MatchAndCopy;

import com.limegroup.gnutella.dht.DHTController;
import com.limegroup.gnutella.dht.DHTEvent;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;

public class MojitoManagerImplTest extends BaseTestCase {

    public MojitoManagerImplTest(String name) {
        super(name);
    }

    public void testGetName() {
        Mockery context = new Mockery();
        final DHTManager dhtManager = context.mock(DHTManager.class);

        MojitoManagerImpl managerImpl = new MojitoManagerImpl(dhtManager);

        final MojitoDHT mojitoDHT = context.mock(MojitoDHT.class);

        final String mojitoName = "mojito";

        context.checking(new Expectations() {
            {
                one(dhtManager).getMojitoDHT();
                will(returnValue(null));
                
                one(dhtManager).getMojitoDHT();
                will(returnValue(mojitoDHT));
                one(mojitoDHT).getName();
                will(returnValue(mojitoName));
            }
        });

        
        assertNull(managerImpl.getName());
        assertEquals(mojitoName, managerImpl.getName());
        context.assertIsSatisfied();
    }

    public void testIsRunning() {
        Mockery context = new Mockery();
        final DHTManager dhtManager = context.mock(DHTManager.class);

        MojitoManagerImpl managerImpl = new MojitoManagerImpl(dhtManager);

        context.checking(new Expectations() {
            {
                one(dhtManager).isRunning();
                will(returnValue(true));
            }
        });

        assertTrue(managerImpl.isRunning());

        context.checking(new Expectations() {
            {
                one(dhtManager).isRunning();
                will(returnValue(false));
            }
        });

        assertFalse(managerImpl.isRunning());

        context.assertIsSatisfied();
    }
    
    
    /** 
     * Test the register function and ensure a functional listener is passed to the
     *  MojitoManager.  Test registering property change listeners and that they are fired
     *  on appropriate events. 
     */
    public void testListenerInfrastructure() {
        Mockery context = new Mockery();
        
        final DHTManager dhtManager = context.mock(DHTManager.class);
        final DHTController dhtController = context.mock(DHTController.class);
        
        final PropertyChangeListener listener1 = context.mock(PropertyChangeListener.class);
        final PropertyChangeListener listener2 = context.mock(PropertyChangeListener.class);
        
        final MatchAndCopy<DHTEventListener> listenerCollector 
            = new MatchAndCopy<DHTEventListener>(DHTEventListener.class);
        
        final MojitoManagerImpl manager = new MojitoManagerImpl(dhtManager);
        
        context.checking(new Expectations() {{
            exactly(1).of(dhtManager).addEventListener(with(listenerCollector));
       
            exactly(2).of(listener1).propertyChange(with(any(PropertyChangeEvent.class)));
            exactly(1).of(listener2).propertyChange(with(any(PropertyChangeEvent.class)));
        }});
        
        // Link the internal listener to the MojitoManager
        manager.registerListener();
        
        // Add external state change listeners
        manager.addPropertyChangeListener(listener1);
        manager.addPropertyChangeListener(listener2);
        
        // Fire an event
        listenerCollector.getLastMatch().handleDHTEvent(new DHTEvent(dhtController, DHTEvent.Type.CONNECTED));
        
        // Remove an external Listener
        manager.removePropertyChangeListener(listener2);
        
        // Fire another event that should not be bounced because caching
        listenerCollector.getLastMatch().handleDHTEvent(new DHTEvent(dhtController, DHTEvent.Type.STOPPED));
        
        // Fire another event that should fire a property changed event
        listenerCollector.getLastMatch().handleDHTEvent(new DHTEvent(dhtController, DHTEvent.Type.CONNECTED));
        
        context.assertIsSatisfied();
    }
    
    // TODO: test rest of handle??? a good handle???  reflection???
    /**
     * Test handle has a graceful fail when no Mojito instance is registered to the DHTManager.
     */
    public void testHandleFail() {
    
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final DHTManager dhtManager = context.mock(DHTManager.class);
        final PrintWriter dummyStream = context.mock(PrintWriter.class);
        
        final MojitoManagerImpl manager = new MojitoManagerImpl(dhtManager);
        
        context.checking(new Expectations() {{
            allowing(dhtManager).getMojitoDHT();
            will(returnValue(null));
            
            ignoring(dummyStream);
        }});
        
        assertFalse(manager.handle("???", dummyStream));

        context.assertIsSatisfied();
    }
}
