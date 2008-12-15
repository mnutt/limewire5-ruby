package org.limewire.ui.swing.event;

import org.bushe.swing.event.EventService;
import org.bushe.swing.event.EventServiceLocator;

/**
 * @author Mario Aquino, Object Computing, Inc.
 * 
 */
public abstract class AbstractEDTEvent {
    private static EventService eventService;
    
    public static void setEventService(EventService service) {
        eventService = service;
    }
    
    public void publish() {
        getEventService().publish(this);
    }

    private static EventService getEventService() {
        if (eventService == null) {
            eventService = EventServiceLocator.getSwingEventService();
        }
        return eventService;
    }
    
    public void publish(String topic) {
        getEventService().publish(topic, this);
    }
}
