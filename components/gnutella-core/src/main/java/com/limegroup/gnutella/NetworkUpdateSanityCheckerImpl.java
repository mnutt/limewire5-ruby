package com.limegroup.gnutella;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.io.IP;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * A sanity checker for many different in-network verification
 * requests.
 * 
 * If we cannot verify the message as received from a number
 * of hosts from different areas, message the user to hit
 * the website as the installation may possible be corrupted.
 */
@Singleton
public class NetworkUpdateSanityCheckerImpl implements NetworkUpdateSanityChecker {

    private static final Set<RequestType> allTypes = EnumSet.allOf(RequestType.class);

    private static final Log LOG = LogFactory.getLog(NetworkUpdateSanityCheckerImpl.class);

    private static final int MAXIMUM_FAILURES = 20;
    private static final String MASK = "/255.255.0.0";

    private final Map<RequestType, Map<ReplyHandler, Boolean>> requests =
        new HashMap<RequestType, Map<ReplyHandler, Boolean>>(RequestType.values().length);
    private final List<IP> failures = new ArrayList<IP>(MAXIMUM_FAILURES);
    private boolean finished = false;
    private final Set<RequestType> successes = EnumSet.noneOf(RequestType.class);

    private final Provider<ActivityCallback> activityCallback;

    @Inject
    public NetworkUpdateSanityCheckerImpl(
            Provider<ActivityCallback> activityCallback) {
        this.activityCallback = activityCallback;
    }

    /**
     * Stores knowledge that we've requested a network-updatable component
     * from the given source.
     */
    public void handleNewRequest(ReplyHandler handler, RequestType type) {
        if(LOG.isDebugEnabled())
            LOG.debug("Adding request to handler: " + handler);

        synchronized(requests) {
            // We're done.
            if(finished) {
                LOG.debug("Already reached maximum failure point, ignoring.");
                return;
            }

            addRequest(handler, type);
        }
    }

    /**
     * Acknowledge we received a valid response from the source.
     */
    public void handleValidResponse(ReplyHandler handler, RequestType type) {
        if(LOG.isDebugEnabled())
            LOG.debug("Received valid response from handler: " + handler + "of type: " + type);
        synchronized(requests) {
            if(!finished) {
                // If we've gotten all kinds of valid responses,
                // assume we're okay.
                successes.add(type);
                if(successes.containsAll(allTypes)) {
                    LOG.debug("Received every kind of success!");
                    finished = true;
                    requests.clear();
                    failures.clear();
                } else {
                    removeRequest(handler, type);
                }
            }
        }
    }

    /**
     * Acknowledge we received an invalid response from the source.
     */
    public void handleInvalidResponse(ReplyHandler handler, RequestType type) {
        synchronized(requests) {
            // If we had a request for this handler...
            if(!finished && removeRequest(handler, type)) {
                if(LOG.isDebugEnabled())
                    LOG.debug("Had a request from handler, adding as failure... " + handler);

                IP ip = new IP(handler.getAddress() + MASK);
                boolean contained = false;
                // See if we already had a failure in this range, if so
                // then ignore it.
                for(IP current : failures) {
                    if(current.contains(ip)) {
                        if(LOG.isDebugEnabled())
                            LOG.debug("Already had a failure from range: " +    
                                    current + ", ignoring handler ip: " + ip);
                        contained = true;
                        break;
                    }
                }

                if(!contained) {
                    // If no failure already here, add it.
                    failures.add(ip);
                    if(LOG.isDebugEnabled())
                        LOG.debug("Current failure size: " + failures.size());

                    if(failures.size() == MAXIMUM_FAILURES) {
                        LOG.debug("Reached failure threshold!");
                        finished = true;
                        requests.clear();
                        failures.clear();
                        activityCallback.get().installationCorrupted();
                    }
                }
            }
        }

        if(handler instanceof Shutdownable)
            ((Shutdownable)handler).shutdown();
    }

    /** Adds a single incoming request to the maps. */
    private void addRequest(ReplyHandler handler, RequestType type) {
        Map<ReplyHandler, Boolean> inner = requests.get(type);
        if(inner == null) {
            inner = new WeakHashMap<ReplyHandler, Boolean>();
            requests.put(type, inner);
        }
        inner.put(handler, Boolean.TRUE);
    }

    /**
     * Removes a request from the maps, returning an object if it was
     * contained.  Null if it wasn't.
     */
    private boolean removeRequest(ReplyHandler handler, RequestType type) {
        Map<ReplyHandler, Boolean> inner = requests.get(type);
        if(inner == null)
            return false;
        else
            return inner.remove(handler) != null;     
    }
}