package org.limewire.facebook.service.livemessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.limewire.facebook.service.FacebookFriendConnection;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.FeatureTransport;
import org.limewire.io.Address;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.net.address.AddressFactory;
import org.limewire.net.address.AddressSerializer;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;


/**
 * Used to (de)serialize "address" live messages.
 */
public class AddressHandler implements LiveMessageHandler, FeatureTransport<Address> {
    
    private static final String TYPE = "address";
    
    private static final Log LOG = LogFactory.getLog(AddressHandler.class);
    
    private final FacebookFriendConnection connection;
    private final AddressFactory addressFactory;

    private final Handler<Address> addressHandler;

    @Inject
    AddressHandler(@Assisted FacebookFriendConnection connection,
            AddressFactory addressFactory,
            FeatureTransport.Handler<Address> addressHandler) {
        this.connection = connection;
        this.addressFactory = addressFactory;
        this.addressHandler = addressHandler;
    }

    @Override
    @Inject
    public void register(LiveMessageHandlerRegistry registry) {
        registry.register(TYPE, this);
    }

    @Override
    public void handle(String messageType, JSONObject message) {
        String from = message.optString("from", null);
        String addressType = message.optString("address-type", null);
        String addressData = message.optString("address", null);
        if (from != null && addressType != null & addressData != null) {
            Address address;
            try {
                address = addressFactory.deserialize(addressType, Base64.decodeBase64(StringUtils.toUTF8Bytes(addressData)));
                addressHandler.featureReceived(from, address);
            } catch (IOException e) {
                LOG.debug("invalid address", e);
            }
        }
    }

    @Override
    public void sendFeature(FriendPresence presence, Address localFeature) throws FriendException {
        Map<String, Object> message = new HashMap<String, Object>();
        AddressSerializer serializer = addressFactory.getSerializer(localFeature);
        message.put("address-type", serializer.getAddressType());
        try {
            message.put("address", StringUtils.toUTF8String(Base64.encodeBase64(serializer.serialize(localFeature))));
        } catch (IOException e) {
            throw new FriendException(e);
        }
        LOG.debugf("sending address: {0}", message);
        connection.sendLiveMessage(presence, TYPE, message);
    }
}
