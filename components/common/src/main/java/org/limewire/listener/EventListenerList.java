package org.limewire.listener;

import java.awt.EventQueue;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.ExceptionUtils;
import org.limewire.util.Objects;

/**
 * Maintains event listeners and broadcasts events to all listeners.
 * <p>
 * The annotations {@link SwingEDTEvent} and {@link BlockingEvent} can be added
 * to implementations of {@link EventListener#handleEvent(Object)} in order to 
 * allow those events to be dispatched on the EDT thread or a new thread.
 * <p>
 * If classes want to delegate implementations of {@link EventListener}, it is
 * important that the delegate listener's <code>handleEvent(E)</code> method
 * is called via {@link EventListenerList#dispatch(EventListener, Object)}.  This
 * ensures that the event is dispatched appropriately, according to the 
 * annotation on the delegate listener.
 */
public class EventListenerList<E> implements ListenerSupport<E>, EventBroadcaster<E> {
    
    private final Log log;
    
    private final List<ListenerProxy<E>> listenerList = new CopyOnWriteArrayList<ListenerProxy<E>>();

    public EventListenerList() {
        this.log = null;        
    }
    
    public EventListenerList(Class loggerKey) {
        this.log = LogFactory.getLog(loggerKey); 
    }
    
    public EventListenerList(Log log) {
        this.log = log;
    }
    
    /**
     * Dispatches the event to the listener. This scans the listener for
     * annotations and dispatches in the correct thread, according to the
     * annotation.
     */
    public static <E> void dispatch(EventListener<E> listener, E event) {
        EventListener<E> proxy = new ListenerProxy<E>(null, Objects.nonNull(listener, "listener"));
        proxy.handleEvent(event);
    }
    
    /** Adds the listener. */
    public void addListener(EventListener<E> listener) {
        if(log != null) {
            log.debugf("adding listener {0} to {1}", listener, this);
        }
        listenerList.add(new ListenerProxy<E>(log, Objects.nonNull(listener, "listener")));
    }
    
    /** Returns true if the listener was removed. */
    public boolean removeListener(EventListener<E> listener) {
        if(log != null) {
            log.debugf("removing listener {0} from {1}", listener, this);
        }
        Objects.nonNull(listener, "listener");
        
        // Find the proxy, then remove it.
        for(ListenerProxy<E> proxyListener : listenerList) {
            if(proxyListener.delegate.equals(listener)) {
                return listenerList.remove(proxyListener);
            }
        }
        return false;
    }
    
    /** Broadcasts an event to all listeners. */
    public void broadcast(E event) {
        Objects.nonNull(event, "event");
        if(log != null) {
            log.debugf("broadcasting event {0} from {1}", event, this);
        }
        
        // When broadcasting, capture exceptions to make sure each listeners
        // gets a shot.  If an exception occurs and can be reported immediately,
        // it is reported.  Otherwise, is captured & the first exception
        // is reported after the loop finishes.
        
        Throwable t = null;
        for(ListenerProxy<E> listener : listenerList) {
            try {
                listener.handleEvent(event);
            } catch(Throwable thrown) {
                if(log != null) {
                    log.error("error dispatching " + event, thrown);
                }
                
                thrown = ExceptionUtils.reportOrReturn(thrown);
                if(thrown != null && t == null) {
                    t = thrown;
                }
            }
        }
        
        if(t != null) {
            ExceptionUtils.reportOrRethrow(t);
        }
    }
    
    /** Returns the size of the list. */
    public int size() {
        return listenerList.size();
    }
    
    private static final class ListenerProxy<E> implements EventListener<E> {
        private final Log log;
        private final EventListener<E> delegate;
        private volatile DispatchType type = DispatchType.UNKNOWN;
        
        public ListenerProxy(Log log, EventListener<E> delegate) {
            this.log = log;
            this.delegate = delegate;
        }
        
        @Override
        public void handleEvent(final E event) {
            // Note: this is not thread-safe, but it is OK to analyze multiple times.
            if(type == DispatchType.UNKNOWN) {
                type = analyze(delegate, event);                
            }
            if(log != null) {
                log.tracef("Dispatching event {0} to {1} of type {2}", event, delegate, type);
            }
            type.dispatch(delegate, event);
        }
        
        /**
         * Loops through all 'handleEvent' methods whose parameter type can match event's
         * classes & superclasses.  When one is found, see if it is annotated with {@link SwingEDTEvent} or
         * {@link BlockingEvent}.
         */
        private DispatchType analyze(EventListener<E> delegate, E event) {
            Class<?> eventClass = event.getClass();
            Method method = null;
            while(eventClass != null) {
                try {
                    method = delegate.getClass().getMethod("handleEvent", eventClass);
                    break;
                } catch (NoSuchMethodException ignored) {
                }
                eventClass = eventClass.getSuperclass();
            }
            
            if(method == null) {
                throw new IllegalStateException("Unable to find method!");
            }
            
            if(method.getAnnotation(SwingEDTEvent.class) != null) {
                return DispatchType.SWING;
            } else if(method.getAnnotation(BlockingEvent.class) != null) {
                return DispatchType.BLOCKING;
            } else {
                return DispatchType.INLINE;
            }
        }
        

        private static enum DispatchType {
            UNKNOWN() {
                @Override
                <E> void dispatch(EventListener<E> listener, E event) {
                    throw new IllegalStateException("unknown dispatch!");
                }
            }

            ,
            INLINE() {
                @Override
                <E> void dispatch(EventListener<E> listener, E event) {
                    listener.handleEvent(event);
                }
            },
            SWING() {
                @Override
                <E> void dispatch(final EventListener<E> listener, final E event) {
                    if(EventQueue.isDispatchThread()) {
                        listener.handleEvent(event);
                    } else {
                        EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                listener.handleEvent(event);
                            }
                        });
                    }
                }
            },
            BLOCKING() {
                @Override
                <E> void dispatch(final EventListener<E> listener, final E event) {
                    ThreadExecutor.startThread(new Runnable() {
                        public void run() {
                            listener.handleEvent(event);
                        }
                    }, "BlockingEvent");
                }
            };

            abstract <E> void dispatch(EventListener<E> listener, E event);

        };
        
    }
}
