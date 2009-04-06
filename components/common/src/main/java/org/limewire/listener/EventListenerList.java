package org.limewire.listener;

import java.awt.EventQueue;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.limewire.concurrent.ExecutorsHelper;
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
 * is called via {@link EventListenerList#dispatch(EventListener, Object, EventListenerListContext)}.  This
 * ensures that the event is dispatched appropriately, according to the 
 * annotation on the delegate listener.
 */
public class EventListenerList<E> implements ListenerSupport<E>, EventBroadcaster<E> {
    
    private final Log log;
    
    private final List<ListenerProxy<E>> listenerList = new CopyOnWriteArrayList<ListenerProxy<E>>();
    private final EventListenerListContext context = new EventListenerListContext();

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
     * 
     * @param context The {@link EventListenerList} context to dispatch the
     *        event with. Usually retrieved by
     *        {@link EventListenerList#getContext()}. The context may be null,
     *        which will result in context not being used.
     */
    public static <E> void dispatch(EventListener<E> listener, E event, EventListenerListContext context) {
        EventListener<E> proxy = new ListenerProxy<E>(null, Objects.nonNull(listener, "listener"), context);
        proxy.handleEvent(event);
    }
    
    /**
     * Returns the context which can be used to dispatch events via
     * {@link EventListenerList#dispatch(EventListener, Object, org.limewire.listener.EventListenerList.EventListenerListContext)}
     */
    public EventListenerListContext getContext() {
        return context; 
    }
    
    /** Adds the listener. */
    public void addListener(EventListener<E> listener) {
        if(log != null) {
            log.debugf("adding listener {0} to {1}", listener, this);
        }
        listenerList.add(new ListenerProxy<E>(log, Objects.nonNull(listener, "listener"), context));
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
    
    /** Notifies just this listener about an event. */
    public void notifyListener(EventListener<E> listener, E event) {
        EventListener<E> proxy = new ListenerProxy<E>(null, Objects.nonNull(listener, "listener"), context);
        proxy.handleEvent(event);
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
        private final EventListenerListContext context;
        
        private volatile DispatchType type = DispatchType.UNKNOWN;
        private volatile Executor executor = null;
        
        public ListenerProxy(Log log, EventListener<E> delegate, EventListenerListContext context) {
            this.log = log;
            this.delegate = delegate;
            this.context = context;
        }
        
        @Override
        public void handleEvent(final E event) {
            // Note: This is not thread-safe, but it is OK to analyze multiple times.
            //       The internals of analyze make sure that only one ExecutorMap & Executor are
            //       ever set.
            if(type == DispatchType.UNKNOWN) {
                analyze(delegate, event);                
            }
            
            if(log != null) {
                log.tracef("Dispatching event {0} to {1} of type {2}", event, delegate, type);
            }
            type.dispatch(delegate, event, executor);
        }
        
        /**
         * Loops through all 'handleEvent' methods whose parameter type can match event's
         * classes & superclasses.  When one is found, see if it is annotated with {@link SwingEDTEvent} or
         * {@link BlockingEvent}.
         */
        private void analyze(EventListener<E> delegate, E event) {
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

            BlockingEvent blockingEvent;
            if(method.getAnnotation(SwingEDTEvent.class) != null) {
                type = DispatchType.SWING;
            } else if((blockingEvent = method.getAnnotation(BlockingEvent.class)) != null) {
                type = DispatchType.BLOCKING;
                if(context != null) { // context may be null if no context is supported.
                    executor = context.getOrCreateExecutor(blockingEvent.queueName());
                }
            } else {
                type = DispatchType.INLINE;
            }
        }
        

        private static enum DispatchType {
            UNKNOWN() {
                @Override
                <E> void dispatch(EventListener<E> listener, E event, Executor executor) {
                    throw new IllegalStateException("unknown dispatch!");
                }
            }

            ,
            INLINE() {
                @Override
                <E> void dispatch(EventListener<E> listener, E event, Executor executor) {
                    listener.handleEvent(event);
                }
            },
            SWING() {
                @Override
                <E> void dispatch(final EventListener<E> listener, final E event, Executor executor) {
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
                <E> void dispatch(final EventListener<E> listener, final E event, Executor executor) {
                    Runnable runner = new Runnable() {
                        public void run() {
                            listener.handleEvent(event);
                        }
                    };
                    
                    if(executor == null) {
                        ThreadExecutor.startThread(runner, "BlockingEvent");
                    } else {
                        executor.execute(runner);
                    }
                }
            };

            abstract <E> void dispatch(EventListener<E> listener, E event, Executor executor);

        };
    }
    
    /** 
     * The context of an {@link EventListenerList}.  The context is used to ensure
     * that any state required over multiple event notifications within a single list
     * is maintained, such as notifying {@link BlockingEvent BlockingEvents} with a specific
     * {@link BlockingEvent#queueName()} in the same queue.
     */
    public static final class EventListenerListContext {
        private final AtomicReference<ConcurrentMap<String, Executor>> eventExecutorsRef = new AtomicReference<ConcurrentMap<String,Executor>>();
        
        private Executor getOrCreateExecutor(String queueName) {
            if(queueName != null && !queueName.equals("")) {
                ConcurrentMap<String, Executor> executorMap = eventExecutorsRef.get();
                // If no executorMap exists already, create a new one.
                if(executorMap == null) {
                    eventExecutorsRef.compareAndSet(null, new ConcurrentHashMap<String, Executor>());
                    executorMap = eventExecutorsRef.get();
                }
                
                // If no executor exists for this queueName, create a new one.
                Executor executor = executorMap.get(queueName);
                if(executor == null) {
                    executorMap.putIfAbsent(queueName, ExecutorsHelper.newProcessingQueue("BlockingEventQueue-" + queueName));
                    executor = executorMap.get(queueName);
                }
                return executor;
            } else {
                return null; // No executor required.
            }
        }
    }
}
