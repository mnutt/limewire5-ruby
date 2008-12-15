package com.limegroup.gnutella.http;

import java.io.IOException;
import java.util.Map;

import org.limewire.statistic.Statistic;


public class SimpleReadHeaderState extends ReadHeadersIOState {
    
    public SimpleReadHeaderState(Statistic stat, int maxHeaders, int maxHeaderSize) {
        super(new HeaderSupport(), stat, maxHeaders, maxHeaderSize);
    }

    @Override
    protected void processConnectLine() throws IOException {
        // Does nothing.
    }

    @Override
    protected void processHeaders() throws IOException {
        // Does nothing.
    }
    
    public Map<String, String> getHeaders() {
        return support.getHeaders();
    }
    
    public String getConnectLine() {
        return connectLine;
    }

}
