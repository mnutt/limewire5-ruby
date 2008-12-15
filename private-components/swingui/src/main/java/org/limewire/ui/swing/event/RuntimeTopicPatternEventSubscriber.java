package org.limewire.ui.swing.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.bushe.swing.event.EventService;
import org.bushe.swing.event.EventServiceLocator;
import org.bushe.swing.event.ThreadSafeEventService;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RuntimeTopicPatternEventSubscriber {
    /**
     * The name of a method (which should return a String) and whose return value will become the subscription topic.
     */
    String methodName() default "getTopicPatternName";

    /** 
     * The event service to subscribe to, default to the EventServiceLocator.SERVICE_NAME_EVENT_BUS. 
     */
    String eventServiceName() default EventServiceLocator.SERVICE_NAME_EVENT_BUS;
    
    /** Whether or not to subcribe to the exact class or a class hierarchy, defaults to class hierarchy (false). */
    boolean exact() default false;

    /**
     * Whether or not to autocreate the event service if it doesn't exist on subscription, default
     * is true. If the service needs to be created, it must have a default constructor.
     */
    Class<? extends EventService> autoCreateEventServiceClass() default ThreadSafeEventService.class;
}
