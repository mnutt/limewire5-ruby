package org.limewire.core.impl.updates;

import java.util.concurrent.atomic.AtomicReference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.version.UpdateEvent;
import com.limegroup.gnutella.version.UpdateHandler;
import com.limegroup.gnutella.version.UpdateInformation;

public class UpdateListenerTest extends BaseTestCase {

    public UpdateListenerTest(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public void testHandleEvent() {
        Mockery context = new Mockery();
        final UpdateHandler updateHandler = context.mock(UpdateHandler.class);
        final EventBroadcaster<org.limewire.core.api.updates.UpdateEvent> broadcaster = context
                .mock(EventBroadcaster.class);

        context.checking(new Expectations() {
            {
                one(updateHandler).addListener(with(any(EventListener.class)));
            }
        });
        UpdateListener updateListener = new UpdateListener(updateHandler, broadcaster);

        final UpdateInformation updateInformation = context.mock(UpdateInformation.class);
        final String button1Text = "1";
        final String button2Text = "2";
        final String commandText = "command";
        final String text = "text";
        final String title = "title";
        final String url = "url";
        
        final AtomicReference<org.limewire.core.api.updates.UpdateEvent> updateEvent = new AtomicReference<org.limewire.core.api.updates.UpdateEvent>();
        context.checking(new Expectations() {
            {
                one(updateInformation).getButton1Text();
                will(returnValue(button1Text));
                one(updateInformation).getButton2Text();
                will(returnValue(button2Text));
                one(updateInformation).getUpdateCommand();
                will(returnValue(commandText));
                one(updateInformation).getUpdateText();
                will(returnValue(text));
                one(updateInformation).getUpdateTitle();
                will(returnValue(title));
                one(updateInformation).getUpdateURL();
                will(returnValue(url));
                one(broadcaster).broadcast(with(any(org.limewire.core.api.updates.UpdateEvent.class)));
                will(new AssignParameterAction<org.limewire.core.api.updates.UpdateEvent>(updateEvent, 0));
            }
        });
        
        UpdateEvent updateEventParam = new UpdateEvent(updateInformation, UpdateEvent.Type.UPDATE);
        updateListener.handleEvent(updateEventParam);
        
        org.limewire.core.api.updates.UpdateEvent ude = updateEvent.get();
        assertNotNull(ude);
        
        assertEquals(button1Text, ude.getData().getButton1Text());
        assertEquals(button2Text, ude.getData().getButton2Text());
        assertEquals(commandText, ude.getData().getUpdateCommand());
        assertEquals(title, ude.getData().getUpdateTitle());
        assertEquals(text, ude.getData().getUpdateText());
        assertEquals(url, ude.getData().getUpdateURL());
    }
}
