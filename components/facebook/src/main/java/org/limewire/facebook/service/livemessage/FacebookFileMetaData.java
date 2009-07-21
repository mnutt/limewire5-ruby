package org.limewire.facebook.service.livemessage;

import org.json.JSONObject;
import org.limewire.friend.impl.FileMetaDataImpl;

class FacebookFileMetaData extends FileMetaDataImpl {
    FacebookFileMetaData(JSONObject fileMetaData) {
        for(Element element : Element.values()) {
            data.put(element.toString(), fileMetaData.optString(element.toString(), null));    
        }
        if (!isValid()) {
            throw new IllegalArgumentException("is missing mandatory fields: " + this);
        }
    }
}
