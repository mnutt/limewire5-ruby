package com.limegroup.gnutella.connection;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.client.methods.HttpGet;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.util.Clock;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.util.LimeWireUtils;

@Singleton
/**
 * Measures how long it takes to get a single ultrapeer connection, and reports back to
 * monitoring machines.
 */
public class ConnectionReporter implements ConnectionLifecycleListener {
    
    private final ApplicationServices application;
    private final HttpExecutor httpExecutor;
    private final Clock clock;

    private final AtomicLong startedConnecting = new AtomicLong(0);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    
    public static final String REPORTING_URL = "http://client-data.limewire.com/conn";
    private ConnectionManager connectionManager;

    @Inject
    public ConnectionReporter(ApplicationServices application,
                              HttpExecutor httpExecutor,
                              Clock clock) {
        this.application = application;
        this.httpExecutor = httpExecutor;
        this.clock = clock;
    }
    
    @Inject
    public void register(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.connectionManager.addEventListener(this);
    }

    @Override
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        switch (evt.getType()) {
            case CONNECTING:
                startedConnecting.compareAndSet(0, clock.now());
                break;
            case CONNECTION_INITIALIZED:  
                // TODO use CONNECTED event instead?
                //if stat gathering has been approved
                if(ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING.getValue() &&
                        !connected.getAndSet(true)) {
                    HttpGet request = new HttpGet(LimeWireUtils.addLWInfoToUrl(REPORTING_URL, application.getMyGUID()) +
                            "&connect_time=" + Long.toString(clock.now() - startedConnecting.get()));  
                    httpExecutor.execute(request);
                    connectionManager.removeEventListener(this);
                }
                break;
        }
    }
}
