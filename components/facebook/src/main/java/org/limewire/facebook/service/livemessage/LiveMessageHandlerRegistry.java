package org.limewire.facebook.service.livemessage;

/**
 * Registry for {@link LiveMessageHandler} by their message type.
 */
public interface LiveMessageHandlerRegistry {
    /**
     * Registers a live message handler by the message type it handles. 
     */
    void register(String messageType, LiveMessageHandler handler);
    /**
     * @return the live message handler for a message type or null if there is none 
     */
    LiveMessageHandler getHandler(String messageType);
}
