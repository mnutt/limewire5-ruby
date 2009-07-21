package org.limewire.core.impl.connection;

import java.beans.PropertyChangeListener;

import org.limewire.core.api.connection.ConnectionItem;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.core.api.connection.ConnectionStrength;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.google.inject.Singleton;

/**
 * Mock implementation of GnutellaConnectionManager.
 */
@Singleton
public class MockConnectionManagerImpl implements GnutellaConnectionManager {

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public boolean isUltrapeer() {
        return false;
    }

    @Override
    public void connect() {
    }

    @Override
    public void disconnect() {
    }

    @Override
    public void restart() {
    }

    @Override
    public ConnectionStrength getConnectionStrength() {
        return ConnectionStrength.FULL;
    }

    @Override
    public EventList<ConnectionItem> getConnectionList() {
        return new BasicEventList<ConnectionItem>();
    }

    @Override
    public void removeConnection(ConnectionItem item) {
    }

    @Override
    public void tryConnection(String hostname, int portnum, boolean useTLS) {
    }

}
