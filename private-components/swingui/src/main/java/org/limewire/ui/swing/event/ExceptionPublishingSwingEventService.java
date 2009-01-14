package org.limewire.ui.swing.event;

import org.bushe.swing.event.EventServiceLocator;
import org.bushe.swing.event.SwingEventService;
import org.limewire.service.ErrorService;

/**
 * Event service (utilized by the EventBus library) that simply passes
 * any exceptions thrown from event subscribers to the LW exception-handling logic.
 */
public class ExceptionPublishingSwingEventService extends SwingEventService {
    
    public static void install() {
        System.setProperty(EventServiceLocator.SWING_EVENT_SERVICE_CLASS,
                ExceptionPublishingSwingEventService.class.getName());
    }

    @Override
    protected void handleException(String action, Object event, String topic, Object eventObj,
            Throwable e, StackTraceElement[] callingStack, String sourceString) {
        super.handleException(action, event, topic, eventObj, e, callingStack, sourceString);
        ErrorService.error(e, "Uncaught EventBus Subscriber Error");
    }
}
