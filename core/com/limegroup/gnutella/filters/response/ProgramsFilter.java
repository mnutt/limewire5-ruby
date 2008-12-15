package com.limegroup.gnutella.filters.response;

import org.limewire.core.settings.LibrarySettings;
import org.limewire.util.MediaType;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryReply;

class ProgramsFilter implements ResponseFilter {

    @Override
    public boolean allow(QueryReply qr, Response response) {
        if(MediaType.getProgramMediaType().matches(response.getName())) {
            return LibrarySettings.ALLOW_PROGRAMS.getValue();
        } else {
            return true;
        }
    }

}
