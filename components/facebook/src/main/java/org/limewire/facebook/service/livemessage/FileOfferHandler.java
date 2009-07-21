package org.limewire.facebook.service.livemessage;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.facebook.service.FacebookFriendConnection;
import org.limewire.friend.api.FileMetaData;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.FeatureTransport;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class FileOfferHandler implements LiveMessageHandler, FeatureTransport<FileMetaData>{

    private static final String TYPE = "file-offer";

    private final FacebookFriendConnection connection;
    private final Handler<FileMetaData> fileMetaDataHandler;

    @Inject
    FileOfferHandler(@Assisted FacebookFriendConnection connection,
                     FeatureTransport.Handler<FileMetaData> fileMetaDataHandler) {
        this.connection = connection;
        this.fileMetaDataHandler = fileMetaDataHandler;
    }

    @Override
    @Inject
    public void register(LiveMessageHandlerRegistry registry) {
        registry.register(TYPE, this);
    }

    @Override
    public void handle(String messageType, JSONObject message) throws JSONException {
        String from = message.optString("from", null);
        FacebookFileMetaData fileMetaData = new FacebookFileMetaData(message);
        fileMetaDataHandler.featureReceived(from, fileMetaData);
    }

    @Override
    public void sendFeature(FriendPresence presence, FileMetaData localFeature) throws FriendException {
        Map<String, Object> messageMap = new HashMap<String, Object>();
        messageMap.putAll(localFeature.getSerializableMap());
        connection.sendLiveMessage(presence, TYPE, messageMap);
    }
}
