package org.limewire.facebook.service.livemessage;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.limewire.facebook.service.FacebookFriendConnection;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.AuthToken;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.friend.impl.feature.AuthTokenImpl;
import org.limewire.util.StringUtils;
import org.apache.commons.codec.binary.Base64;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class AuthTokenHandler implements LiveMessageHandler, FeatureTransport<AuthToken> {
    
    private static final String TYPE = "auth-token";
    
    private final FacebookFriendConnection connection;

    private final Handler<AuthToken> authTokenHandler;

    @Inject
    AuthTokenHandler(@Assisted FacebookFriendConnection connection,
            FeatureTransport.Handler<AuthToken> authTokenHandler) {
        this.connection = connection;
        this.authTokenHandler = authTokenHandler;
    }

    @Override
    @Inject
    public void register(LiveMessageHandlerRegistry registry) {
        registry.register(TYPE, this);
    }

    @Override
    public void handle(String messageType, JSONObject message) {
        String from = message.optString("from", null);
        String authtoken = message.optString("auth-token", null);
        if (from != null && authtoken != null) {
            authTokenHandler.featureReceived(from, new AuthTokenImpl(authtoken));
        }
    }
    
    @Override
    public void sendFeature(FriendPresence presence, AuthToken localFeature) throws FriendException {
        Map<String, Object> message = new HashMap<String, Object>();
        message.put("auth-token", StringUtils.getUTF8String(Base64.encodeBase64(StringUtils.toUTF8Bytes(localFeature.getBase64()))));
        connection.sendLiveMessage(presence, TYPE, message);
    }
}
