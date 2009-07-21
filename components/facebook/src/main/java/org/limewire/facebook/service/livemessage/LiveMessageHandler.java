package org.limewire.facebook.service.livemessage;

import org.json.JSONObject;
import org.json.JSONException;

import com.google.inject.Inject;

/**
 * Handles incoming live messages from Facebook.
 */
public interface LiveMessageHandler {
    /**
     * Should be annotated with {@link Inject} and then call 
     * {@link LiveMessageHandlerRegistry#register(String, LiveMessageHandler)}
     * with the message type it handles.
     */
    void register(LiveMessageHandlerRegistry registry);
    /**
     * Called when a live message comes in of the type that the handler registered
     * for in {@link #register(LiveMessageHandlerRegistry)}. 
     * @param messageType the type of the message
     * @param message the message payload
     * @throws JSONException thrown by the handler if the message payload is
     * invalid due to missing data or invalid data
     */
    void handle(String messageType, JSONObject message) throws JSONException;
}
