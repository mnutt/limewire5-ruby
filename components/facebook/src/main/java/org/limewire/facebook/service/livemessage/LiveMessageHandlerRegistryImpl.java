package org.limewire.facebook.service.livemessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Default implementation of {@link LiveMessageHandlerRegistry}.
 * <p>
 * Threadsafe.
 */
@Singleton
class LiveMessageHandlerRegistryImpl implements LiveMessageHandlerRegistry {
    
    private final Map<String, LiveMessageHandler> handlers;
    
    @Inject
    LiveMessageHandlerRegistryImpl() {
        this.handlers = new ConcurrentHashMap<String, LiveMessageHandler>();
    }

    @Override
    public void register(String messageType, LiveMessageHandler handler) {
        handlers.put(messageType, handler);
    }

    @Override
    public LiveMessageHandler getHandler(String messageType) {
        return handlers.get(messageType);
    }
}
