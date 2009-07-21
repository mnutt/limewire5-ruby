package org.limewire.facebook.service.livemessage;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.facebook.service.FacebookFriendConnection;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class PresenceHandler implements LiveMessageHandler {

    private static final String TYPE = "presence";

    @SuppressWarnings("unused")
    private static final Log LOG = LogFactory.getLog(PresenceHandler.class);

    private final FacebookFriendConnection connection;

    @Inject
    public PresenceHandler(@Assisted FacebookFriendConnection connection) {
        this.connection = connection;
    }

    @Override
    @Inject
    public void register(LiveMessageHandlerRegistry registry) {
        registry.register(TYPE, this);
    }

    @Override
    public void handle(String messageType, JSONObject message) throws JSONException {
        String from = message.getString("from");
        String type = message.getString("type");
        if (type.equals("unavailable")) {
            connection.removePresence(from);
        }
    }
}
