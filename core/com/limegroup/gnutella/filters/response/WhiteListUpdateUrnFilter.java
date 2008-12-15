package com.limegroup.gnutella.filters.response;

import com.google.inject.Inject;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.version.UpdateCollection;
import com.limegroup.gnutella.version.UpdateData;
import com.limegroup.gnutella.version.UpdateHandler;

/**
 * Allows responses to go through whose sha1 urns match one of the urns
 * in the {@link UpdateCollection}. 
 */
class WhiteListUpdateUrnFilter implements ResponseFilter {

    private final UpdateHandler updateHandler;

    @Inject
    public WhiteListUpdateUrnFilter(UpdateHandler updateHandler) {
        this.updateHandler = updateHandler;
    }
    
    @Override
    public boolean allow(QueryReply qr, Response response) {
        URN sha1 = UrnSet.getSha1(response.getUrns());
        if (sha1 == null) {
            return false;
        }
        UpdateCollection updateCollection = updateHandler.getUpdateCollection();
        if (updateCollection == null) {
            return false;
        }
        for (UpdateData updateData : updateCollection.getUpdateData()) {
            URN urn = updateData.getUpdateURN();
            if (urn != null && urn.equals(sha1)) {
                return true;
            }
        }
        return false;
    }

}
