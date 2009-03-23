package org.limewire.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts all events sent to the PropertyChangeListener. Stores the latest
 * event in the lastestEvent field.
 */
public class TestPropertyChangeListener implements PropertyChangeListener {

    private PropertyChangeEvent latestEvent = null;

    private AtomicInteger eventCount = new AtomicInteger(0);

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        this.latestEvent = evt;
        eventCount.incrementAndGet();
    }

    public PropertyChangeEvent getLatestEvent() {
        return latestEvent;
    }

    public int getEventCount() {
        return eventCount.intValue();
    }
}