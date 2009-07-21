package org.limewire.facebook.service.livemessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.facebook.service.FacebookFriendConnection;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.io.Connectable;
import org.limewire.io.GUID;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.ConnectBackRequest;
import org.limewire.net.address.ConnectableSerializer;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Handles incoming connect back request through the facebook connection and
 * fowards them to the {@link Handler} for {@link ConnectBackRequest connect back requests}.
 * <p>
 * Sends {@link ConnectBackRequest connect back requests} to facebook friends.
 */
public class ConnectBackRequestHandler implements FeatureTransport<ConnectBackRequest>,
        LiveMessageHandler {

    private static final Log LOG = LogFactory.getLog(AddressHandler.class);
    
    private static final String TYPE = "connect-back-request";
    private final Handler<ConnectBackRequest> connectBackRequestHandler;

    private final FacebookFriendConnection connection;
    
    @Inject
    public ConnectBackRequestHandler(@Assisted FacebookFriendConnection connection,
                FeatureTransport.Handler<ConnectBackRequest> connectBackRequestHandler) {
        this.connection = connection;
        this.connectBackRequestHandler = connectBackRequestHandler;
    }

    @Inject
    @Override
    public void register(LiveMessageHandlerRegistry registry) {
        registry.register(TYPE, this);
    }
    
    @Override
    public void handle(String messageType, JSONObject message) throws JSONException {
        assert messageType.equals(TYPE);
        GUID clientGuid;
        try {
            clientGuid = new GUID(message.getString("client-guid"));
        } catch (IllegalArgumentException iae) {
            LOG.debug("invalid guid", iae);
            throw new JSONException(iae);
        }
        int fwtVersion = message.getInt("supported-fwt-version");
        Connectable address = deserializeAddress(message.getJSONObject("address"));
        String from = message.getString("from");
        connectBackRequestHandler.featureReceived(from, new ConnectBackRequest(address, clientGuid, fwtVersion));
    }
    
    static Connectable deserializeAddress(JSONObject address) throws JSONException {
        ConnectableSerializer serializer = new ConnectableSerializer();
        String type = address.getString("type"); 
        if (!type.equals(serializer.getAddressType())) {
            LOG.debugf("invalid address type: {0}", type);
            throw new JSONException("wrong address type: " + type);
        }
        try {
            return serializer.deserialize(Base64.decodeBase64(StringUtils.toUTF8Bytes(address.getString("value"))));
        } catch (IOException ie) {
            LOG.debug("invalid address", ie);
            throw new JSONException(ie);
        }
    }

    @Override
    public void sendFeature(FriendPresence presence, ConnectBackRequest data)
            throws FriendException {
        try {
            Map<String, Object> message = new HashMap<String, Object>();
            message.put("client-guid", data.getClientGuid().toHexString());
            message.put("supported-fwt-version", data.getSupportedFWTVersion());
            
            Map<String, Object> address = new HashMap<String, Object>();
            ConnectableSerializer serializer = new ConnectableSerializer();
            address.put("type", serializer.getAddressType());
            address.put("value", StringUtils.toUTF8String(Base64.encodeBase64(serializer.serialize(data.getAddress()))));
            
            message.put("address", address);
            
            connection.sendLiveMessage(presence, TYPE, message);
        } catch (IOException ie) {
            // not supposed to happen, would indicate internal erro in address serializer
            throw new RuntimeException(ie);
        }
    }

}
