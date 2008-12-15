package org.limewire.core.impl.monitor;

import org.limewire.core.api.monitor.IncomingSearchManager;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

/**
 * Mock implementation of IncomingSearchManager.
 */
public class MockIncomingSearchManager implements IncomingSearchManager {

    @Override
    public EventList<String> getIncomingSearchList() {
        return new BasicEventList<String>();
    }

    @Override
    public void setListEnabled(boolean enabled) {
    }

    @Override
    public void setListSize(int size) {
    }
    
}
