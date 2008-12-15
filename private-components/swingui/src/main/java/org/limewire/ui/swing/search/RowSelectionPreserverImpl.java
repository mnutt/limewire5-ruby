package org.limewire.ui.swing.search;

import java.util.ArrayList;

class RowSelectionPreserverImpl implements RowSelectionPreserver {
    private ArrayList<RowPresevationListener> listeners = new ArrayList<RowPresevationListener>();
    
    @Override
    public void addRowPreservationListener(RowPresevationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void beforeUpdateEvent() {
        for(RowPresevationListener listener : listeners) {
            listener.preserveRowSelection();
        }
    }

    @Override
    public void afterUpdateEvent() {
        for(RowPresevationListener listener : listeners) {
            listener.restoreRowSelection();
        }
    }
}